package com.intendia.gwt.autorest.client;

import static com.google.gwt.http.client.URL.encodeQueryString;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class Resource {
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    private final String path;
    private final @Nullable String query;
    private final Map<String, String> headers;

    public Resource(String uri) {
        this(uri, null, null);
    }

    public Resource(String uri, @Nullable String query, @Nullable Map<String, String> headers) {
        // Strip off trailing "/" so we have a known format to work off of when concatenating paths
        this.path = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
        this.query = query;
        this.headers = (headers != null) ? headers : Collections.emptyMap();
    }

    public Method method(String method) {
        return new Method(this, method);
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query != null ? query : "";
    }

    public String getUri() {
        return query != null ? path + "?" + query : path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Resource resolve(String path) {
        // it might be an absolute path...
        if (path.startsWith("http:") || path.startsWith("https:") || path.startsWith("file:")) {
            return new Resource(path, query, headers);
        }
        // strip prefix / if needed...
        else return new Resource(this.path + "/" + (path.startsWith("/") ? path.substring(1) : path), query, headers);
    }

    public Resource param(String key, @Nullable Object value) {
        if (value == null) return this;
        else if (value instanceof Iterable<?>) return addQueryParams(key, (Iterable<?>) value);
        else return addQueryParam(key, Objects.toString(value));
    }

    private Resource addQueryParam(String key, String value) {
        return value == null ? this : new Resource(path,
                (query == null ? "" : query + "&") + encodeQueryString(key) + "=" + encodeQueryString(value), headers);
    }

    private Resource addQueryParams(String key, Iterable<?> values) {
        if (values == null) return this;
        key = encodeQueryString(key);
        StringBuilder q = new StringBuilder(query == null ? "" : query + "&");
        boolean ampersand = false;
        for (Object value : values) {
            if (value == null) continue;
            if (ampersand) {
                q.append('&');
            } else {
                ampersand = true;
            }
            value = encodeQueryString(Objects.toString(value));
            q.append(key).append("=").append(value);
        }

        return new Resource(path, q.toString(), headers);
    }
}
