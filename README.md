# AutoREST for GWT 

This is a fork from [autorest](https://github.com/intendia-oss/autorest) for adding [gwt-jackson-apt](https://github.com/vegegoku/gwt-jackson-apt) support.


## Usage:

You have to add these dependencies:
```xml
        <dependency>
            <groupId>com.progressoft.brix.domino.gwtjackson</groupId>
            <artifactId>gwt-jackson-apt-api</artifactId>
            <version>${gwt.jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.progressoft.brix.domino.gwtjackson</groupId>
            <artifactId>gwt-jackson-apt-processor</artifactId>
            <version>${gwt.jackson.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.intendia.gwt.autorest</groupId>
            <artifactId>autorest-gwt-processor</artifactId>
            <version>HEAD-SNAPSHOT</version>
        </dependency>
```

Now it is supporting gwt-jackson by simple create a simple bean and use it like this:

```java

public class ExampleEntryPoint implements EntryPoint {

    @AutoRestGwt @Path("search") interface Nominatim {
        @GET Observable<SearchResult> search(@QueryParam("q") String query, @QueryParam("format") String format);
    }

    public class SearchResult {
        public String display_name;
        public String lat;
        public String lon;
        public double importance;
    }

    public void onModuleLoad() {
        Nominatim nominatim = new Nominatim_RestServiceModel(() -> osm());
        nominatim.search("Málaga,España", "json").subscribe(n -> {
            GWT.log("[" + (int) (n.importance * 10.) + "] " + n.display_name + " (" + n.lon + "," + n.lat + ")");
        });
    }

    static ResourceVisitor osm() { return new RequestResourceBuilder().path("http://nominatim.openstreetmap.org/"); }
}

```

You need to add `package-info.java` in your `client` package:

```java
@JacksonConfiguration
package com.foo.client;

import com.intendia.gwt.autorest.client.jackson.JacksonConfiguration;
```
