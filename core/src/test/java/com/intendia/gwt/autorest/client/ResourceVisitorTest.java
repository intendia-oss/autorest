package com.intendia.gwt.autorest.client;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.awt.List;

import org.junit.Test;

public class ResourceVisitorTest {

    @Test public void collector_visitor_works() {
        final ResourceVisitor.Supplier ch0 = new ResourceVisitor.Supplier() {
            public ResourceVisitor get() { return new MyCollectorResourceVisitor().path("http://base"); }
        };
        MyCollectorResourceVisitor rb0 = (MyCollectorResourceVisitor) ch0.get();
        assertThat(rb0.uri(), equalTo("http://base"));

        final ResourceVisitor.Supplier ch1 = new ResourceVisitor.Supplier() {
            public ResourceVisitor get() { return ch0.get().path("path1").param("p1", "v1", Type.of(String.class)); }
        };
        MyCollectorResourceVisitor rb1 = (MyCollectorResourceVisitor) ch1.get();
        assertThat(rb1.uri(), equalTo("http://base/path1?p1=v1"));

        ResourceVisitor.Supplier ch2 = new ResourceVisitor.Supplier() {
            public ResourceVisitor get() { return ch1.get().path("path2").param("p2", asList("v2a", "v2b"), Type.of(List.class).typeParam(Type.of(String.class))); }
        };
        MyCollectorResourceVisitor rb2 = (MyCollectorResourceVisitor) ch2.get();
        assertThat(rb2.uri(), equalTo("http://base/path1/path2?p1=v1&p2=v2a&p2=v2b"));
    }

    private static class MyCollectorResourceVisitor extends CollectorResourceVisitor {
        @Override protected String encodeComponent(String str) { return str; }
        @Override public <T> T as(Type type) { return null; }
    }
}
