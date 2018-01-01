package com.intendia.gwt.autorest.example.client;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.example.shared.JacksonGreeting;
import rx.Observable;
import rx.Single;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@AutoRestGwt
@Path("single")
public interface SingleService {

    @PUT
    Single<Void> ping();

    @GET
    Single<Greeting> get();

    @POST
    Single<Greeting> put(Greeting name);

    @POST
    Single<JacksonGreeting> put(JacksonGreeting name);

    @GET
    Observable<JacksonGreeting> getJackson();


    @com.google.gwt.core.shared.GwtIncompatible
    Response gwtIncompatible();

    @com.google.common.annotations.GwtIncompatible("serverOnly")
    Response guavaIncompatible();

    @POST
    Single<Greeting> postForm(@FormParam("name") String name);
}
