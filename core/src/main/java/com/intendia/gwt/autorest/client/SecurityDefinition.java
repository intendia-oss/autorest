package com.intendia.gwt.autorest.client;

public @interface SecurityDefinition {
	public enum SecurityType {
		UNDEFINED(""),
	    BASIC("basic"),
	    APIKEY("apiKey");
	    private final String text;
	    SecurityType(final String text) {
	        this.text = text;
	    }
	    public String toString() {
	        return text;
	    }
	    public static SecurityType fromString(String txt) {
	    	for(SecurityType st : values())
	    	{
	    		if(st.toString().equals(txt)) {
	    			return st;
	    		}
	    	}
	    	return UNDEFINED;
	    }
	}

	public enum Location {
		UNDEFINED(""),
	    HEADER("header"),
	    QUERY("query");
	    private final String text;
	    Location(final String text) {
	        this.text = text;
	    }
	    public String toString() {
	        return text;
	    }
	    public static Location fromString(String txt) {
	    	for(Location loc : values())
	    	{
	    		if(loc.toString().equals(txt)) {
	    			return loc;
	    		}
	    	}
	    	return UNDEFINED;
	    }
	}
	

	SecurityType type() default SecurityType.BASIC;
	Location location() default Location.UNDEFINED;
	String name() default "X-Auth-Key";
}
