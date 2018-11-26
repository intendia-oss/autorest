package com.intendia.gwt.autorest.client;

public class JreAutorestProxyResourceBuilderTest extends JreResourceBuilderTest {
	@Override
	protected TestRestService createRestService(ResourceVisitor.Supplier path) {
        return AutorestProxy.create(
        	Thread.currentThread().getContextClassLoader(), 
        	TestRestService.class,
        	path);
    }
}
