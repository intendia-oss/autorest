package com.intendia.gwt.autorest.client;

public class RestServiceProxy {
    private final Resource resource;
    private final Dispatcher dispatcher;

    public RestServiceProxy(Resource resource, Dispatcher dispatcher, String path) {
        this.resource = resource.resolve(path);
        this.dispatcher = dispatcher;
    }

    public Resource resource(String path) {
        return resource.resolve(path);
    }

    public Dispatcher dispatcher() {
        return dispatcher;
    }
}
