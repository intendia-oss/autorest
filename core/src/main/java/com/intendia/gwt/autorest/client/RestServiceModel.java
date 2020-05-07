package com.intendia.gwt.autorest.client;

import java.util.HashMap;

public class RestServiceModel {
    protected final ResourceVisitor.Supplier path;
    protected final HashMap<String, String> securityTokens = new HashMap<String, String>();

    public RestServiceModel(ResourceVisitor.Supplier path) {
        this.path = path;
    }
    
    public void setSecurityToken(String tokenName, String tokenVal) {
    	this.securityTokens.put(tokenName, tokenVal);
    }
    
    protected String getSecurityToken(String tokenName) {
    	String ret = this.securityTokens.get(tokenName);
    	return ret != null ? ret : "";
    }

    protected ResourceVisitor method(String method) {
        return path.get().method(method);
    }
}
