package com.intendia.gwt.autorest.client;

import static com.google.gwt.core.client.GWT.getHostPageBaseURL;
import static java.util.Collections.singleton;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jsinterop.annotations.JsMethod;
import rx.Observable;
import rx.Single;
import rx.Subscriber;
import rx.functions.Func1;
import rx.internal.producers.SingleDelayedProducer;
import rx.subscriptions.Subscriptions;

public class Method {
    private static final Logger log = Logger.getLogger(Method.class.getName());
    private static final List<Integer> DEFAULT_EXPECTED_STATUS = Arrays.asList(200, 201, 204, 1223/*MSIE*/);

    static class MyRequestBuilder extends RequestBuilder {
        protected MyRequestBuilder(String httpMethod, String url) {
            super(httpMethod, url);
        }
    }

    public final RequestBuilder builder;
    private Func1<Integer, Boolean> expectedStatuses = DEFAULT_EXPECTED_STATUS::contains;

    public Method(Resource resource, String method) {
        builder = new MyRequestBuilder(method, resource.getUri());
        builder.setRequestData(null); // just in case
        for (Entry<String, String> e : resource.getHeaders().entrySet()) {
            header(e.getKey(), e.getValue());
        }
    }

    public Method header(String header, String value) {
        builder.setHeader(header, value);
        return this;
    }

    public Method accept(String value) {
        return header(Resource.HEADER_ACCEPT, value);
    }

    public Method data(Object data) {
        defaultContentType(Resource.CONTENT_TYPE_JSON);
        builder.setRequestData(stringify(data));
        return this;
    }

    /**
     * Sets the expected response status code.  If the response status code does not match any of the values specified
     * then the request is considered to have failed.  Defaults to accepting 200,201,204. If set to -1 then any status
     * code is considered a success.
     */
    public Method expect(int... statuses) {
        if (statuses.length == 1 && statuses[0] < 0) expectedStatuses = status -> true;
        else expectedStatuses = Arrays.asList(statuses)::contains;
        return this;
    }

    public <T> Observable<T> observe(Dispatcher d) {
        defaultAcceptType(Resource.CONTENT_TYPE_JSON);
        return Observable.<T[]>create(s -> new LiveRequest(s, d))
                .flatMapIterable(o -> o == null ? singleton(null) : Method.asJavaList(o));
    }

    public <T> Single<T> single(Dispatcher d) {
        defaultAcceptType(Resource.CONTENT_TYPE_JSON);
        return Observable.<T>create(s -> new LiveRequest(s, d)).toSingle();
    }

    /**
     * Local file-system (file://) does not return any status codes. Therefore - if we read from the file-system we
     * accept all codes. This is for instance relevant when developing a PhoneGap application.
     */
    boolean isExpected(int status) {
        return isRequestGoingToFileSystem(getHostPageBaseURL(), builder.getUrl()) || expectedStatuses.call(status);
    }

    static boolean isRequestGoingToFileSystem(String baseUrl, String requestUrl) {
        return requestUrl.startsWith("file") ||
                (baseUrl.startsWith("file") && (requestUrl.startsWith("/") || requestUrl.startsWith(".")));
    }

    void defaultContentType(String type) {
        if (builder.getHeader(Resource.HEADER_CONTENT_TYPE) == null) header(Resource.HEADER_CONTENT_TYPE, type);
    }

    void defaultAcceptType(String type) {
        if (builder.getHeader(Resource.HEADER_ACCEPT) == null) header(Resource.HEADER_ACCEPT, type);
    }

    @Override
    public String toString() {
        return builder.getHTTPMethod() + " " + builder.getUrl();
    }

    class LiveRequest {
        public @Nullable Response response;
        public @Nullable Request request;
        public <T> LiveRequest(Subscriber<T> s, Dispatcher d) {
            SingleDelayedProducer<T> producer = new SingleDelayedProducer<>(s);
            try {
                builder.setCallback(new RequestCallback() {
                    Method method = Method.this;

                    @Override public void onError(Request request, Throwable exception) {
                        s.onError(exception);
                    }

                    @Override public void onResponseReceived(Request request, @Nullable Response response) {
                        LiveRequest.this.response = response;
                        if (response == null) {
                            s.onError(new FailedStatusCodeException("TIMEOUT", 999));
                        } else if (!method.isExpected(response.getStatusCode())) {
                            s.onError(new FailedResponseException(response.getStatusText(), response.getStatusCode()));
                        } else {
                            try {
                                log.fine("Received http response for request: " + method.toString());
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
                s.add(Subscriptions.create(() -> { if (request != null) request.cancel(); }));
                request = d.send(Method.this, builder);
            } catch (Throwable e) {
                log.log(Level.FINE, "Received http error for: " + Method.this.toString(), e);
                s.onError(new RequestResponseException(e));
            }
        }

        public class RequestResponseException extends RuntimeException {
            public RequestResponseException() { }
            public RequestResponseException(String message) { super(message); }
            public RequestResponseException(String message, Throwable cause) { super(message, cause); }
            public RequestResponseException(Throwable cause) { super(cause); }
            public Method getMethod() { return Method.this; }
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

    private static native <T> List<T> asJavaList(T[] o) /*-{
        var l = @java.util.ArrayList::new()();
        l.@java.util.ArrayList::array = o;
        return l;
    }-*/;
}
