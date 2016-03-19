package com.intendia.gwt.autorest.example.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.intendia.gwt.autorest.client.Resource;
import rx.Subscriber;
import rx.functions.Action1;

public class ExampleEntryPoint implements EntryPoint {
    Action1<Throwable> err = e -> GWT.log("exception: " + e, e);

    public void onModuleLoad() {
        Resource root = new Resource(GWT.getModuleBaseURL());

        final ObservableService oService = ObservableService.Factory.create(root);
        final SingleService sService = SingleService.Factory.create(root);

        RootPanel.get().add(new Label("Name:"));
        final TextBox nameInput = new TextBox();
        RootPanel.get().add(nameInput);
        nameInput.addValueChangeHandler(e -> getCustomGreeting(oService, nameInput.getValue()));
        nameInput.setValue("ping", true);

        oService.ping().subscribe(n -> RootPanel.get().add(new Label("observable pong")), err);
        sService.ping().subscribe(n -> RootPanel.get().add(new Label("single pong")), err);
    }

    private void getCustomGreeting(ObservableService service, String name) {
        Overlay overlay = (Overlay) JavaScriptObject.createObject();
        overlay.setGreeting(name);
        service.overlay(overlay).subscribe(subscriber("overlays", new AbstractRenderer<Overlay>() {
            public String render(Overlay object) {
                return object.getGreeting();
            }
        }));

        Interface iface = (Overlay) JavaScriptObject.createObject();
        iface.setGreeting(name);
        service.iface(iface).subscribe(subscriber("iface", new AbstractRenderer<Interface>() {
            public String render(Interface object) { return object.getGreeting(); }
        }));
    }

    private <T> Subscriber<T> subscriber(final String container, final Renderer<T> render) {
        return new Subscriber<T>() {
            @Override
            public void onCompleted() { }

            @Override
            public void onError(Throwable e) { err.call(e); }

            @Override
            public void onNext(T t) {
                RootPanel.get().add(new Label("server said using " + container + ": " + render.render(t)));
            }
        };
    }
}
