package com.intendia.gwt.autorest.client;

import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;

public interface Metadata {

    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(K<T> key);

    public static final class K<T> {

        public static <T> K<T> key() { return new K<>(); }

        @SuppressWarnings("unchecked")
        public T cast(Object o) { return (T) o; }
    }

    class Mutadata {
        private final Map<K, Object> data = new IdentityHashMap<>();

        @SuppressWarnings("unchecked")
        public @Nullable <T> T get(K<T> key) { return (T) data.get(key); }

        public <T> Mutadata put(K<T> key, T value) { data.put(key, value); return this; }

        public Mutadata remove(K<?> key) { data.remove(key); return this; }
    }
}

