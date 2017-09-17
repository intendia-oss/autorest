package com.intendia.gwt.autorest.client;

import static javax.ws.rs.HttpMethod.GET;

import com.intendia.gwt.autorest.client.Metadata.K;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@AutoRestGwt @Path("a") @Produces("*/*") @Consumes("*/*")
public class TestService_RestServiceModel_NG extends RestServiceModel implements ResourceVisitorTest.TestService {
    public static final K<Metadata> RESOURCE_CLASS = Metadata.K.key();
    public static final K<String> HTTP_METHOD = Metadata.K.key();
    public static final K<String[]> PRODUCES = Metadata.K.key();
    public static final K<String[]> CONSUMES = Metadata.K.key();
    public static final K<String[]> PATH_TEMPLATE = Metadata.K.key();
    public static final K<String[]> PATH_PARAMS = Metadata.K.key();
    public static final K<Object[]> PATH_VALUES = Metadata.K.key();
    public static final K<String[]> QUERY_PARAMS = Metadata.K.key();
    public static final K<Object[]> QUERY_VALUES = Metadata.K.key();
    public static final K<String[]> HEADER_PARAMS = Metadata.K.key();
    public static final K<Object[]> HEADER_VALUES = Metadata.K.key();

    @Inject
    public TestService_RestServiceModel_NG(final ResourceVisitor.Factory parent) {
        super(parent);
    }

    // el ojetivo es recolectar informacion de cada end-point (cada metodo)
    // hay dos tipos de información, informacion compile-time y run-time
    // compile-time: path, params, produces, consumers, etc.
    // run-time: a partir de la info compile-time se le añade el valor de los parametros (query, path, header, body...)
    //
    // con esta información se debe llegar a poder hacer 3 cosas
    // * configurar y lanzar un request y decodificar la respuest encapsulandolo en un tipo RX
    // * poder crear un servicio que escuche los request y pueda redirigir a una implementación de este interfaz
    // * poder generar documentacion de todos los end-points (eg. generar un esquema open-api)

    public static Metadata resource_class_metadata = new Metadata() {
        @Override public @Nullable <T> T get(K<T> k) {
            if (k == PATH_TEMPLATE) return k.cast(new String[] { "a" });
            return null;
        }
    };

    @Override
    @Produces("application/json") @Consumes("application/json")
    @javax.ws.rs.GET @Path("b/{pS}/{pI}/c") public List<String> foo(
            @PathParam("pS") String pS, @PathParam("pI") int pI,
            @QueryParam("qS") String qS, @QueryParam("qI") int qI, @QueryParam("qIs") List<Integer> qIs,
            @HeaderParam("hS") String hS, @HeaderParam("hI") int hI) {
//        return method(GET) // compile-time
        // requestFactory.apply(resource_method_foo_metadata);
        return request.create(resource_method_foo_metadata)
                // compile-time
//                .method(GET)
//                .produces("application/json")
//                .consumes("application/json")
//                .path("b", pS, pI, "c")
//                .param("qS", qS)
//                .param("qI", qI)
//                .param("qIs", qIs)
//                .header("hS", hS)
//                .header("hI", hI)
                .as(List.class, String.class);
    }

    public static Metadata resource_method_foo_metadata = new Metadata() {
        @Override public @Nullable <T> T get(K<T> k) {
            if (k == RESOURCE_CLASS) return k.cast(resource_class_metadata);
            if (k == HTTP_METHOD) return k.cast(GET);
            if (k == PRODUCES) return k.cast("application/json");
            if (k == CONSUMES) return k.cast("application/json");
            if (k == PATH_TEMPLATE) return k.cast(new String[] { "b", "{pS}", "{pI}", "c" });
            if (k == PATH_PARAMS) return k.cast(new String[] { "b", "{pS}", "{pI}", "c" });
            if (k == QUERY_PARAMS) return k.cast(new String[] { "qS", "qI", "qIs" });
            if (k == HEADER_PARAMS) return k.cast(new String[] { "hS", "hI" });
            return null;
        }
    };

    public static Metadata resource_method_foo_metadata(String pS, int pI, String qS, int qI, List<Integer> qIs,
            String hS, int hI) {
        return new Metadata() {
            @Override public @Nullable <T> T get(final K<T> k) {
                if (k == PATH_VALUES) return k.cast(new Object[] { pS, pI });
                if (k == QUERY_VALUES) return k.cast(new Object[] { qS, qI, qIs });
                if (k == HEADER_VALUES) return k.cast(new Object[] { hS, hI });
                return resource_method_foo_metadata.get(k);
            }
        };
    }

    @Override
    @ResourceVisitorTest.GwtIncompatible
    public Response gwtIncompatible() {
        throw new UnsupportedOperationException("gwtIncompatible");
    }
}
