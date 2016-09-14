package com.intendia.gwt.autorest.client;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.ws.rs.HttpMethod;

/* @Experimental */
public abstract class CollectorResourceVisitor implements ResourceVisitor {
    private static final String ABSOLUTE_PATH = "[a-z][a-z0-9+.-]*:.*|//.*";

    public static class Param {
        public final String k;
        public final Object v;
        public Param(String k, Object v) { this.k = k; this.v = v; }
        public static List<Param> expand(List<Param> in) {
            List<Param> out = new ArrayList<>();
            for (Param p : in) {
                if (!(p.v instanceof Iterable<?>)) out.add(p);
                else for (Object v : ((Iterable<?>) p.v)) out.add(new Param(p.k, v));
            }
            return out;
        }
        @Override public String toString() { return "Param{k='" + k + "', v=" + v + '}'; }
    }

    protected List<String> paths = new ArrayList<>();
    protected List<Param> queryParams = new ArrayList<>();
    protected List<Param> headerParams = new ArrayList<>();
    protected List<Param> formParams = new ArrayList<>();
    protected String method = HttpMethod.GET;
    protected Object data = null;

    @Override public ResourceVisitor method(String method) {
        Objects.requireNonNull(method, "path required");
        this.method = method;
        return this;
    }

    @Override public ResourceVisitor path(Object... paths) {
        for (Object path : paths) path(Objects.toString(Objects.requireNonNull(path, "path required")));
        return this;
    }

    public ResourceVisitor path(String path) {
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // strip off trailing slash
        if (path.matches(ABSOLUTE_PATH)) this.paths = new ArrayList<>(singleton(path)); // reset current path
        else this.paths.add(path.startsWith("/") ? path : "/" + path);
        return this;
    }

    @Override public ResourceVisitor param(String key, @Nullable Object value) {
        Objects.requireNonNull(key, "query param key required");
        if (value != null) queryParams.add(new Param(key, value));
        return this;
    }

    @Override public ResourceVisitor header(String key, @Nullable Object value) {
        Objects.requireNonNull(key, "header param key required");
        if (value != null) headerParams.add(new Param(key, value));
        return this;
    }

    @Override public ResourceVisitor form(String key, @Nullable Object value) {
        Objects.requireNonNull(key, "form param key required");
        if (value != null) formParams.add(new Param(key, value));
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
