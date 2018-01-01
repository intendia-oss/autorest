package com.intendia.gwt.autorest.example.client;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.example.shared.JacksonGreeting;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@AutoRestGwt
@Path("observable")
public interface ObservableService {

    @PUT
    Observable<Void> ping();

    @GET
    Observable<Greeting> get();

    @POST
    Observable<Greeting> post(Greeting name);

    @Path("foo")
    @GET
    Observable<Greeting> getFoo();

    @Path("foo/{foo}")
    @GET
    Observable<Greeting> getFoo(
            @PathParam("foo") String foo,
            @QueryParam("bar") String bar,
            @QueryParam("unk") String oth);

    @POST
    Observable<JacksonGreeting> test(JacksonGreeting name);

    @com.google.gwt.core.shared.GwtIncompatible
    Response gwtIncompatible();

    @com.google.common.annotations.GwtIncompatible("serverOnly")
    Response guavaIncompatible();

}
