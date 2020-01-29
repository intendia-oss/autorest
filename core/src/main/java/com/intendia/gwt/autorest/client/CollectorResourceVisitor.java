package com.intendia.gwt.autorest.client;

import static com.intendia.gwt.autorest.client.CollectorResourceVisitor.Param.expand;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.ws.rs.HttpMethod;

/* @Experimental */
public abstract class CollectorResourceVisitor implements ResourceVisitor {
    private static final List<Integer> DEFAULT_EXPECTED_STATUS = asList(200, 201, 204, 1223/*MSIE*/);

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

    protected final String base;
    protected List<String> paths = new ArrayList<>();
    protected List<Param> queryParams = new ArrayList<>();
    protected List<Param> headerParams = new ArrayList<>();
    protected List<Param> formParams = new ArrayList<>();
    protected String method = HttpMethod.GET;
    protected String[] produces = { "application/json" };
    protected String[] consumes = { "application/json" };
    protected Object data = null;
    private List<Integer> expectedStatuses = DEFAULT_EXPECTED_STATUS;

    protected CollectorResourceVisitor(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

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
        if (path.isEmpty()) throw new IllegalArgumentException("non-empty path required");
        this.paths.add(path);
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

    public String method() { return method; }

    public String uri() {
        String out = base;
        for (String pathComponent : paths) {
            out += "/" + encodeComponent(pathComponent);
        }
        return out + query();
    }

    public String query() {
        String q = encodeParams(queryParams);
        return q.isEmpty() ? "" : "?" + q;
    }

    protected String encodeParams(List<Param> params) {
        String out = "";
        for (Param p : expand(params)) {
            out += (out.isEmpty() ? "" : "&") + encodeComponent(p.k) + "=" + encodeComponent(Objects.toString(p.v));
        }
        return out;
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
        return method + " " + uri();
    }
}
