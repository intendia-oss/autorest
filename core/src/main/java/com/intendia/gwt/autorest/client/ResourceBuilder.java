package com.intendia.gwt.autorest.client;

import javax.annotation.Nullable;

public interface ResourceBuilder {

    /** Append paths, or set if the path is absolute. */
    ResourceBuilder path(Object... paths);

    /** Sets a query param. */
    ResourceBuilder param(String key, @Nullable Object value);

    /** Sets the http method. */
    ResourceBuilder method(String method);

    /** Sets a http header. */
    ResourceBuilder header(String key, String value);

    /** Sets the content data. */
    ResourceBuilder data(Object data);

    /** Returns the current URI. */
    /* @Experimental */ String uri();

    <T> T build(Class<? super T> type);

    default ResourceBuilder nest() { return build(ResourceBuilder.class); }
}
