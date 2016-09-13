package com.intendia.gwt.autorest.client;

public class RestServiceModel {
    protected final ResourceVisitor.Supplier path;

    public RestServiceModel(ResourceVisitor.Supplier path) {
        this.path = path;
    }

    protected ResourceVisitor method(String method) {
        return path.get().method(method);
    }
}
