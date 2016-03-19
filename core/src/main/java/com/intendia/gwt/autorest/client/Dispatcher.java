package com.intendia.gwt.autorest.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;

public interface Dispatcher {
    Request send(Method method, RequestBuilder builder) throws RequestException;

    class DefaultDispatcher implements Dispatcher {
        public static final Dispatcher INSTANCE = new DefaultDispatcher();

        @Override
        public Request send(Method method, RequestBuilder builder) throws RequestException {
            return builder.send();
        }
    }
}
