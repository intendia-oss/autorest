package com.intendia.gwt.autorest.client;

import java.util.function.Function;
import java.util.function.Supplier;

public class ResourceFactory {
    private final Supplier<ResourceBuilder> producer;

    private ResourceFactory(Supplier<ResourceBuilder> producer) {
        this.producer = producer;
    }

    public ResourceFactory lift(Function<ResourceBuilder, ResourceBuilder> operator) {
        return create(() -> operator.apply(producer.get()));
    }

    public ResourceBuilder collect() {
        return producer.get();
    }

    public static ResourceFactory create(Supplier<ResourceBuilder> producer) {
        return new ResourceFactory(producer);
    }
}
