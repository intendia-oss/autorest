package com.intendia.gwt.autorest.client;

import jsinterop.annotations.JsMethod;

public class JsJsonMapper implements JsonMapper {

    @Override
    public <T> T read(String text) {
        return parse(text);
    }

    @Override
    public <T> T readAsArray(String json) {
        return read(json);
    }

    @Override
    public <T> String write(T value) {
        return stringify(value);
    }

    @JsMethod(namespace = "JSON")
    private static native <T> T parse(String text);

    @JsMethod(namespace = "JSON")
    private static native <T> T stringify(Object value);
}
