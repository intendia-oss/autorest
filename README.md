# AutoREST, request metadata organizer

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.intendia.gwt.autorest/autorest-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.intendia.gwt.autorest/autorest-parent)
[![Build Status](https://travis-ci.org/intendia-oss/autorest.svg)](https://travis-ci.org/intendia-oss/autorest)
[![Join the chat at https://gitter.im/intendia-oss/autorest](https://badges.gitter.im/intendia-oss/autorest.svg)](https://gitter.im/intendia-oss/autorest?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

*A source code generator to organize and gather metadata from RESTful service calls (JSR311) and make it trivial
to build requests using any transport library.*

It helps organize because AutoREST uses a simplified JSR311 RESTful API definition which is easy to write and read. 
You can use this API in the server side using any JAX-RS implementation like [jersey](https://jersey.github.io/) and 
AutoREST will make it trivial to create your clients (JRE, Android or GWT) using this same JAX-RS API definition.
The lib includes a JRE (android compatible) and GWT clients, but it is strongly recommended seeing the source code 
and create your own request builder. 

To keep the project simple only part of the JSR311 is supported:
* *@Path* (params are supported but regex are not supported and there is no intention to do so)
* *@HttpMethod* (@GET, @POST, @PUT, @DELETE, @HEAD, @OPTIONS, actually any annotation annotated with @HttpMethod)
* *@PathParam*, *@QueryParam*, *@HeaderParam* and *@FormParam* (@MatrixParam not supported because it is impossible to know
 to which segment the matrix param applies, @Encoded still not supported)
* *@Consumer* and *@Producer*

All included implementations requires using [RxJava][rxjava] types (Observable, Single or Completable) as return types.
This is mandatory to share the same interface between the client and the server, because the server requires a 
synchronous return value, but the client requires an asynchronous one. [RxJava][rxjava] types allows to get both 
strategies using the same type. Also, those containers describe perfectly all available JSON responses, empty, one and 
many objects (see response containers) which make client implementation and response parsing much easier.

## Download

Releases are deployed to [the Central Repository][dl].

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

## What is this for?

Creating REST services using JAX-RS annotations in a breeze, this is a minimal working GWT example:

```java
public class ExampleEntryPoint implements EntryPoint {

    @AutoRestGwt @Path("search") interface Nominatim {
        @GET Observable<SearchResult> search(@QueryParam("q") String query, @QueryParam("format") String format);
    }

    @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
    public static class SearchResult {
        public String display_name; //ex: "Málaga, Provincia de Málaga, Andalusia, Spain",
        public String lat; //ex: "36.7210805",
        public String lon; //ex: "-4.4210409",
        public double importance; //ex: 0.73359836669253,
    }

    public void onModuleLoad() {
        Nominatim nominatim = new Nominatim_RestServiceModel(() -> osm());
        nominatim.search("Málaga,España", "json").subscribe(n -> {
            GWT.log("[" + (n.importance * 10.) + "] " + n.display_name + " (" + n.lon + "," + n.lat + ")");
        });
    }

    static ResourceVisitor osm() { return new RequestResourceBuilder().path("http://nominatim.openstreetmap.org/"); }
}
```

*AutoREST* will generate the proxy (Nominatim_RestServiceModel) and you get the awesome [RxJava][rxjava] API for free. 
If your have a java server side, you probably should try share the REST API (this is the whole idea), and if you are
using Jackson too, you can get inspired by [RxJavaJacksonSerializers][jackson].

## How is done?

You define the JAX-RS service interface...

```java
@AutoRestGwt @Path("orders")
public interface PizzaService {

    @POST Single<OrderConfirmation> createOrder(PizzaOrder request);

    @GET Observable<PizzaOrder> fetchOrders(@QueryParam("first") int first, @QueryParam("max") int max);

    @GET @Path("{id}") Single<PizzaOrder> fetchOrder(@PathParam("id") orderId);
}
```

And *AutoREST* generates the service model...

```java
public class PizzaService_RestServiceModel extends RestServiceModel implements PizzaService {

    public PizzaService_RestServiceModel(ResourceVisitor.Supplier parent) {
        super(() -> parent.get().path("orders"));
    }

    @POST Single<OrderConfirmation> createOrder(PizzaOrder request) {
        return method(POST).path().data(request).as(Single.class, OrderConfirmation.class);
    }

    @GET Observable<PizzaOrder> fetchOrders(@QueryParam("first") int first, @QueryParam("max") int max) {
        return method(GET).path().param("first", first).param("max", max).as(Observable.class, PizzaOrder.class);
    }

    @GET @Path("{id}") Single<PizzaOrder> fetchOrder(@PathParam("id") int orderId) {
        return method(GET).path(orderId).as(Single.class, PizzaOrder.class);
    }
}
```

This model map each resource method call all the way back to the root ``ResourceVisitor`` factory,
create a new visitor, visits each resource until the end point is reached and ends wrapping the result
into the expected type (i.e. traverses service call and gather all required metadata).

![AutoREST evaluation flow](https://github.com/intendia-oss/autorest/raw/master/autorest-flow.gif)

Everything looks quite simple, isn't it? This is important, keep it simple. If at any point something is not supported 
you can always implements it yourself. This project try to be just a boilerplate-reducer library and we have considered
that adding extension or configuration points will just fire complexity and that it is so damn simple to create a 
request from the metadata gathered by the AutoREST visitor that you better just create your own request builder 
implementation to customize the behaviour. The actual logic is in the transport and codec library you choose to use, 
so the request builder might be considered binding code between request metadata, transport and codec libraries. 
And this means that AutoREST has no configuration points because the whole request builder should be considered 
configuration itself.

If you create your own custom request builder you will only need to depend on AutoREST core, and at compile time on
the processor. If you are starting and the included request builder works perfect for you, you will need to depend on
the specific platform implementation module (JRE or GWT).   

## Response containers

This library is focused on JSON. So you should only expect 3 types of JSON responses, an empty/ignored body response,
a JSON Object response or a JSON Array response. This tree types will match perfectly with the Completable, Single
and Observable RxJava type, we call this the **container**. You are not limited to RxJava wrappers, but you should keep
in mind this 3 containers so you codec will handle the response as expected. The synchronous counterpart of this 3
containers are ``Void``, ``T`` and ``T[]``. The next table shows the recommended response/container matching strategy.

|           |Observable\<T>|Single\<T>|Completable|
| :-:       | :-:          | :-:      | :-:       |
|**[…]**    | **T(n items)** | Error* | Ignore    |
|**{…}**    | T(1 item)    | **T**    | Ignore    |
|**empty**  | Void(0 item) | Error    | **Ignore**|

**\*Error**: an JSON array should be handled by an stream wrapper (to keep things simpler), but you are in control of
the codec, so you might support array types like ``String[]``, so ``Single<String[]>`` will return the JSON arrays in a 
``Single`` wrapper.

**\*Ignore**: completable will always ignore the response only notifying a successfully response or error otherwise.

**Note**: the "T" row might be non object values like ``"some string"`` or ``123``. BUT it is recommended to only
support ``T`` types where ``T`` is not a primitive (boxed or not) nor an array, but is up to you support this
``Single<Integer>`` or ``Observable<Float>``.




 [dl]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.intendia.gwt.autorest%22
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [jackson]: https://gist.github.com/ibaca/71be7c73d8619d11182807b871c5975c
 [rxjava]: https://github.com/ReactiveX/RxJava
