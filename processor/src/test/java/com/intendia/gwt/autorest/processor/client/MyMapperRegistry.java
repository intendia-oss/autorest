package com.intendia.gwt.autorest.processor.client;

import com.intendia.gwt.autorest.client.MapperRegistry;
import com.progressoft.brix.domino.gwtjackson.ObjectMapper;
import com.progressoft.brix.domino.gwtjackson.annotation.JSONMapper;

public class MyMapperRegistry extends MapperRegistry {
    public MyMapperRegistry() {
        register(TestService.TestBean.class.getCanonicalName(), TestBeanMapper.INSTANCE);
    }

    @JSONMapper
    public interface TestBeanMapper extends ObjectMapper<TestService.TestBean> {
        TestBeanMapper INSTANCE = new MyMapperRegistry_TestBeanMapperImpl();
    }
}
