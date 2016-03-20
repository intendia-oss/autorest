package com.intendia.gwt.autorest.client;

import static com.google.gwt.http.client.URL.encodeQueryString;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class Resource {
    static final String CONTENT_TYPE_JSON = "application/json";
    static final String HEADER_ACCEPT = "Accept";
    static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final String path;
    private final Map<String, String> headers = new HashMap<>();
    private final List<String> params = new ArrayList<>();

    public Resource(String uri) {
        // Strip off trailing "/" so we have a known format to work off of when concatenating paths
        this.path = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    public String getUri() {
        return path + getQuery();
    }

    private String getQuery() {
        String query = "";
        for (String param : params) query += (query.isEmpty() ? "" : "&") + param;
        return query.isEmpty() ? "" : "?" + query;
    }

    public Resource header(String key, String value) {
        headers.put(requireNonNull(key, "header key requred"), requireNonNull(value, "header value required"));
        return this;
    }

    public Resource header(Map<String, String> headers) {
        for (Map.Entry<String, String> e : headers.entrySet()) header(e.getKey(), e.getValue());
        return this;
    }

    public Resource param(String key, @Nullable Object value) {
        if (value instanceof Iterable<?>) for (Object v : ((Iterable<?>) value)) param(key, v);
        else if (value != null) params.add(encodeQueryString(key) + "=" + encodeQueryString(Objects.toString(value)));
        return this;
    }

    public Method method(String method) {
        final Method m = new Method(getUri(), method);
        for (Map.Entry<String, String> e : headers.entrySet()) m.header(e.getKey(), e.getValue());
        return m;
    }

    public Resource resolve(Object path) {
        return resolve(requireNonNull(path, "path required").toString());
    }

    public Resource resolve(String path) {
        // it might be an absolute path...
        if (path.startsWith("http:") || path.startsWith("https:") || path.startsWith("file:")) {
            return new Resource(path).header(headers);
        }
        // strip prefix / if needed...
        else return new Resource(this.path + "/" + (path.startsWith("/") ? path.substring(1) : path)).header(headers);
    }
}
