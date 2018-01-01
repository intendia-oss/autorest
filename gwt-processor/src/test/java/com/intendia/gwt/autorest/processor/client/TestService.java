package com.intendia.gwt.autorest.processor.client;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import com.intendia.gwt.autorest.processor.client.test.TestBean;
import rx.Single;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@AutoRestGwt
@Path("test")
public interface TestService {

    @GET
    Single<TestBean> getBean();

    @GET
    Single<JsTypeBean> getJsTypeBean();

    @GET
    Single<TestInterface> getTestInterface();
}
