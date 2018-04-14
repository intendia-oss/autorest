package com.intendia.gwt.autorest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.sun.net.httpserver.HttpServer;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JreResourceBuilderTest {

    private static HttpServer httpServer;

    @BeforeClass public static void prepareServer() throws Exception {
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
    @AfterClass public static void closeServer() {
        httpServer.stop(0);
    }

    private static String baseUrl;
    private static TestRestService_RestServiceModel rest;

    @Before public void prepareClient() {
        baseUrl = "http://localhost:" + httpServer.getAddress().getPort() + "/";
        rest = new TestRestService_RestServiceModel(() -> new JreResourceBuilder(baseUrl));
    }

    @Test public void zero() {
        assertNull(rest.zero().blockingGet());
    }

    @Test public void one() {
        assertEquals("expected", rest.one().blockingGet().bar);
    }

    @Test public void many() {
        assertEquals(2, rest.many().toList().blockingGet().size());
    }

    @AutoRestGwt @Path("api")
    public interface TestRestService {
        @GET @Path("zero") Completable zero();
        @GET @Path("one") Single<Foo> one();
        @GET @Path("many") Observable<Foo> many();
    }

    public static class Foo {
        public String bar;
    }
}
