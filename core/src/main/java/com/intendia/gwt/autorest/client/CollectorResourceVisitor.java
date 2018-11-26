package com.intendia.gwt.autorest.client;

import static com.intendia.gwt.autorest.client.CollectorResourceVisitor.Param.expand;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.ws.rs.HttpMethod;

/* @Experimental */
public abstract class CollectorResourceVisitor implements ResourceVisitor {
    private static final String ABSOLUTE_PATH = "[a-z][a-z0-9+.-]*:.*|//.*";
    private static final List<Integer> DEFAULT_EXPECTED_STATUS = asList(200, 201, 204, 1223/*MSIE*/);

    public static class Param<T> {
        public final String k;
        public final T v;
        public final TypeToken<T> t;
        public Param(String k, T v, TypeToken<T> t) { this.k = k; this.v = v; this.t = t; }
        
        public static List<Param<?>> expand(List<Param<?>> in) {
            List<Param<?>> out = new ArrayList<>();
            for (Param<?> p : in) {
                if (!(p.v instanceof Iterable<?>)) out.add(p);
                else for (Object v : ((Iterable<?>) p.v)) out.add(new Param<Object>(p.k, v, null));
            }
            return out;
        }
        @Override public String toString() { return "Param{k='" + k + "', v=" + v + ", t='" + t + '}'; }
    }

    protected List<Object> paths = new ArrayList<>();
    protected List<Param<?>> queryParams = new ArrayList<>();
    protected List<Param<?>> headerParams = new ArrayList<>();
    protected List<Param<?>> formParams = new ArrayList<>();
    protected String method = HttpMethod.GET;
    protected String produces[] = { "application/json" };
    protected String consumes[] = { "application/json" };
    protected Param<?> data = null;
    private List<Integer> expectedStatuses = DEFAULT_EXPECTED_STATUS;

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

    @Override public <T> ResourceVisitor path(@Nullable T value, TypeToken<T> typeToken) {
        if (value != null) paths.add(new Param<>(null, value, typeToken));
        return this;
    }

    @Override public ResourceVisitor produces(String... produces) {
        if (produces.length > 0 /*0 means undefined, so do not override default*/) this.produces = produces;
        return this;
    }

    @Override public ResourceVisitor consumes(String... consumes) {
        if (consumes.length > 0 /*0 means undefined, so do not override default*/) this.consumes = consumes;
        return this;
    }

    @Override public <T> ResourceVisitor param(String key, @Nullable T value, TypeToken<T> typeToken) {
        Objects.requireNonNull(key, "query param key required");
        if (value != null) queryParams.add(new Param<>(key, value, typeToken));
        return this;
    }

    @Override public <T> ResourceVisitor header(String key, @Nullable T value, TypeToken<T> typeToken) {
        Objects.requireNonNull(key, "header param key required");
        if (value != null) headerParams.add(new Param<>(key, value, typeToken));
        return this;
    }

    @Override public <T> ResourceVisitor form(String key, @Nullable T value, TypeToken<T> typeToken) {
        Objects.requireNonNull(key, "form param key required");
        if (value != null) formParams.add(new Param<>(key, value, typeToken));
        return this;
    }

    @Override public <T> ResourceVisitor data(T data, TypeToken<T> typeToken) {
        this.data = new Param<>(null, data, typeToken);
        return this;
    }

    public String method() { return method; }

    public String uri() {
        String path = "";
        for (Object p : paths) {
        	if (p instanceof Param<?>) {
        		Param<?> param = (Param<?>)p;
        		path += encodeComponent(Objects.toString(param.v));
        	} else
        		path += p;
        }
        
        return path + query();
    }

    public String query() {
        String q = encodeParams(queryParams);
        return q.isEmpty() ? "" : "?" + q;
    }

    protected String encodeParams(List<Param<?>> params) {
        String q = "";
        for (Param<?> p : expand(params)) {
            q += (q.isEmpty() ? "" : "&") + encodeComponent(p.k) + "=" + encodeComponent(Objects.toString(p.v));
        }
        return q;
    }

    protected abstract String encodeComponent(String str);

    /**
     * Sets the expected response status code.  If the response status code does not match any of the values specified
     * then the request is considered to have failed.  Defaults to accepting 200,201,204. If set to -1 then any status
     * code is considered a success.
     */
    public CollectorResourceVisitor expect(Integer... statuses) {
        if (statuses.length == 1 && statuses[0] < 0) expectedStatuses = emptyList();
        else expectedStatuses = asList(statuses);
        return this;
    }

    /**
     * Local file-system (file://) does not return any status codes. Therefore - if we read from the file-system we
     * accept all codes. This is for instance relevant when developing a PhoneGap application.
     */
    protected boolean isExpected(String url, int status) {
        return url.startsWith("file") || expectedStatuses.isEmpty() || expectedStatuses.contains(status);
    }

    @Override public String toString() {
        return method + " " + paths;
    }
}
