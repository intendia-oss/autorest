package com.intendia.gwt.autorest.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ResourceBuilderTest {
    @Test public void factory_works() {
        ResourceFactory ch0 = ResourceFactory.create(() -> new CollectorResourceBuilder().path("http://base"));
        ResourceBuilder rb0 = ch0.collect();
        assertThat(rb0.uri(), equalTo("http://base"));

        ResourceFactory ch1 = ch0.lift(rb -> rb.path("path1").param("p1", "v1"));
        ResourceBuilder rb1 = ch1.collect();
        assertThat(rb1.uri(), equalTo("http://base/path1?p1=v1"));

        ResourceFactory ch2 = ch1.lift(rb -> rb.path("path2").param("p2", "v2"));
        ResourceBuilder rb2 = ch2.collect();
        assertThat(rb2.uri(), equalTo("http://base/path1/path2?p1=v1&p2=v2"));
    }
}
