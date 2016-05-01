package com.intendia.gwt.autorest.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.junit.Test;
import rx.Observable;

public class ResourceBuilderTest {

    @Test public void params_works() throws Exception {
        TestResourceBuilder builder = new TestResourceBuilder();
        builder.service.paramsWorks("s", 1, Arrays.asList(1, 2, 3));
        assertThat(builder.uri(), equalTo("./test?basic=s&primitiveInt=1&listOfInt=1&listOfInt=2&listOfInt=3"));
    }

    @Test public void paths_works() throws Exception {
        TestResourceBuilder builder = new TestResourceBuilder();
        builder.service.pathsWorks("s", 1);
        assertThat(builder.uri(), equalTo("./test/s/middle/1"));
    }

    @Test(expected = UnsupportedOperationException.class) public void incompatible_gwt_throws_exception() {
        new TestResourceBuilder().service.gwtIncompatible();
    }

    @Test(expected = UnsupportedOperationException.class) public void incompatible_guava_throws_exception() {
        new TestResourceBuilder().service.guavaIncompatible();
    }

    private static class TestResourceBuilder extends CollectorResourceBuilder {
        final TestService service = new TestService_RestServiceProxy(this);
        @Override @SuppressWarnings("unchecked") public <T> T build(Class<? super T> type) {
            return ResourceBuilder.class.equals(type) ? (T) this : null;
        }
    }

    @AutoRestGwt @Path("test") public interface TestService {

        @GET Observable<Void> paramsWorks(
                @QueryParam("basic") String basic,
                @QueryParam("primitiveInt") int primitiveInt,
                @QueryParam("listOfInt") List<Integer> listOfInt);

        @GET @Path("{basic}/middle/{primitiveInt}") Observable<Void> pathsWorks(
                @PathParam("basic") String basic,
                @PathParam("primitiveInt") int primitiveInt);

        @com.google.gwt.core.shared.GwtIncompatible Response gwtIncompatible();
        @com.google.common.annotations.GwtIncompatible("serverOnly") Response guavaIncompatible();
    }
}
