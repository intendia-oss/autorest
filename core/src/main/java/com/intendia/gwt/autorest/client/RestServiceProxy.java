package com.intendia.gwt.autorest.client;

public class RestServiceProxy {
    private final ResourceBuilder resource;

    public RestServiceProxy(ResourceBuilder resource, String path) {
        this.resource = resource.nest().path(path);
    }

    protected ResourceBuilder resolve(Object... paths) {
        return resource.nest().path(paths);
    }
}
