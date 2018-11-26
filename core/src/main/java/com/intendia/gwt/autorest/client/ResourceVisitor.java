package com.intendia.gwt.autorest.client;

import javax.annotation.Nullable;

/** Visit each resource gathering the metadata and end up calling {@link #as(Class, Class)}. */
public interface ResourceVisitor {

    /** Sets the http method. */
    ResourceVisitor method(String method);

    /** Append paths, or set if the path is absolute. */
    ResourceVisitor path(Object... paths);

    /** Sets a path param with its type */
    <T> ResourceVisitor path(@Nullable T value, TypeToken<T> typeToken);
    
    /** Sets the produced media-type. */
    ResourceVisitor produces(String... produces);

    /** Sets the consumed media-type. */
    ResourceVisitor consumes(String... consumes);

    /** Sets a query param with its type */
    <T> ResourceVisitor param(String key, @Nullable T value, TypeToken<T> typeToken);

    /** Sets a header param with its type. */
    <T> ResourceVisitor header(String key, @Nullable T value, TypeToken<T> typeToken);

    /** Sets a form param with its type. */
    <T> ResourceVisitor form(String key, @Nullable T value, TypeToken<T> typeToken);
    
    /** Sets the content data with its type. */
    <T> ResourceVisitor data(T data, TypeToken<T> typeToken);

    /** Wrap the current resource state into a {@code type}. */
    <T> T as(TypeToken<T> typeToken);

    interface Supplier {
        ResourceVisitor get();
    }
}
