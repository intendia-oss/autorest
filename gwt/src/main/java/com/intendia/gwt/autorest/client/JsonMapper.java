package com.intendia.gwt.autorest.client;

public interface JsonMapper {
    <T> T read(String json);

    <T> T readAsArray(String json);

    <T> String write(T value);
}
