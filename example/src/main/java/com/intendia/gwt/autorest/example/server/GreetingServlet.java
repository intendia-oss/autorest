package com.intendia.gwt.autorest.example.server;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// @WebServlet(name = "greeting-service", urlPatterns = "/example/api/*")
public class GreetingServlet extends HttpServlet {
    private static final Logger L = Logger.getLogger(GreetingServlet.class.getName());
    private static final String helloWorldJson = "[{\"greeting\":\"Hello World\"}]";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String uri = req.getRequestURI();
        L.info("Sending 'Hello World' in response of " + uri);
        try {
            String FOO_URI = "/example/api/observable/foo";
            if (uri.equals(FOO_URI)) {
                resp.getWriter().write("[{\"greeting\":\"/foo\"}]");
            } else if (uri.startsWith(FOO_URI)) {
                String x = uri.substring(FOO_URI.length()) + "?" + req.getQueryString();
                resp.getWriter().write("[{\"greeting\":\"/foo" + x + "\"}]");
            } else {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode helloJsonNode = mapper.readTree(helloWorldJson);
                mapper.writeValue(resp.getOutputStream(), helloJsonNode);
            }
        } catch (Throwable e) {
            L.log(Level.SEVERE, "error sending 'Hello World'", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String uri = req.getRequestURI();
        L.info("Creating custom greeting in response of " + uri);
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode nameObject = mapper.readValue(req.getInputStream(), ObjectNode.class);

            final ObjectNode value = new ObjectNode(instance);
            value.put("greeting", "Hello " + nameObject.get("greeting").asText());
            mapper.writeValue(resp.getOutputStream(), new ArrayNode(instance).add(value));
        } catch (Throwable e) {
            L.log(Level.SEVERE, "error creating custom greeting", e);
        }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        L.info("Void pong response...");
    }
}
