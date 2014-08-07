/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/
package net.nosegrind.apitoolkit;

/*
 * Method		Idempotent	Safe
 * OPTIONS		yes			yes
 * HEAD			yes			yes
 * 
 * GET			yes			yes
 * PUT			yes			no
 * POST			no			no
 * DELETE		yes			no
 * PATCH		no			no
 * TRACE		no			yes
 */

public enum Method {
	OPTIONS("OPTIONS"),
	HEAD("HEAD"),
	GET("GET"),
	POST("POST"),
	PUT("PUT"),
	DELETE("DELETE"),
	PATCH("PATCH"),
	TRACE("TRACE");

    private String value;

    Method(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    public String getKey() {
        return name();
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static Method getEnum(String value) {
        if(value == null)
            throw new IllegalArgumentException();
        for(Method v : values())
            if(value.equalsIgnoreCase(v.getValue())) return v;
        throw new IllegalArgumentException();
    }
}