package net.nosegrind.apitoolkit;

import grails.util.Holders
import grails.validation.Validateable

//@grails.validation.Validateable
@Validateable
class ParamsDescriptor {

	String paramType
	String name
	String idReferences
	String description
	boolean expose = true
	boolean visible = true
	boolean required = true
	List roles = []
	String mockData
	ParamsDescriptor[] values = []

	static constraints = { 
		paramType(nullable:false,maxSize:100,inList: ["PKEY", "FKEY", "INDEX","STRING","LONG","BOOLEAN","FLOAT","BIGDECIMAL","EMAIL","URL"])
		name(nullable:false,maxSize:100)
		idReferences(maxSize:100, validator: { val, obj ->
			if (paramType!="PKEY" && paramType!="FKEY") {
			  return ['nullable']
			}else {
			  return true
			}
		})
		description(nullable:false,maxSize:1000)
		expose(nullable:false)
		visible(nullable:false)
		required(nullable:false)
		roles(nullable:true)
		mockData(nullable:true)
		values(nullable:true)
	} 
}