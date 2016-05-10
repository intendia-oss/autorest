package com.intendia.gwt.autorest.example.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.intendia.gwt.autorest.client.RequestResourceBuilder;
import com.intendia.gwt.autorest.client.ResourceBuilder;
import rx.functions.Action1;

public class ExampleEntryPoint implements EntryPoint {
    private Action1<Throwable> err = e -> GWT.log("exception: " + e, e);

    public void onModuleLoad() {
        final ObservableService oService = ObservableService.Factory.create(this::getApi);
        final SingleService sService = SingleService.Factory.create(this::getApi);

        append("Name:");
        final TextBox nameInput = new TextBox();
        RootPanel.get().add(nameInput);
        nameInput.addValueChangeHandler(e -> getCustomGreeting(oService, nameInput.getValue()));
        nameInput.setValue("ping", true);

        oService.ping().subscribe(n -> append("observable pong"), err);
        sService.ping().subscribe(n -> append("single pong"), err);

        oService.getFoo().subscribe(n -> append("observable.foo response: " + n.getGreeting()), err);
        oService.getFoo("FOO", "BAR", null).subscribe(n -> append("observable.foo response: " + n.getGreeting()), err);
    }

    private ResourceBuilder getApi() { return new RequestResourceBuilder().path(GWT.getModuleBaseURL(), "api"); }

    private void getCustomGreeting(ObservableService service, String name) {
        Greeting greeting = JavaScriptObject.createObject().cast();
        greeting.setGreeting(name);
        service.post(greeting).subscribe(r -> append("server said " + r.getGreeting()), err);
    }

    private static void append(String text) {
        RootPanel.get().add(new Label(text));
    }
}
