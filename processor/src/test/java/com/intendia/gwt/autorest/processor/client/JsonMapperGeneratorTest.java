package com.intendia.gwt.autorest.processor.client;

import static com.intendia.gwt.autorest.client.JacksonRequestResourceBuilder.iterableDeserializerOf;

import com.google.gwt.junit.client.GWTTestCase;
import com.intendia.gwt.autorest.client.MapperRegistry;
import com.intendia.gwt.autorest.processor.client.MyMapperRegistry.TestBeanMapper;
import com.intendia.gwt.autorest.processor.client.TestService.TestBean;
import com.intendia.gwt.autorest.processor.client.TestService.TestInterface;
import java.util.Iterator;
import rx.functions.Func1;

public class JsonMapperGeneratorTest extends GWTTestCase {
    @Override public String getModuleName() { return "com.intendia.gwt.autorest.processor.JsonMapperProcessorTest"; }

    public void testMapperWriteObject() {
        TestBean testBean = new TestBean(); testBean.message = "hello write";
        assertEquals("{\"message\":\"hello write\"}", TestBeanMapper.INSTANCE.write(testBean));
    }

    public void testMapperReadObject() {
        TestBean testBean = new TestBean(); testBean.message = "hello read";
        assertEquals(testBean, TestBeanMapper.INSTANCE.read("{\"message\":\"hello read\"}"));
    }

    public void testMapperReadAsArrayObject() {
        Func1<String, Iterable<TestBean>> itd = iterableDeserializerOf(TestBeanMapper.INSTANCE.getDeserializer());
        Iterator<TestBean> it = itd.call("[{\"message\":\"a\"},{\"message\":\"b\"}]").iterator();
        assertEquals("a", it.next().message);
        assertEquals("b", it.next().message);
    }

    public void testClassAnnotatedWithJsType_thenMapperShouldBeJsMapper() {
        assertNull(new MapperRegistry().get(TestService.JsTypeBean.class));
    }

    public void testInterface_thenMapperShouldBeJsMapper() {
        assertNull(new MapperRegistry().get(TestInterface.class));
    }
}
