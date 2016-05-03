# AutoREST for GWT [![Build Status](https://travis-ci.org/intendia-oss/autorest-gwt.svg)](https://travis-ci.org/intendia-oss/autorest-gwt)

*A source code generator for GWT compatible proxies from RESTful services (JSR311).*

This porject is a fresh start of *RestyGWT* removing everything related to
encoding/decoding which now is delegated to ``JSON.parse`` and
``JSON.stringify``. Thought to be used with JSO or JsInterop.

To keep the project simple only part of the JSR311 will be supported:
* @Path (regex not supported and there is no intention to do so)
* @HttpMethod (so all @GET, @POST...)
* @PathParam and @QueryParam (other params will be supported)
* @Consumer and @Producer are currently ignored (treated both and always as 'application/json')

Only [RxJava][rxjava] types (Observable and Single) can be used as return value.
This is mandatory to share the same interface between the client and the server,
usually the server requires a synchronous return value but the client requires
an asynchronous response. [RxJava][rxjava] types allows to get both strategies
using the same type.

## Download

Releases are deployed to [the Central Repository][dl].

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

## What is this for?

Creating REST services using JAX-RS annotations in a breeze, this is a minimal working example:

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
        ResourceBuilder root = new RequestResourceBuilder().path("http://nominatim.openstreetmap.org/");
        Nominatim nominatim = new Nominatim_RestServiceProxy(root);
        nominatim.search("Málaga,España", "json").subscribe(n -> {
            GWT.log("[" + (int) (n.importance * 10.) + "] " + n.display_name + " (" + n.lon + "," + n.lat + ")");
        });
    }
}
```

*AutoREST* will generate the proxy (Nominatim_RestServiceProxy) and you get the
awesome [RxJava][rxjava] API for free. If your have a java server side, you
probably should try share the REST API (this is the whole idea), and if you are
using Jackson too, you can get inspired by [RxJavaJacksonSerializers][jackson].

## How is done?

You define the service interface...

```java
@AutoRestGwt
@Path("orders")
public interface PizzaService {

    @POST Single<OrderConfirmation> createOrder(PizzaOrder request);

    @GET Observable<PizzaOrder> fetchOrders(@QueryParam("first") int first, @QueryParam("max") int max);

    @GET @Path("{id}") Single<PizzaOrder> fetchOrder(@PathParam("id") orderId);
}
```

And *AutoREST* generates the GWT service proxy...

```java
public class PizzaService_RestServiceProxy extends RestServiceProxy implements PizzaService {

    public PizzaService_RestServiceProxy(ResourceBuilder resource) {
        super(resource, "orders");
    }

    @POST Single<OrderConfirmation> createOrder(PizzaOrder request) {
        return resolve().method(POST).data(request).build(Single.class);
    }

    @GET Observable<PizzaOrder> fetchOrders(@QueryParam("first") int first, @QueryParam("max") int max) {
        return resolve().param("first",first).param("max",max).method(GET).build(Observable.class);
    }

    @GET @Path("{id}") Single<PizzaOrder> fetchOrder(@PathParam("id") orderId) {
        return resolve(orderId).method(GET).build(Single.class);
    }
}
```

Everything looks quite simple, isn't it? This is important, keep it simple. If
at any point something is not supported you can always implements it yourserlf.
This project try to be just a boilerplate-reducer library, the unique actual
logic is the ``com.google.gwt.http.client.RequestBuilder`` to ``rx.Producer``code.



 [dl]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.intendia.gwt.autorest%22%20AND%20a%3A%22autorest-gwt%22
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 [jackson]: https://gist.github.com/ibaca/71be7c73d8619d11182807b871c5975c
 [rxjava]: https://github.com/ReactiveX/RxJava
