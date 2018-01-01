package com.intendia.gwt.autorest.example.shared;

public class JacksonGreeting {
    private String greeting;

    public JacksonGreeting() {
    }

    public JacksonGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    @Override
    public String toString() {
        return "JacksonGreeting{" +
                "greeting='" + greeting + '\'' +
                '}';
    }
}
