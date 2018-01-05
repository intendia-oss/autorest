package com.intendia.gwt.autorest.client;

import com.progressoft.brix.domino.gwtjackson.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class MapperRegistry {
    private final Map<String, ObjectMapper<?>> byCanonicalName = new HashMap<>();

    public void register(String type, ObjectMapper<?> mapper) { byCanonicalName.put(type, mapper); }

    public <T> ObjectMapper<T> get(Class<T> type) { return (ObjectMapper<T>) get(type.getCanonicalName()); }

    public ObjectMapper<?> get(String type) { return byCanonicalName.get(type); }

    public boolean contains(String canonicalName) { return byCanonicalName.containsKey(canonicalName); }
}
