/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

import java.util.ArrayList;
import java.util.List;
import java.util.Map

import grails.converters.JSON
import grails.converters.XML
import net.nosegrind.apitoolkit.Api;
import org.codehaus.groovy.grails.web.json.JSONObject

import net.nosegrind.apitoolkit.Method;
import net.nosegrind.apitoolkit.ApiStatuses;
import org.springframework.web.context.request.RequestContextHolder as RCH
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.sitemesh.GrailsContentBufferingResponse

class ApiToolkitFilters {
	
	def apiToolkitService
	def grailsApplication
	def apiCacheService
	
	def filters = {
		String apiName = grailsApplication.config.apitoolkit.apiName
		String apiVersion = grailsApplication.metadata['app.version']
		String apiDir = (apiName)?"${apiName}_v${apiVersion}":"v${apiVersion}"
		
		//String apiRegex = "/${apiDir}-[0-9]?[0-9]?(\\.[0-9][0-9]?)?/**".toString()
		//println apiRegex
		
		//apitoolkit(regex:apiRegex){
		apitoolkit(uri:"/${apiDir}*/**"){
			before = {
				//log.error("##### FILTER (BEFORE)")
				try{
					if(!request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){
						return false
					}
					apiToolkitService.setApiObjectVersion(apiDir, request, params)
					params.action = (params.action)?params.action:'index'
					def cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]
					if(cache){
						boolean result = apiToolkitService.handleApiRequest(cache,request,params)
						return result
					}
					return false

				}catch(Exception e){
					log.error("[ApiToolkitFilters :: apitoolkit.before] : Exception - full stack trace follows:", e);
					return false
				}
			}
			
			after = { Map model ->
				 //log.error("##### FILTER (AFTER)")
				 try{
				 	def cache = (params.controller)?apiCacheService.getApiCache(params.controller):[:]
					 if(params?.apiChain?.order){
						 // return map of variable and POP first variable off chain 'order'
						 boolean result = apiToolkitService.handleApiChain(cache, request,response,model,params)
						 forward(controller:"${params.controller}",action:"${params.action}",id:"${model.id}")
						 return false
					 }else if(params?.apiBatch){
							 forward(controller:"${params.controller}",action:"${params.action}",params:params)
							 return false
					 }else{
					 	LinkedHashMap map = apiToolkitService.handleApiResponse(cache, request,response,model,params)
						 if(!model){
							 response.flushBuffer()
							 return false
						 }
						 
						 if(params?.apiCombine==true){
								map = params.apiCombine
						 }
						 
						 switch(request.method) {
							 case 'PURGE':
								 // cleans cache
								 break;
							 case 'TRACE':
								 break;
							 case 'HEAD':
								 break;
							 case 'OPTIONS':
		
								 LinkedHashMap doc = apiToolkitService.getApiDoc(params)
								 
								 switch(params.contentType){
									 case 'application/xml':
										 render(text:doc as XML, contentType: "${params.contentType}")
										 break
									 case 'application/json':
									 default:
										 render(text:doc as JSON, contentType: "${params.contentType}")
										 break
								 }
								 return false
								 break;
							 case 'GET':
								 if(map?.isEmpty()==false){
									 switch(params.contentType){
										 case 'application/xml':
											 if(params.encoding){
												 render(text:map as XML, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as XML, contentType: "${params.contentType}")
											 }
											 break
										 case 'text/html':
											 break
										 case 'application/json':
										 default:
											 if(params.encoding){
												 render(text:map as JSON, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as JSON, contentType: "${params.contentType}")
											 }
											 break
									 }
									 return false
								 }
								 break
							 case 'POST':
								 if(!map.isEmpty()){
									 switch(params.contentType){
										 case 'application/xml':
											 if(params.encoding){
												 render(text:map as XML, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as XML, contentType: "${params.contentType}")
											 }
											 break
										 case 'application/json':
										 default:
											 if(params.encoding){
												 render(text:map as JSON, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as JSON, contentType: "${params.contentType}")
											 }
											 break
									 }
									 return false
								 }
								 break
							 case 'PUT':
								 if(!map.isEmpty()){
									 switch(params.contentType){
										 case 'application/xml':
											 if(params.encoding){
												 render(text:map as XML, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as XML, contentType: "${params.contentType}")
											 }
											 break
										 case 'application/json':
										 default:
											 if(params.encoding){
												 render(text:map as JSON, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as JSON, contentType: "${params.contentType}")
											 }
											 break
									 }
									 return false
								 }
								 break
							 case 'DELETE':
								 if(!map.isEmpty()){
									 switch(params.contentType){
										 case 'application/xml':
											 if(params.encoding){
												 render(text:map as XML, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as XML, contentType: "${params.contentType}")
											 }
											 break
										 case 'application/json':
										 default:
											 if(params.encoding){
												 render(text:map as JSON, contentType: "${params.contentType}",encoding:"${params.encoding}")
											 }else{
												 render(text:map as JSON, contentType: "${params.contentType}")
											 }
											 break
									 }
									 return false
								 }
								 break
						 }
					 }
					 return false
				}catch(Exception e){
					log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
					return false
				}
			 }

		}

	}

}
