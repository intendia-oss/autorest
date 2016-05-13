package com.intendia.gwt.autorest.client;

import static com.google.gwt.core.client.GWT.getHostPageBaseURL;
import static com.google.gwt.http.client.URL.encodeQueryString;
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
import java.util.Map;
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

@Experimental
public class RequestResourceVisitor extends CollectorResourceVisitor {
    private static final Logger log = Logger.getLogger(RequestResourceVisitor.class.getName());
    private static final List<Integer> DEFAULT_EXPECTED_STATUS = asList(200, 201, 204, 1223/*MSIE*/);
    private static final Func1<RequestBuilder, Request> DEFAULT_DISPATCHER = new MyDispatcher();

    private Func1<Integer, Boolean> expectedStatuses;
    private Func1<RequestBuilder, Request> dispatcher;

    public RequestResourceVisitor() {
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
        return Observable.<T[]>create((s) -> createRequest(s))
                .flatMapIterable(o -> o == null ? singleton(null) : asList(o));
    }

    public <T> Single<T> single() {
        //noinspection Convert2MethodRef
        return Observable.<T>create((s) -> createRequest(s)).toSingle();
    }

    protected String query() {
        String query = "";
        for (Param param : params) {
            query += (query.isEmpty() ? "" : "&") + encodeQueryString(param.key) + "=" + encodeQueryString(param.value);
        }
        return query.isEmpty() ? "" : "?" + query;
    }

    protected String uri() {
        String uri = "";
        for (String path : paths) uri += path;
        return URL.encode(uri) + query();
    }

    private <T> MethodRequest createRequest(Subscriber<T> s) {
        MyRequestBuilder rb = new MyRequestBuilder(method, uri());

        rb.setRequestData(data == null ? null : stringify(data));
        for (Map.Entry<String, String> h : headers.entrySet()) rb.setHeader(h.getKey(), h.getValue());
        if (!headers.containsKey(CONTENT_TYPE)) rb.setHeader(CONTENT_TYPE, APPLICATION_JSON);
        if (!headers.containsKey(ACCEPT)) rb.setHeader(ACCEPT, APPLICATION_JSON);

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

                    @Override public void onError(Request request, Throwable exception) {
                        s.onError(exception);
                    }

                    @Override public void onResponseReceived(Request request, @Nullable Response response) {
                        MethodRequest.this.response = response;
                        if (response == null) {
                            s.onError(new FailedStatusCodeException("TIMEOUT", 999));
                        } else if (!isExpected(response.getStatusCode())) {
                            s.onError(new FailedResponseException(response.getStatusText(), response.getStatusCode()));
                        } else {
                            try {
                                log.fine("Received http response for request: " + rb.getUrl());
                                String text = response.getText();
                                if (text == null || text.isEmpty()) {
                                    producer.setValue(null);
                                } else {
                                    producer.setValue(parse(text));
                                }
                            } catch (Throwable e) {
                                log.log(Level.FINE, "Could not parse response: " + e, e);
                                s.onError(new ResponseFormatException(e));
                            }
                        }
                    }
                });
                s.setProducer(producer);
                s.add(Subscriptions.create(() -> {
                    if (request != null) request.cancel();
                }));
                request = d.call(rb);
            } catch (Throwable e) {
                log.log(Level.FINE, "Received http error for: " + rb.getUrl(), e);
                s.onError(new RequestResponseException(e));
            }
        }

        /**
         * Local file-system (file://) does not return any status codes. Therefore - if we read from the file-system we
         * accept all codes. This is for instance relevant when developing a PhoneGap application.
         */
        private boolean isExpected(int status) {
            return isRequestGoingToFileSystem(getHostPageBaseURL(), url) || expectedStatuses.call(status);
        }

        private static boolean isRequestGoingToFileSystem(String baseUrl, String requestUrl) {
            return requestUrl.startsWith("file") ||
                    (baseUrl.startsWith("file") && (requestUrl.startsWith("/") || requestUrl.startsWith(".")));
        }

        public class RequestResponseException extends RuntimeException {
            public RequestResponseException() { }
            public RequestResponseException(String message) { super(message); }
            public RequestResponseException(String message, Throwable cause) { super(message, cause); }
            public RequestResponseException(Throwable cause) { super(cause); }
            public @Nullable Request getRequest() { return request; }
            public @Nullable Response getResponse() { return response; }
        }

        public class ResponseFormatException extends RequestResponseException {
            public ResponseFormatException() {}
            public ResponseFormatException(Throwable e) { super(e); }
        }

        public class FailedStatusCodeException extends RequestResponseException {
            private final int statusCode;
            public FailedStatusCodeException() { this.statusCode = 0; }
            public FailedStatusCodeException(String message, int sc) { super(message); this.statusCode = sc; }
            public int getStatusCode() { return statusCode; }
        }

        public class FailedResponseException extends FailedStatusCodeException {
            public FailedResponseException() {}
            public FailedResponseException(String statusText, int statusCode) { super(statusText, statusCode); }
        }
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
