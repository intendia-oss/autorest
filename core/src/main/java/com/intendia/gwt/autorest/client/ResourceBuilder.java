package com.intendia.gwt.autorest.client;

import javax.annotation.Nullable;
import rx.annotations.Experimental;

public interface ResourceBuilder {

    /** Append paths the the current resource path, or set if the path is absolute. */
    ResourceBuilder path(Object... paths);

    /** Add a query param to the current resource path. */
    ResourceBuilder param(String key, @Nullable Object value);

    /** Sets the http method to the current resource path. */
    ResourceBuilder method(String method);

    /** Add a http header to the current resource path. */
    ResourceBuilder header(String key, String value);

    /** Sets the content data to the current resource path. */
    ResourceBuilder data(Object data);

    /** Returns the current builder URI. */
    @Experimental String uri();

    <T> T build(Class<? super T> type);

    default ResourceBuilder nest() { return build(ResourceBuilder.class); }
}
