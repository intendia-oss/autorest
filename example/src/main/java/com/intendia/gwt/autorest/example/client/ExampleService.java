package com.intendia.gwt.autorest.example.client;

import static jsinterop.annotations.JsPackage.GLOBAL;

import java.util.List;
import java.util.Map;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import jsinterop.annotations.JsType;

@AutoRestGwt
@Path("observable")
public interface ExampleService {

    @PUT @Path("pingObservable") Observable<Void> pingObservable();

    @PUT @Path("pingMaybe") Maybe<Void> pingMaybe();

    @PUT @Path("pingCompletable") Completable pingCompletable();

    @POST @Path("observable") Observable<Greeting> post(Greeting name);

    @GET @Path("observable/foo") Observable<Greeting> getFoo();

    @GET @Path("observable/foo/{foo}") Observable<Greeting> getFoo(
            @PathParam("foo") String foo,
            @QueryParam("bar") String bar,
            @QueryParam("unk") String oth);
    
    @GET @Path("observable/foo/{foo}") Greeting getFoo(
            @PathParam("foo") String foo,
            @QueryParam("bar") String bar,
            @QueryParam("unk") Integer anInt,
            @QueryParam("iii") int i,
            @QueryParam("greeting") Greeting g);
    
    @GET @Path("observable/foo/{foo}") Greeting getFoo(
    		@PathParam("foo") String foo,
            @QueryParam("unk") List<String> oth,
            @QueryParam("greeting") List<Map<String, Greeting>> g);
    
    @POST @Path("observable/foo/{foo}") Greeting getFoo4(
    		@PathParam("foo") String foo,
            List<Greeting> oth[]);
    
    @JsType(namespace = GLOBAL, name = "Object", isNative = true) class Greeting {
        public String greeting;
    }

    // incompatible

    @com.google.gwt.core.shared.GwtIncompatible Response gwtIncompatible();

    @com.google.common.annotations.GwtIncompatible("serverOnly") Response guavaIncompatible();
}
