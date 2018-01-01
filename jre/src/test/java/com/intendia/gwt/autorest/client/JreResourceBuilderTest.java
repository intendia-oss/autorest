package com.intendia.gwt.autorest.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Completable;
import rx.Observable;
import rx.Single;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JreResourceBuilderTest {

    private static HttpServer httpServer;

    @BeforeClass
    public static void prepareServer() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/api/zero", httpExchange -> {
            httpExchange.sendResponseHeaders(200, 0);
            try (OutputStream responseBody = httpExchange.getResponseBody()) {
                responseBody.write("".getBytes());
            }
        });
        httpServer.createContext("/api/one", httpExchange -> {
            httpExchange.sendResponseHeaders(200, 0);
            try (OutputStream responseBody = httpExchange.getResponseBody()) {
                responseBody.write("{\"bar\":\"expected\"}".getBytes());
            }
        });
        httpServer.createContext("/api/many", httpExchange -> {
            httpExchange.sendResponseHeaders(200, 0);
            try (OutputStream responseBody = httpExchange.getResponseBody()) {
                responseBody.write("[{\"bar\":\"expected1\"},{\"bar\":\"expected2\"}]".getBytes());
            }
        });
        httpServer.start();
    }

    @AfterClass
    public static void closeServer() throws Exception {
        httpServer.stop(0);
    }

    private static String baseUrl;
    private static FooService_RestServiceModel rest;

    @Before
    public void prepareClient() throws Exception {
        baseUrl = "http://localhost:" + httpServer.getAddress().getPort() + "/";
        rest = new FooService_RestServiceModel(() -> new JreResourceBuilder(baseUrl));
    }

    @Test
    public void zero() throws Exception {
        assertNull(rest.zero().get());
    }

    @Test
    public void one() throws Exception {
        assertEquals("expected", rest.one().toBlocking().value().bar);
    }

    @Test
    public void many() throws Exception {
        assertEquals(2, rest.many().toList().toSingle().toBlocking().value().size());
    }

    @AutoRestGwt
    @Path("api")
    public interface FooService {
        @GET
        @Path("zero")
        Completable zero();

        @GET
        @Path("one")
        Single<Foo> one();

        @GET
        @Path("many")
        Observable<Foo> many();
    }

}
