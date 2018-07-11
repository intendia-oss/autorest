package com.intendia.gwt.autorest.client;

public @interface SecurityDefinition {
	public enum SecurityType {
	    BASIC("basic"),
	    APIKEY("apiKey");
	    private final String text;
	    SecurityType(final String text) {
	        this.text = text;
	    }
	    public String toString() {
	        return text;
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
	}
	

	SecurityType type() default SecurityType.BASIC;
	Location location() default Location.UNDEFINED;
	String name() default "X-Auth-Key";
}
