package com.intendia.gwt.autorest.processor.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.intendia.gwt.autorest.client.mapper.JsJsonMapper;
import com.intendia.gwt.autorest.client.mapper.JsonMapper;
import com.intendia.gwt.autorest.client.RequestResourceBuilder;
import com.intendia.gwt.autorest.processor.client.test.TestBean;
import rx.Single;

public class JsonMapperGeneratorTest extends GWTTestCase {

    private final RequestResourceBuilderSpy requestResourceBuilderSpy = new RequestResourceBuilderSpy();
    private TestBeanJsonMapper mapper;

    @Override
    public String getModuleName() {
        return "com.intendia.gwt.autorest.processor.JsonMapperProcessorTest";
    }

    @Override
    protected void gwtSetUp() throws Exception {
//        new MappersConfigurator_Generated().configure();
        mapper = new TestBeanJsonMapper();
    }

    public void testMapperWriteObject() {
        TestBean testBean = new TestBean();
        testBean.setMessage("hello");
        String json = mapper.write(testBean);

        assertEquals(json, "{\"message\":\"hello\"}");
    }

    public void testMapperReadObject() {
        TestBean testBean = new TestBean();
        testBean.setMessage("hello");
        String json = mapper.write(testBean);

        TestBean result = mapper.read(json);

        assertEquals(testBean, result);
    }

    public void testMapperReadAsArrayObject() {
        TestBean[] result = mapper.readAsArray("[{\"message\":\"hello\"},{\"message\":\"hello from other bean\"}]");

        assertEquals(result[0].getMessage(), "hello");
        assertEquals(result[1].getMessage(), "hello from other bean");
    }

    public void testClassAnnotatedWithJsType_thenMapperShouldBeJsMapper() {
        TestService testService = new TestService_RestServiceModel(() -> requestResourceBuilderSpy);
        testService.getJsTypeBean();

        assertEquals(requestResourceBuilderSpy.mapper.getClass(), JsJsonMapper.class);
    }

    public void testInterface_thenMapperShouldBeJsMapper() {
        TestService testService = new TestService_RestServiceModel(() -> requestResourceBuilderSpy);
        testService.getTestInterface();

        assertEquals(requestResourceBuilderSpy.mapper.getClass(), JsJsonMapper.class);
    }

    private class RequestResourceBuilderSpy extends RequestResourceBuilder {

        private JsonMapper mapper;

        @Override
        public <T> T as(Class<? super T> container, Class<?> type) {
            mapper = getMapper(type);
            return (T) Single.just(new TestBean());
        }
    }
}
