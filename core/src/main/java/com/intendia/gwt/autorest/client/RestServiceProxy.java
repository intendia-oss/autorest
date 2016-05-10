package com.intendia.gwt.autorest.client;

import java.util.function.Supplier;

public class RestServiceProxy {
    protected final Supplier<ResourceBuilder> factory;

    public RestServiceProxy(Supplier<ResourceBuilder> factory) {
        this.factory = factory;
    }
}
