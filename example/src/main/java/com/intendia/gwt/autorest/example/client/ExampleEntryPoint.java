package com.intendia.gwt.autorest.example.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.intendia.gwt.autorest.client.RequestResourceBuilder;
import com.intendia.gwt.autorest.client.ResourceVisitor;
import com.intendia.gwt.autorest.example.client.ExampleService.Greeting;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public class ExampleEntryPoint implements EntryPoint {
    private Consumer<Throwable> err = e -> GWT.log("exception: " + e, e);

    public void onModuleLoad() {
        TextBox name = append(new TextBox());
        HTML out = append(new HTML());

        ResourceVisitor.Supplier getApi = () -> new RequestResourceBuilder().path(GWT.getModuleBaseURL(), "api");
        ExampleService srv = new ExampleService_RestServiceModel(() -> getApi.get().header("auth", "ok", Type.undefined()));

        Observable.merge(valueChange(name), keyUp(name)).map(e -> name.getValue())
                .switchMap(q -> {
                    Greeting greeting = new Greeting();
                    greeting.greeting = q;
                    return srv.post(greeting)
                            .map(o -> o.greeting)
                            .onErrorReturn(Throwable::toString);
                })
                .forEach(out::setHTML);
        name.setValue("ping", true);

        append("-- Static tests --");
        srv.pingObservable().ignoreElements().subscribe(() -> append("observable pong"), err);
        srv.pingMaybe().ignoreElement().subscribe(() -> append("maybe pong"), err);
        srv.pingCompletable().subscribe(() -> append("completable pong"), err);

        srv.getFoo().subscribe(n -> append("observable.foo response: " + n.greeting), err);
        srv.getFoo("FOO", "BAR", null).subscribe(n -> append("observable.foo response: " + n.greeting), err);
    }

    private static void append(String text) { append(new Label(text)); }

    private static <T extends IsWidget> T append(T w) { RootPanel.get().add(w); return w; }

    private static Observable<KeyUpEvent> keyUp(HasKeyUpHandlers source) {
        return Observable.create(s -> register(s, source.addKeyUpHandler(s::onNext)));
    }

    public static <T> Observable<ValueChangeEvent<T>> valueChange(HasValueChangeHandlers<T> source) {
        return Observable.create(s -> register(s, source.addValueChangeHandler(s::onNext)));
    }

    private static void register(ObservableEmitter<?> s, HandlerRegistration handlerRegistration) {
        s.setCancellable(handlerRegistration::removeHandler);
    }
}
