package com.intendia.gwt.autorest.client;

import java.util.function.Supplier;

public class RestServiceModel {
    protected final Supplier<ResourceVisitor> parent;

    public RestServiceModel(Supplier<ResourceVisitor> parent) {
        this.parent = parent;
    }

    protected ResourceVisitor method(String method) {
        return parent.get().method(method);
    }
}
