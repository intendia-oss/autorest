package com.intendia.gwt.autorest.client;

import static com.google.gwt.http.client.URL.encodeQueryString;
import static com.intendia.gwt.autorest.client.CollectorResourceVisitor.Param.expand;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jsinterop.annotations.JsMethod;
import rx.Observable;
import rx.Single;
import rx.Subscriber;
import rx.annotations.Experimental;
import rx.functions.Func1;
import rx.internal.producers.SingleDelayedProducer;
import rx.subscriptions.Subscriptions;

@Experimental @SuppressWarnings("GwtInconsistentSerializableClass")
public class RequestResourceBuilder extends CollectorResourceVisitor {
    private static final Logger log = Logger.getLogger(RequestResourceBuilder.class.getName());
    private static final List<Integer> DEFAULT_EXPECTED_STATUS = asList(200, 201, 204, 1223/*MSIE*/);
    private static final Func1<RequestBuilder, Request> DEFAULT_DISPATCHER = new MyDispatcher();

    private Func1<Integer, Boolean> expectedStatuses;
    private Func1<RequestBuilder, Request> dispatcher;

    public RequestResourceBuilder() {
        super();
        this.expectedStatuses = DEFAULT_EXPECTED_STATUS::contains;
        this.dispatcher = DEFAULT_DISPATCHER;
    }

    @Override @SuppressWarnings("unchecked") public <T> T as(Class<? super T> container, Class<?> type) {
        if (Single.class.equals(container)) return (T) single();
        if (Observable.class.equals(container)) return (T) observe();
        throw new UnsupportedOperationException("unsupported type " + container);
    }

    public <T> Observable<T> observe() {
        //noinspection Convert2MethodRef
        return Observable.<T[]>create(s -> createRequest(s))
                .flatMapIterable(o -> o == null ? singleton(null) : asList(o));
    }

    public <T> Single<T> single() {
        //noinspection Convert2MethodRef
        return Observable.<T>create(s -> createRequest(s)).toSingle();
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

    private <T> MethodRequest createRequest(Subscriber<T> s) {
        MyRequestBuilder rb = new MyRequestBuilder(method, uri());

        rb.setRequestData(data == null ? null : stringify(data));
        for (Param h : headerParams) rb.setHeader(h.k, Objects.toString(h.v));
        if (rb.getHeader(CONTENT_TYPE) == null) rb.setHeader(CONTENT_TYPE, APPLICATION_JSON);
        if (rb.getHeader(ACCEPT) == null) rb.setHeader(ACCEPT, APPLICATION_JSON);

        return new MethodRequest(s, dispatcher, rb, expectedStatuses);
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
    public ResourceVisitor expect(int... statuses) {
        if (statuses.length == 1 && statuses[0] < 0) expectedStatuses = status -> true;
        else expectedStatuses = asList(statuses)::contains;
        return this;
    }

    private static class MethodRequest {
        private final String url;
        private final Func1<Integer, Boolean> expectedStatuses;
        public @Nullable Response response;
        public @Nullable Request request;

        public <T> MethodRequest(Subscriber<T> s, Func1<RequestBuilder, Request> d, MyRequestBuilder rb,
                Func1<Integer, Boolean> expectedStatuses) {
            this.expectedStatuses = expectedStatuses;
            url = rb.getUrl();
            SingleDelayedProducer<T> producer = new SingleDelayedProducer<>(s);
            try {
                rb.setCallback(new RequestCallback() {

                    @Override public void onError(Request req, Throwable e) {
                        if (!(e instanceof CanceledRequestException)) s.onError(e);
                    }

                    @Override public void onResponseReceived(Request req, @Nullable Response res) {
                        MethodRequest mr = MethodRequest.this;
                        mr.response = res;
                        if (res == null) {
                            s.onError(new FailedStatusCodeException(mr, "TIMEOUT", 999));
                        } else if (!isExpected(res.getStatusCode())) {
                            s.onError(new FailedStatusCodeException(mr, res.getStatusText(), res.getStatusCode()));
                        } else {
                            try {
                                log.fine("Received http response for request: " + rb.getUrl());
                                String text = res.getText();
                                if (text == null || text.isEmpty()) {
                                    producer.setValue(null);
                                } else {
                                    producer.setValue(parse(text));
                                }
                            } catch (Throwable e) {
                                log.log(Level.FINE, "Could not parse response: " + e, e);
                                s.onError(new ResponseFormatException(mr, e));
                            }
                        }
                    }
                });
                s.setProducer(producer);
                s.add(Subscriptions.create(() -> {
                    if (request != null && request.isPending()) {
                        request.cancel(); // fire canceled exception so decorated callbacks get notified
                        rb.getCallback().onError(request, new CanceledRequestException(this, "CANCELED"));
                    }
                }));
                request = d.call(rb);
            } catch (Throwable e) {
                log.log(Level.FINE, "Received http error for: " + rb.getUrl(), e);
                s.onError(new RequestResponseException(this, e));
            }
        }

        /**
         * Local file-system (file://) does not return any status codes. Therefore - if we read from the file-system we
         * accept all codes. This is for instance relevant when developing a PhoneGap application.
         */
        private boolean isExpected(int status) {
            return url.startsWith("file") || expectedStatuses.call(status);
        }

    }

    public static class RequestResponseException extends RuntimeException {
        private final MethodRequest mr;
        public RequestResponseException(MethodRequest r, String m) { super(m); this.mr = r; }
        public RequestResponseException(MethodRequest r, Throwable c) { super(c); this.mr = r; }
        public @Nullable Request getRequest() { return mr.request; }
        public @Nullable Response getResponse() { return mr.response; }
    }

    public static class ResponseFormatException extends RequestResponseException {
        public ResponseFormatException(MethodRequest mr, Throwable e) { super(mr, e); }
    }

    public static class FailedStatusCodeException extends RequestResponseException {
        private final int statusCode;
        public FailedStatusCodeException(MethodRequest mr, String m, int sc) { super(mr, m); this.statusCode = sc; }
        public int getStatusCode() { return statusCode; }
    }

    public static class CanceledRequestException extends RequestResponseException {
        public CanceledRequestException(MethodRequest r, String m) { super(r, m); }
    }

    @JsMethod(namespace = "JSON")
    private static native <T> T parse(String text);

    @JsMethod(namespace = "JSON")
    private static native <T> T stringify(Object value);

    private static class MyRequestBuilder extends RequestBuilder {
        MyRequestBuilder(String httpMethod, String url) { super(httpMethod, url); }
    }

    private static class MyDispatcher implements Func1<RequestBuilder, Request> {
        @Override public Request call(RequestBuilder requestBuilder) {
            try { return requestBuilder.send(); } catch (RequestException e) { throw new RuntimeException(e); }
        }
    }
}
