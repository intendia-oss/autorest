package com.intendia.gwt.autorest.example.client;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.intendia.gwt.autorest.client.RequestResourceBuilder;
import com.intendia.gwt.autorest.client.ResourceVisitor;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Consumer;

public class ExampleEntryPoint implements EntryPoint {
    private Consumer<Throwable> err = e -> GWT.log("exception: " + e, e);

    public void onModuleLoad() {
        TextBox name = append(new TextBox());
        FlowPanel status = append(new FlowPanel());
        HTML out = append(new HTML());

        ResourceVisitor.Supplier getApi = () -> new RequestResourceBuilder()
                .dispatcher(rb -> {
                    Label info = new Label(rb.getUrl() + " (PENDING)"); status.add(info);
                    Runnable hide = () -> Observable.timer(1, SECONDS).subscribe(n -> status.remove(info));
                    RequestCallback inner = rb.getCallback();
                    rb.setCallback(new RequestCallback() {
                        @Override public void onResponseReceived(Request request, Response response) {
                            info.setText(rb.getUrl() + " (RECEIVED)"); hide.run();
                            inner.onResponseReceived(request, response);
                        }
                        @Override public void onError(Request request, Throwable exception) {
                            info.setText(rb.getUrl() + "(ERROR): " + exception); hide.run();
                            inner.onError(request, exception);
                        }
                    });
                    try { return rb.send(); } catch (RequestException e) { throw new RuntimeException(e); }
                })
                .path(GWT.getModuleBaseURL(), "api");
        ObservableService oService = new ObservableService_RestServiceModel(
                () -> getApi.get().header("mode", "observable"));
        SingleService sService = new SingleService_RestServiceModel(() -> getApi.get().header("mode", "single"));

        Observable.merge(valueChange(name), keyUp(name)).map(e -> name.getValue())
                .switchMap(q -> {
                    Greeting greeting = JavaScriptObject.createObject().cast();
                    greeting.setGreeting(q);
                    return oService.post(greeting)
                            .map(Greeting::getGreeting)
                            .onErrorReturn(Throwable::toString);
                })
                .forEach(out::setHTML);
        name.setValue("ping", true);

        append("-- Static tests --");
        oService.ping().subscribe(n -> append("observable pong"), err);
        sService.ping().subscribe(n -> append("single pong"), err);

        oService.getFoo().subscribe(n -> append("observable.foo response: " + n.getGreeting()), err);
        oService.getFoo("FOO", "BAR", null).subscribe(n -> append("observable.foo response: " + n.getGreeting()), err);
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
