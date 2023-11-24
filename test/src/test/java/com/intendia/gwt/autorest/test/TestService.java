package com.intendia.gwt.autorest.test;

import com.intendia.gwt.autorest.client.AutoRestGwt;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@AutoRestGwt @Path("api") @Produces("*/*") @Consumes("*/*")
public interface TestService {
    @Produces("application/json") @Consumes("application/json")
    @GET @Path("foo") List<String> foo(@QueryParam("bar") String bar);
}