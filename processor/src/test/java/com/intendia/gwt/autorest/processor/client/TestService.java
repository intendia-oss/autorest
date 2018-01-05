package com.intendia.gwt.autorest.processor.client;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import jsinterop.annotations.JsType;
import rx.Single;

@AutoRestGwt
@Path("test")
public interface TestService {

    @GET Single<TestBean> getBean();

    @GET Single<JsTypeBean> getJsTypeBean();

    @GET Single<TestInterface> getTestInterface();

    class TestBean {
        public String message;

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestBean testBean = (TestBean) o;
            return message != null ? message.equals(testBean.message) : testBean.message == null;
        }

        @Override public int hashCode() {
            return message != null ? message.hashCode() : 0;
        }
    }

    @JsType class JsTypeBean {}

    interface TestInterface {}
}
