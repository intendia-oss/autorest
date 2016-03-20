package com.intendia.gwt.autorest.example.client;

import com.google.gwt.core.client.JavaScriptObject;

public class OverlayGreeting extends JavaScriptObject implements Greeting {

    protected OverlayGreeting() {
    }

    public final native String getGreeting() /*-{
        return this.greeting;
    }-*/;

    public final native void setGreeting(String greeting) /*-{
        this.greeting = greeting;
    }-*/;
}
