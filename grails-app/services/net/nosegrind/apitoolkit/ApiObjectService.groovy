package net.nosegrind.apitoolkit

import org.codehaus.groovy.grails.web.json.JSONObject
import java.lang.reflect.Method
import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass

import grails.converters.JSON
import grails.converters.XML

import java.util.HashSet;
import java.util.LinkedHashMap;

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import grails.plugin.cache.CachePut
import grails.plugin.cache.GrailsCacheManager
import org.springframework.cache.Cache

import org.codehaus.groovy.grails.validation.routines.UrlValidator
import org.springframework.web.context.request.RequestContextHolder as RCH

import net.nosegrind.apitoolkit.*

class ApiObjectService{

	def grailsApplication
	def springSecurityService
	def apiToolkitService
	def apiCacheService
	GrailsCacheManager grailsCacheManager
	
	static transactional = false
	
	private JSONObject readObjectFile(){
		def filePath = "apiObjects.json"
		def text = grailsApplication.getParentContext().getResource("classpath:$filePath").getInputStream().getText()
		def json = JSON.parse(text)
		return json
	}
	
	
	String getKeyType(String reference, String type){
		String keyType = (reference.toLowerCase()=='self')?((type.toLowerCase()=='long')?'PKEY':'INDEX'):((type.toLowerCase()=='long')?'FKEY':'INDEX')
		return keyType
	}
	
	private LinkedHashMap getIOSet(JSONObject io,LinkedHashMap apiObject){
		LinkedHashMap<String,ParamsDescriptor> ioSet = [:]

		io.each{ k, v ->
			// init
			if(!ioSet["${k}"]){
				ioSet["${k}"] = []
			}

			def roleVars=v.toList()
			roleVars.each{ val ->
				if(v.contains(val)){
					if(!ioSet["${k}"].contains(apiObject["${val}"])){
						ioSet["${k}"].add(apiObject["${val}"])
					}
				}
			}
		}
		
		// add permitAll vars to other roles after processing
		ioSet.each(){ key, val ->
			if(key!='permitAll'){
				ioSet["permitAll"].each(){ it ->
						ioSet["${key}"].add(it)
				}
			}
		}
		
		return ioSet
	}
	
	private ApiDescriptor createApiDescriptor(String apiname,String apiMethod, String apiDescription, List apiRoles, String uri, JSONObject json){
		LinkedHashMap<String,ParamsDescriptor> apiObject = [:]
		ApiParams param = new ApiParams()
		
		json["${apiname}"].VALUES.each{ k,v ->
			String references = ""
			String hasDescription = ""
			String hasMockData = ""
			
			v.type = (v.references)?getKeyType(v.references, v.type):v.type

			param.setParam(v.type,"${k}")
			
			def configType = grailsApplication.config.apitoolkit.apiobject.type."${v.type}"
			
			hasDescription = (configType?.description)?configType.description:hasDescription
			hasDescription = (v?.description)?v.description:hasDescription
			if(hasDescription){ param.hasDescription("${hasDescription}") }
			
			references = (configType?.references)?configType.references:""
			references = (v?.references)?v.references:references
			if(references){ param.referencedBy(references) }
			
			hasMockData = (v?.mockData)?v.mockData:hasMockData
			if(hasMockData){ param.hasMockData("${hasMockData}") }

			// collect api vars into list to use in apiDescriptor
			apiObject["${param.param.name}"] = param.toObject()
		}
		
		LinkedHashMap receives = getIOSet(json["${apiname}"].URI["${uri}"]?.REQUEST,apiObject)
		LinkedHashMap returns = getIOSet(json["${apiname}"].URI["${uri}"]?.RESPONSE,apiObject)
		
		ApiDescriptor service = new ApiDescriptor(
			"method":"${apiMethod}",
			"description":"${apiDescription}",
			"doc":[:],
			"receives":receives,
			"returns":returns
		)
		service['roles'] = apiRoles

		return service
	}
	
	def initApiCache(){
		JSONObject json = readObjectFile()

		grailsApplication.controllerClasses.each { DefaultGrailsControllerClass controllerClass ->
			String controllername = controllerClass.logicalPropertyName
			
			Map methods = [:]
			controllerClass.getClazz().methods.each { Method method ->
				String actionname = method.getName()
				
				ApiStatuses error = new ApiStatuses()
				
				if(method.isAnnotationPresent(Api)) {
					def api = method.getAnnotation(Api)

					ApiDescriptor apiDescriptor
					Map apiParams
					
					String apiName = (api?.name())?api.name().capitalize():controllername.capitalize()
					String apiMethod = api.method()
					String apiDescription = api.description()
					List apiRoles = api?.roles()
					
					if(json["${apiName}"]){
						String uri = controllername+"/"+actionname
						if(json["${apiName}"].URI?."${uri}"){
							apiDescriptor = createApiDescriptor(apiName, apiMethod, apiDescription, apiRoles, uri, json)
						}
					}

					methods["${actionname}"] = apiDescriptor
				}
			}
			
			if(methods){
				apiToolkitService.setApiCache(controllername.toString(),methods)
			}
			
			def cache = apiCacheService.getApiCache(controllername)

		}
	}
}
