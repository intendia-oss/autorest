package com.intendia.gwt.autorest.client;

import javax.annotation.Nullable;

/** Visit each resource gathering the metadata and end up calling {@link #as(Class)}. */
public interface ResourceVisitor {

    /** Append paths, or set if the path is absolute. */
    ResourceVisitor path(Object... paths);

    /** Sets a query param. */
    ResourceVisitor param(String key, @Nullable Object value);

    /** Sets the http method. */
    ResourceVisitor method(String method);

    /** Sets a http header. */
    ResourceVisitor header(String key, String value);

    /** Sets the content data. */
    ResourceVisitor data(Object data);

    /** Wrap the current resource state into a {@code type}. */
    <T> T as(Class<? super T> type);

}
