# AutoREST for GWT [![Build Status](https://travis-ci.org/intendia-oss/autorest-gwt.svg)](https://travis-ci.org/intendia-oss/autorest-gwt)

A source code generator for GWT compatible proxies from RESTful services (JSR311).

## Download

Releases are deployed to [the Central Repository][dl].

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].

## What is this for?

Fresh start of RestyGWT removing everything related to encoding/decoding which now is delegated to
``JSON.parse`` and ``JSON.stringify``. Thought to be used with JSO or JsInterop.

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

And *autorest-gwt* generates the GWT service proxy...

```java
public class PizzaService_RestServiceProxy extends RestServiceProxy implements PizzaService {
    
    public PizzaService_RestServiceProxy(Resource resource, Dispatcher dispatcher) {
        super(resource, dispatcher, "orders");
    }

    @POST Single<OrderConfirmation> createOrder(PizzaOrder request) {
        return resolve().method(POST).data(request).observe(dispatcher());
    }

    @GET Observable<PizzaOrder> fetchOrders(@QueryParam("first") int first, @QueryParam("max") int max) {
        return resolve().param("first",first).param("max",max).method(GET).observe(dispatcher());
    }
    
    @GET @Path("{id}") Single<PizzaOrder> fetchOrder(@PathParam("id") orderId) {
        return resolve(orderId).method(GET).single(dispatcher()); 
    }
}
```



 [dl]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.intendia.gwt%22%20AND%20a%3A%22autorest-gwt%22
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
 
