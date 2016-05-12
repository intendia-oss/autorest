package com.intendia.gwt.autorest.client;

import static com.google.common.primitives.Ints.asList;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.InOrder;

public class ResourceVisitorTest {

    @Test public void visitor_works() throws Exception {
        ResourceVisitor visitor = mock(ResourceVisitor.class, RETURNS_SELF);
        TestService service = new TestService_RestServiceModel(() -> visitor);
        service.method("s", 1, "s", 1, asList(1, 2, 3));
        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor).path("a");
        inOrder.verify(visitor).path("b", "s", 1, "c");
        inOrder.verify(visitor).param("s", "s");
        inOrder.verify(visitor).param("i", 1);
        inOrder.verify(visitor).param("is", asList(1, 2, 3));
        inOrder.verify(visitor).as(Object.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = UnsupportedOperationException.class) public void gwt_incompatible_throws_exception() {
        TestService service = new TestService_RestServiceModel(() -> mock(ResourceVisitor.class));
        service.gwtIncompatible();
    }

    @AutoRestGwt @Path("a") public interface TestService {
        @GET @Path("b/{s}/{i}/c") Object method(
                @PathParam("s") String sPath,
                @PathParam("i") int iPath,
                @QueryParam("s") String sQuery,
                @QueryParam("i") int iQuery,
                @QueryParam("is") List<Integer> is);

        @GwtIncompatible Response gwtIncompatible();
    }

    @Retention(RetentionPolicy.CLASS) @Target(ElementType.METHOD)
    public @interface GwtIncompatible {}
}
