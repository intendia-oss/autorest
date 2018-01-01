package com.intendia.gwt.autorest.client;

import java.util.HashMap;
import java.util.Map;

public class MappersRegistry {
    private final static Map<String, JsonMapper> MAPPERS = new HashMap<>();

    public static void register(String type, JsonMapper mapper) {
        MAPPERS.put(type, mapper);
    }

    public static JsonMapper get(String type) {
        return MAPPERS.get(type);
    }

    public static boolean contains(String canonicalName) {
        return MAPPERS.containsKey(canonicalName);
    }
}
