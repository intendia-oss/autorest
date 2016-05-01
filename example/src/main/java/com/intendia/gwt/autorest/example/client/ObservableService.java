package com.intendia.gwt.autorest.example.client;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.client.ResourceBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import rx.Observable;

@AutoRestGwt
@Path("observable")
public interface ObservableService {

    @PUT Observable<Void> ping();

    @GET Observable<Greeting> get();

    @POST Observable<Greeting> post(Greeting name);

    @Path("foo")
    @GET Observable<Greeting> getFoo();

    @Path("foo/{foo}")
    @GET Observable<Greeting> getFoo(
            @PathParam("foo") String foo,
            @QueryParam("bar") String bar,
            @QueryParam("unk") String oth);

    @com.google.gwt.core.shared.GwtIncompatible Response gwtIncompatible();

    @com.google.common.annotations.GwtIncompatible("serverOnly") Response guavaIncompatible();

    class Factory {
        public static ObservableService create(ResourceBuilder parent) {
            return new ObservableService_RestServiceProxy(parent.copy().header("mode", "observable"));
        }
    }
}
