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
public abstract class CollectorResourceVisitor implements ResourceVisitor {
    private static final String ABSOLUTE_PATH = "[a-z][a-z0-9+.-]*:.*|//.*";

    protected static class Param {
        public String key;
        public Object value;
        public Param(String key, Object value) { this.key = key; this.value = value; }
    }

    protected List<String> paths;
    protected List<Param> queryParams;
    protected List<Param> formParams;
    protected String method;
    protected Map<String, String> headers;
    protected Object data;

    protected CollectorResourceVisitor() {
        this.paths = new ArrayList<>(singleton(".")); // so new Resource().path('/foo') results './foo' and not '/foo'
        this.queryParams = new ArrayList<>();
        this.method = HttpMethod.GET;
        this.headers = new TreeMap<>();
        this.data = null;
    }

    @Override public ResourceVisitor path(Object... paths) {
        for (Object path : paths) path(Objects.toString(path));
        return this;
    }

    public ResourceVisitor path(String path) {
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // strip off trailing slash
        if (path.matches(ABSOLUTE_PATH)) this.paths = new ArrayList<>(singleton(path)); // reset current path
        else this.paths.add(path.startsWith("/") ? path : "/" + path);
        return this;
    }

    @Override public ResourceVisitor param(String key, @Nullable Object value) {
        if (value instanceof Iterable<?>) for (Object v : ((Iterable<?>) value)) param(key, v);
        else if (value != null) queryParams.add(new Param(key, value));
        return this;
    }

    @Override public ResourceVisitor form(String key, @Nullable Object value) {
        if (value instanceof Iterable<?>) for (Object v : ((Iterable<?>) value)) form(key, v);
        else if (value != null) formParams.add(new Param(key, value));
        return this;
    }

    @Override public ResourceVisitor method(String method) {
        this.method = method;
        return this;
    }

    @Override public ResourceVisitor header(String key, String value) {
        headers.put(requireNonNull(key, "header key required"), requireNonNull(value, "header value required"));
        return this;
    }

    @Override public ResourceVisitor data(Object data) {
        this.data = data;
        return this;
    }

    @Override public String toString() {
        return method + " " + paths;
    }
}
