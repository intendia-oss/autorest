package com.intendia.gwt.autorest.client;

public class RestServiceModel {
    protected final ResourceVisitor.Factory request;

    public RestServiceModel(ResourceVisitor.Factory request) {
        this.request = request;
    }
}
