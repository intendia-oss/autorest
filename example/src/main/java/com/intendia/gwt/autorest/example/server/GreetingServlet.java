package com.intendia.gwt.autorest.example.server;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GreetingServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(GreetingServlet.class.getName());
    private static final String helloWorldJson = "[{\"greeting\":\"Hello World\"}]";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("Sending 'Hello World'...");
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode helloJsonNode = mapper.readTree(helloWorldJson);
            mapper.writeValue(resp.getOutputStream(), helloJsonNode);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "error sending 'Hello World'", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("Creating custom greeting.");
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode nameObject = mapper.readValue(req.getInputStream(), ObjectNode.class);

            final ObjectNode value = new ObjectNode(instance);
            value.put("greeting", "Hello " + nameObject.get("greeting").asText());
            mapper.writeValue(resp.getOutputStream(), new ArrayNode(instance).add(value));
        } catch (Throwable e) {
            log.log(Level.SEVERE, "error creating custom greeting", e);
        }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        log.info("Void pong response...");
    }
}
