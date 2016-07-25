package com.intendia.gwt.autorest.client;

public class RestServiceModel {
    protected final ResourceVisitor.Supplier parent;

    public RestServiceModel(ResourceVisitor.Supplier parent) {
        this.parent = parent;
    }

    protected ResourceVisitor method(String method) {
        return parent.get().method(method);
    }
}
