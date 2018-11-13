package com.intendia.gwt.autorest.client;

import javax.annotation.Nullable;

/** Visit each resource gathering the metadata and end up calling {@link #as(Class, Class)}. */
public interface ResourceVisitor {

    /** Sets the http method. */
    ResourceVisitor method(String method);

    /** Append paths, or set if the path is absolute. */
    ResourceVisitor path(Object... paths);

    /** Sets the produced media-type. */
    ResourceVisitor produces(String... produces);

    /** Sets the consumed media-type. */
    ResourceVisitor consumes(String... consumes);

    /** Sets a query param with its type */
    ResourceVisitor param(String key, @Nullable Object value, Type type);

    /** Sets a header param with its type. */
    ResourceVisitor header(String key, @Nullable Object value, Type type);

    /** Sets a from param with its type. */
    ResourceVisitor form(String key, @Nullable Object value, Type type);
    
    /** Sets the content data with its type. */
    ResourceVisitor data(Object data, Type typeInfo);

    /** Wrap the current resource state into a {@code type}. */
    <T> T as(Type type);

    interface Supplier {
        ResourceVisitor get();
    }
}
