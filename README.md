# AutoREST for GWT [![Build Status](https://travis-ci.org/intendia-oss/autorest-gwt.svg)](https://travis-ci.org/intendia-oss/autorest-gwt)

A source code generator for GWT compatible proxies from RESTful services (JSR311).

Fresh start of RestyGWT removing everything related to encoding/decoding which now is delegated to
``JSON.parse`` and ``JSON.stringify``. Thought to be used with JSO or JsInterop.

You define the service interface...

```java
@AutoRestGwt
@Path("foo")
public interface FooService {

    @PUT Observable<Void> ping();

    @GET Observable<Foo> getFoo();
}
```

And *autorest-gwt* generates the GWT service proxy...

```java
public class FooService_RestServiceProxy extends RestServiceProxy implements FooService {
    
    public FooService_RestServiceProxy(Resource resource, Dispatcher dispatcher) {
        super(resource, dispatcher, "greeting-service");
    }

    @PUT public Observable<Void> ping() {
        return resource().method("PUT").observe(getDispatcher());
    }

    @GET public Observable<Foo> getFoo() {
        return resource().method("GET").observe(getDispatcher());
    }
}
```
