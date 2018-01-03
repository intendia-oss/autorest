package com.intendia.gwt.autorest.client;

import com.google.gwt.http.client.*;
import com.intendia.gwt.autorest.client.mapper.JsJsonMapper;
import com.intendia.gwt.autorest.client.mapper.JsonMapper;
import com.intendia.gwt.autorest.client.mapper.MappersRegistry;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.annotations.Experimental;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.gwt.http.client.URL.encodeQueryString;
import static com.intendia.gwt.autorest.client.CollectorResourceVisitor.Param.expand;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Experimental
@SuppressWarnings("GwtInconsistentSerializableClass")
public class RequestResourceBuilder extends CollectorResourceVisitor {
    private static final List<Integer> DEFAULT_EXPECTED_STATUS = asList(200, 201, 204, 1223/*MSIE*/);
    private static final Func1<RequestBuilder, Request> DEFAULT_DISPATCHER = new MyDispatcher();
    private final JsJsonMapper jsJsonMapper = new JsJsonMapper();

    @FunctionalInterface
    private interface JsonDecoder<T> {
        T decode(String json, JsonMapper jsonMapper);
    }

    private static final JsonDecoder<Object> SINGLE_JSON_DECODER = (json, jsonMapper) -> jsonMapper.read(json);
    private static final JsonDecoder<Object[]> ARRAY_JSON_DECODER = (json, jsonMapper) -> jsonMapper.readAsArray(json);


    private Func1<Integer, Boolean> expectedStatuses;
    private Func1<RequestBuilder, Request> dispatcher;

    public RequestResourceBuilder() {
        super();
        this.expectedStatuses = DEFAULT_EXPECTED_STATUS::contains;
        this.dispatcher = DEFAULT_DISPATCHER;
    }

    public String query() {
        String q = "";
        for (Param p : expand(queryParams)) q += (q.isEmpty() ? "" : "&") + encode(p.k) + "=" + encode(p.v.toString());
        return q.isEmpty() ? "" : "?" + q;
    }

    private String encode(String str) {
        return encodeQueryString(str);
    }

    public String uri() {
        String uri = "";
        for (String path : paths) uri += path;
        return URL.encode(uri) + query();
    }

    public ResourceVisitor dispatcher(Func1<RequestBuilder, Request> dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    /**
     * Sets the expected response status code.  If the response status code does not match any of the values specified
     * then the request is considered to have failed.  Defaults to accepting 200,201,204. If set to -1 then any status
     * code is considered a success.
     */
    public ResourceVisitor expect(Integer... statuses) {
        if (statuses.length == 1 && statuses[0] < 0) expectedStatuses = status -> true;
        else expectedStatuses = asList(statuses)::contains;
        return this;
    }

    /**
     * Local file-system (file://) does not return any status codes. Therefore - if we read from the file-system we
     * accept all codes. This is for instance relevant when developing a PhoneGap application.
     */
    private boolean isExpected(String url, int status) {
        return url.startsWith("file") || expectedStatuses.call(status);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T as(Class<? super T> container, Class<?> type) {
        if (Completable.class.equals(container)) return (T) request().toCompletable();
        if (Single.class.equals(container)) return (T) request().map(context -> decodeSingle(context, type));
        if (Observable.class.equals(container)) return (T) request().toObservable().flatMapIterable(ctx -> {
            Object[] decode = decodeArray(ctx, type);
            return decode == null ? Collections.emptyList() : Arrays.asList(decode);
        });
        throw new UnsupportedOperationException("unsupported type " + container);
    }

    private <T> T decodeSingle(Context context, Class<?> type) {
        return decode(context, SINGLE_JSON_DECODER, getMapper(type));
    }

    private <T> T decodeArray(Context context, Class<?> type) {
        return decode(context, ARRAY_JSON_DECODER, getMapper(type));
    }

    protected JsonMapper getMapper(Class<?> type) {
        if (MappersRegistry.contains(type.getCanonicalName()))
            return MappersRegistry.get(type.getCanonicalName());
        return jsJsonMapper;
    }

    private @Nullable
    <T> T decode(Context ctx, JsonDecoder jsonDecoder, JsonMapper jsonMapper) {
        try {
            String text = requireNonNull(ctx.res).getText();
            return text == null || text.isEmpty() ? null : (T) jsonDecoder.decode(text, jsonMapper);
        } catch (Throwable e) {
            throw new ResponseFormatException(ctx, "Parsing response error", e);
        }
    }

    private Single<Context> request() {
        return Single.create(em -> {
            final Context ctx = new Context();
            try {
                MyRequestBuilder rb = new MyRequestBuilder(method, uri());
                rb.setRequestData(data == null ? null : getMapper(data.getClass()).write(data));
                for (Param h : headerParams) rb.setHeader(h.k, Objects.toString(h.v));
                if (rb.getHeader(CONTENT_TYPE) == null) rb.setHeader(CONTENT_TYPE, APPLICATION_JSON);
                if (rb.getHeader(ACCEPT) == null) rb.setHeader(ACCEPT, APPLICATION_JSON);
                rb.setCallback(new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request req, Response res) {
                        ctx.res = res;
                        if (isExpected(rb.getUrl(), res.getStatusCode())) {
                            em.onSuccess(ctx);
                        } else {
                            em.onError(new FailedStatusCodeException(ctx, res.getStatusText(), res.getStatusCode()));
                        }
                    }

                    @Override
                    public void onError(Request req1, Throwable e) {
                        if (!(e instanceof CanceledRequestException)) em.onError(e);
                    }
                });
                em.add(Subscriptions.create(() -> {
                    if (ctx.req != null && ctx.req.isPending()) {
                        ctx.req.cancel(); // fire canceled exception so decorated callbacks get notified
                        rb.getCallback().onError(ctx.req, new CanceledRequestException(ctx, "CANCELED"));
                    }
                }));
                ctx.req = dispatcher.call(rb);
            } catch (Throwable e) {
                em.onError(new RequestResponseException(ctx, "Request '" + uri() + "' error", e));
            }
        });
    }

    private static final class Context {
        @Nullable
        Request req;
        @Nullable
        Response res;
    }

    public static class RequestResponseException extends RuntimeException {
        private final Context req;

        public RequestResponseException(Context req, String msg, @Nullable Throwable c) {
            super(msg, c);
            this.req = req;
        }

        public @Nullable
        Request getRequest() {
            return req.req;
        }

        public @Nullable
        Response getResponse() {
            return req.res;
        }
    }

    public static class ResponseFormatException extends RequestResponseException {
        public ResponseFormatException(Context req, String msg, Throwable e) {
            super(req, msg, e);
        }
    }

    public static class FailedStatusCodeException extends RequestResponseException {
        private final int sc;

        public FailedStatusCodeException(Context req, String m, int sc) {
            super(req, m, null);
            this.sc = sc;
        }

        public int getStatusCode() {
            return sc;
        }
    }

    public static class CanceledRequestException extends RequestResponseException {
        public CanceledRequestException(Context req, String m) {
            super(req, m, null);
        }
    }

    private static class MyRequestBuilder extends RequestBuilder {
        MyRequestBuilder(String httpMethod, String url) {
            super(httpMethod, url);
        }
    }

    private static class MyDispatcher implements Func1<RequestBuilder, Request> {
        @Override
        public Request call(RequestBuilder requestBuilder) {
            try {
                return requestBuilder.send();
            } catch (RequestException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
