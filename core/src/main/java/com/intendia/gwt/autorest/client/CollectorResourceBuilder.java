package com.intendia.gwt.autorest.client;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.annotation.Nullable;
import javax.ws.rs.HttpMethod;

/* @Experimental */
public class CollectorResourceBuilder implements ResourceBuilder {
    private static final String ABSOLUTE_PATH = "[a-z][a-z0-9+.-]*:.*|//.*";

    protected static class Param {
        String key, value;
        public Param(String key, String value) { this.key = key; this.value = value; }
    }

    /* @VisibleForTesting */ List<String> paths;
    /* @VisibleForTesting */ List<Param> params;
    /* @VisibleForTesting */ String method;
    /* @VisibleForTesting */ Map<String, String> headers;
    /* @VisibleForTesting */ Object data;

    protected CollectorResourceBuilder() {
        this.paths = new ArrayList<>(singleton(".")); // so new Resource().path('/foo') results './foo' and not '/foo'
        this.params = new ArrayList<>();
        this.method = HttpMethod.GET;
        this.headers = new TreeMap<>();
        this.data = null;
    }

    protected CollectorResourceBuilder(CollectorResourceBuilder resource) {
        this.paths = new ArrayList<>(resource.paths);
        this.params = new ArrayList<>(resource.params);
        this.method = resource.method;
        this.headers = new TreeMap<>(resource.headers);
        this.data = resource.data;
    }

    @Override public ResourceBuilder path(Object... paths) {
        for (Object path : paths) path(Objects.toString(path));
        return this;
    }

    public ResourceBuilder path(String path) {
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // strip off trailing slash
        if (path.matches(ABSOLUTE_PATH)) this.paths = new ArrayList<>(singleton(path)); // reset current path
        else this.paths.add(path.startsWith("/") ? path : "/" + path);
        return this;
    }

    @Override public ResourceBuilder param(String key, @Nullable Object value) {
        if (value instanceof Iterable<?>) for (Object v : ((Iterable<?>) value)) param(key, v);
        else if (value != null) params.add(new Param(key, Objects.toString(value)));
        return this;
    }

    @Override public ResourceBuilder method(String method) {
        this.method = method;
        return this;
    }

    @Override public ResourceBuilder header(String key, String value) {
        headers.put(requireNonNull(key, "header key required"), requireNonNull(value, "header value required"));
        return this;
    }

    @Override public ResourceBuilder data(Object data) {
        this.data = data;
        return this;
    }

    protected String query() {
        String query = "";
        for (Param param : params) query += (query.isEmpty() ? "" : "&") + param.key + "=" + param.value;
        return query.isEmpty() ? "" : "?" + query;
    }

    @Override public String uri() {
        String uri = "";
        for (String path : paths) uri += path;
        return uri + query();
    }

    @Override @SuppressWarnings("unchecked") public <T> T build(Class<? super T> type) {
        if (ResourceBuilder.class.equals(type)) return (T) new CollectorResourceBuilder(this);
        throw new UnsupportedOperationException("unsupported type " + type);
    }

    @Override public String toString() {
        return method + " " + uri();
    }
}
