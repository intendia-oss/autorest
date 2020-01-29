package com.intendia.gwt.autorest.client;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ResourceVisitorTest {

    @Test public void collector_visitor_works() {
        // check root path is accepted (this is a bit obscure, currently anything starting with ".*?//" is a root path)
        MyCollectorResourceVisitor rb0 = new MyCollectorResourceVisitor("http://base/");
        assertThat(rb0.uri(), equalTo("http://base"));
        // basic path an param check
        MyCollectorResourceVisitor rb1 = (MyCollectorResourceVisitor) new MyCollectorResourceVisitor("http://base")
                .path("path").param("p1", "v1");
        assertThat(rb1.uri(), equalTo("http://base/path?p1=v1"));
        // check nested path and params combine correctly
        MyCollectorResourceVisitor rb2 = (MyCollectorResourceVisitor) new MyCollectorResourceVisitor("http://base/")
                .path("path").param("p1", "v1").path("nest").param("p2", asList("v2a", "v2b"));
        assertThat(rb2.uri(), equalTo("http://base/path/nest?p1=v1&p2=v2a&p2=v2b"));
        // check paths (except root) and params are escaped
        MyCollectorResourceVisitor rb3 = (MyCollectorResourceVisitor) new MyCollectorResourceVisitor("http://base:/")
                .path("path:").param("p:", "v:");
        assertThat(rb3.uri(), equalTo("http://base:/path%3A?p%3A=v%3A"));
    }

    private static class MyCollectorResourceVisitor extends CollectorResourceVisitor {
        private MyCollectorResourceVisitor(String base) {super(base);}
        @Override protected String encodeComponent(String str) { return str.replaceAll(":", "%3A"); }
        @Override public <T> T as(Class<? super T> container, Class<?> type) { return null; }
    }
}
