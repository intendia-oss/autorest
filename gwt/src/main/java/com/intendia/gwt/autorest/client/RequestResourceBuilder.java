package com.intendia.gwt.autorest.client;

import static com.intendia.gwt.autorest.client.CollectorResourceVisitor.Param.expand;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import elemental2.core.Global;
import elemental2.dom.FormData;
import elemental2.dom.XMLHttpRequest;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import jsinterop.base.Js;

@SuppressWarnings("GwtInconsistentSerializableClass")
public class RequestResourceBuilder extends CollectorResourceVisitor {
    private static final List<Integer> DEFAULT_EXPECTED_STATUS = asList(200, 201, 204, 1223/*MSIE*/);
    private static final Consumer<XMLHttpRequest> DEFAULT_DISPATCHER = request -> { /* no op */ };

    private Function<Integer, Boolean> expectedStatuses = DEFAULT_EXPECTED_STATUS::contains;
    private Consumer<XMLHttpRequest> dispatcher = DEFAULT_DISPATCHER;

    public String query() {
        String q = "";
        for (Param p : expand(queryParams)) q += (q.isEmpty() ? "" : "&") + encode(p.k) + "=" + encode(p.v.toString());
        return q.isEmpty() ? "" : "?" + q;
    }

    private String encode(String decodedURLComponent) {
        return Global.encodeURIComponent(decodedURLComponent).replaceAll("%20", "+");
    }

    public String uri() {
        return Global.encodeURI(paths.stream().collect(Collectors.joining())) + query();
    }

    public RequestResourceBuilder dispatcher(Consumer<XMLHttpRequest> dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    /**
     * Sets the expected response status code.  If the response status code does not match any of the values specified
     * then the request is considered to have failed.  Defaults to accepting 200,201,204. If set to -1 then any status
     * code is considered a success.
     */
    public RequestResourceBuilder expect(Integer... statuses) {
        if (statuses.length == 1 && statuses[0] < 0) expectedStatuses = status -> true;
        else expectedStatuses = asList(statuses)::contains;
        return this;
    }

    /**
     * Local file-system (file://) does not return any status codes. Therefore - if we read from the file-system we
     * accept all codes. This is for instance relevant when developing a PhoneGap application.
     */
    private boolean isExpected(String url, int status) {
        return url.startsWith("file") || expectedStatuses.apply(status);
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T as(Class<? super T> container, Class<?> type) {
        if (Completable.class.equals(container)) return (T) request().toCompletable();
        if (Maybe.class.equals(container)) return (T) request().flatMapMaybe(ctx -> {
            @Nullable Object decode = decode(ctx);
            return decode == null ? Maybe.empty() : Maybe.just(decode);
        });
        if (Single.class.equals(container)) return (T) request().map(ctx -> {
            @Nullable Object decode = decode(ctx);
            return requireNonNull(decode, "null response forbidden, use Maybe instead");
        });
        if (Observable.class.equals(container)) return (T) request().toObservable().flatMapIterable(ctx -> {
            @Nullable Object[] decode = decode(ctx);
            return decode == null ? Collections.emptyList() : Arrays.asList(decode);
        });
        throw new UnsupportedOperationException("unsupported type " + container);
    }

    private @Nullable <T> T decode(XMLHttpRequest ctx) {
        try {
            String text = ctx.response.asString();
            return text == null || text.isEmpty() ? null : Js.cast(Global.JSON.parse(text));
        } catch (Throwable e) {
            throw new RequestResponseException.ResponseFormatException("Parsing response error", e);
        }
    }

    public Single<XMLHttpRequest> request() {
        return Single.create(em -> {
            String uri = uri();
            XMLHttpRequest xhr = new XMLHttpRequest();
            xhr.open(method, uri);

            Map<String, String> headers = new HashMap<>();
            for (Param h : headerParams) headers.put(h.k, Objects.toString(h.v));
            for (Map.Entry<String, String> h : headers.entrySet()) xhr.setRequestHeader(h.getKey(), h.getValue());

            try {
                xhr.onreadystatechange = evt -> {
                    if (em.isDisposed()) return null;
                    if (xhr.readyState == XMLHttpRequest.DONE) {
                        if (isExpected(uri, xhr.status)) em.onSuccess(xhr);
                        else em.tryOnError(
                                new RequestResponseException.FailedStatusCodeException(xhr.status, xhr.responseText, xhr.statusText));
                    }
                    return null;
                };
                em.setCancellable(() -> {
                    if (xhr.readyState != XMLHttpRequest.DONE) xhr.abort();
                });

                dispatcher.accept(xhr);

                if (!formParams.isEmpty()) {
                    xhr.setRequestHeader(CONTENT_TYPE, MULTIPART_FORM_DATA);
                    FormData form = new FormData();
                    formParams.forEach(p -> form.append(p.k, Objects.toString(p.v)));
                    xhr.send(form);
                } else {
                    if (!headers.containsKey(CONTENT_TYPE)) xhr.setRequestHeader(CONTENT_TYPE, APPLICATION_JSON);
                    if (!headers.containsKey(ACCEPT)) xhr.setRequestHeader(ACCEPT, APPLICATION_JSON);
                    if (data != null) xhr.send(Global.JSON.stringify(data));
                    else xhr.send();
                }
            } catch (Throwable e) {
                em.tryOnError(new RequestResponseException("", e));
            }
        });
    }
}
