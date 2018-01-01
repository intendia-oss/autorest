package com.intendia.gwt.autorest.processor;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.client.ResourceVisitor;
import org.junit.Test;
import org.mockito.InOrder;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static com.google.common.primitives.Ints.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

public class ResourceVisitorTest {

    @Test
    public void visitor_works() throws Exception {
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

    @Test(expected = UnsupportedOperationException.class)
    public void gwt_incompatible_throws_exception() {
        TestService service = new TestService_RestServiceModel(() -> mock(ResourceVisitor.class));
        service.gwtIncompatible();
    }

//    @Test
//    public void givenServiceWithSingleResponse_thenShouldPassSingleJsonMapperToVisitor() throws Exception {
//        ResourceVisitor visitor = mock(ResourceVisitor.class, RETURNS_SELF);
//        when(visitor.as(Single.class, TestBean.class)).thenReturn(Single.just(any(TestBean.class)));
//
//        TestService service = new TestService_RestServiceModel(() -> visitor);
//        service.single();
//        InOrder inOrder = inOrder(visitor);
//        inOrder.verify(visitor).path();
//        inOrder.verify(visitor).produces("*/*");
//        inOrder.verify(visitor).consumes("*/*");
//        inOrder.verify(visitor).jsonMapper(any(TestBeanJsonMapper.class));
//        inOrder.verify(visitor).as(Single.class, TestBean.class);
//        inOrder.verifyNoMoreInteractions();
//    }
//
//    @Test
//    public void givenServiceWithObservableResponse_thenShouldPassObservableJsonMapperToVisitor() throws Exception {
//        ResourceVisitor visitor = mock(ResourceVisitor.class, RETURNS_SELF);
//        when(visitor.as(Observable.class, TestBean.class)).thenReturn(Observable.just(any(TestBean.class)));
//
//        TestService service = new TestService_RestServiceModel(() -> visitor);
//        service.observable();
//        InOrder inOrder = inOrder(visitor);
//        inOrder.verify(visitor).path();
//        inOrder.verify(visitor).produces("*/*");
//        inOrder.verify(visitor).consumes("*/*");
//        inOrder.verify(visitor).jsonMapper(Mockito.any(TestBeanJsonMapper.class));
//        inOrder.verify(visitor).as(Observable.class, TestBean.class);
//        inOrder.verifyNoMoreInteractions();
//    }

    @AutoRestGwt
    @Path("a")
    @Produces("*/*")
    @Consumes("*/*")
    public interface TestService {
        @Produces("application/json")
        @Consumes("application/json")
        @GET
        @Path("b/{pS}/{pI}/c")
        List<String> method(
                @PathParam("pS") String pS, @PathParam("pI") int pI,
                @QueryParam("qS") String qS, @QueryParam("qI") int qI, @QueryParam("qIs") List<Integer> qIs,
                @HeaderParam("hS") String hS, @HeaderParam("hI") int hI);

        @GwtIncompatible
        Response gwtIncompatible();
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface GwtIncompatible {
    }
}
