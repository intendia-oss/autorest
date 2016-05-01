package com.intendia.gwt.autorest.client;

public class RestServiceProxy {
    private final ResourceBuilder resource;

    public RestServiceProxy(ResourceBuilder resource, String path) {
        this.resource = resource.copy().path(path);
    }

    protected ResourceBuilder resolve(String... paths) {
        return resource.copy().path(paths);
    }
}
