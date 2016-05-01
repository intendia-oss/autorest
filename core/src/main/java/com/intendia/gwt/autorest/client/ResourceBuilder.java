package com.intendia.gwt.autorest.client;

import javax.annotation.Nullable;

public interface ResourceBuilder {

    ResourceBuilder copy();

    ResourceBuilder path(String... paths);

    ResourceBuilder param(String key, @Nullable Object value);

    ResourceBuilder method(String method);

    ResourceBuilder header(String key, String value);

    ResourceBuilder data(Object data);

    <T> T build(Class<? super T> type);

}
