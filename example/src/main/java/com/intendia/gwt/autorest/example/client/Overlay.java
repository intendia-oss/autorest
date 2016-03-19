package com.intendia.gwt.autorest.example.client;

import com.google.gwt.core.client.JavaScriptObject;

public class Overlay extends JavaScriptObject implements Interface {

    protected Overlay() {
    }

    public final native String getGreeting() /*-{
        return this.greeting;
    }-*/;

    public final native void setGreeting(String greeting) /*-{
        this.greeting = greeting;
    }-*/;
}
