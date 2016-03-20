package com.intendia.gwt.autorest.client;

public class RestServiceProxy {
    private final Resource resource;
    private final Dispatcher dispatcher;

    public RestServiceProxy(Resource resource, Dispatcher dispatcher, String path) {
        this.resource = resource.resolve(path);
        this.dispatcher = dispatcher;
    }

    protected Resource resolve(Object... path) {
        Resource resolve = resource.resolve(""); // ensure new copy
        for (Object subPath : path) {
            resolve = resolve.resolve(subPath);
        }
        return resolve;
    }

    protected Dispatcher dispatcher() {
        return dispatcher;
    }
}
