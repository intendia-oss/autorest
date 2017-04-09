package com.intendia.gwt.autorest.client;

import static com.google.common.primitives.Ints.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.InOrder;

public class ResourceVisitorTest {

    @Test public void visitor_works() throws Exception {
        ResourceVisitor visitor = mock(ResourceVisitor.class, RETURNS_SELF);
        when(visitor.as(List.class, String.class)).thenReturn(singletonList("done"));
        TestService service = new TestService_RestServiceModel(() -> visitor);
        service.method("s", 1, "s", 1, asList(1, 2, 3), "s", 1);
        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor).path("a");
        inOrder.verify(visitor).path("b", "s", 1, "c");
        inOrder.verify(visitor).produces("application/json");
        inOrder.verify(visitor).consumes("application/json");
        inOrder.verify(visitor).param("qS", "s");
        inOrder.verify(visitor).param("qI", 1);
        inOrder.verify(visitor).param("qIs", asList(1, 2, 3));
        inOrder.verify(visitor).header("hS", "s");
        inOrder.verify(visitor).header("hI", 1);
        inOrder.verify(visitor).as(List.class, String.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = UnsupportedOperationException.class) public void gwt_incompatible_throws_exception() {
        TestService service = new TestService_RestServiceModel(() -> mock(ResourceVisitor.class));
        service.gwtIncompatible();
    }

    @AutoRestGwt @Path("a") @Produces("*/*") @Consumes("*/*")
    public interface TestService {
        @Produces("application/json") @Consumes("application/json")
        @GET @Path("b/{pS}/{pI}/c") List<String> method(
                @PathParam("pS") String pS, @PathParam("pI") int pI,
                @QueryParam("qS") String qS, @QueryParam("qI") int qI, @QueryParam("qIs") List<Integer> qIs,
                @HeaderParam("hS") String hS, @HeaderParam("hI") int hI);

        @GwtIncompatible Response gwtIncompatible();
    }

    @Retention(RetentionPolicy.CLASS) @Target(ElementType.METHOD)
    public @interface GwtIncompatible {}
}
