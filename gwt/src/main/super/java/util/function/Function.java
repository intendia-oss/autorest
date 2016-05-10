package java.util.function;

@FunctionalInterface
public interface Function<T, R> {

    static <T> Function<T, T> identity() {
        return t -> t;
    }

    R apply(T t);

    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        return t -> apply(before.apply(t));
    }
}
