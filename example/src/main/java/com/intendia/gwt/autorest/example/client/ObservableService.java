package com.intendia.gwt.autorest.example.client;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.client.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import rx.Observable;

@AutoRestGwt
@Path("greeting-service")
public interface ObservableService {

    @PUT Observable<Void> ping();

    @GET Observable<Overlay> overlay();

    @POST Observable<Overlay> overlay(Overlay name);

    @GET Observable<Interface> iface();

    @POST Observable<Interface> iface(Interface name);

    @com.google.gwt.core.shared.GwtIncompatible Response gwtIncompatible();

    @com.google.common.annotations.GwtIncompatible("serverOnly") Response guavaIncompatible();

    class Factory {
        public static ObservableService create(Resource parent) {
            return new ObservableService_RestServiceProxy(parent, (method, builder) -> {
                builder.setHeader("mode", "observable");
                return builder.send();
            });
        }
    }
}
