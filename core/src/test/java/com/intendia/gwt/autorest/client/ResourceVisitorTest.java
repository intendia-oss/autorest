package com.intendia.gwt.autorest.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.function.Supplier;
import org.junit.Test;

public class ResourceVisitorTest {
    @Test public void factory_works() {
        Supplier<ResourceVisitor> ch0 = () -> new CollectorResourceVisitor().path("http://base");
        ResourceVisitor rb0 = ch0.get();
        assertThat(rb0.uri(), equalTo("http://base"));

        Supplier<ResourceVisitor> ch1 = () -> ch0.get().path("path1").param("p1", "v1");
        ResourceVisitor rb1 = ch1.get();
        assertThat(rb1.uri(), equalTo("http://base/path1?p1=v1"));

        Supplier<ResourceVisitor> ch2 = () -> ch1.get().path("path2").param("p2", "v2");
        ResourceVisitor rb2 = ch2.get();
        assertThat(rb2.uri(), equalTo("http://base/path1/path2?p1=v1&p2=v2"));
    }
}
