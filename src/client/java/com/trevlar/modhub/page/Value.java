package com.trevlar.modhub.page;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Something a control edits: a Mod Hub setting, a YACL option, a Cloth entry, a MidnightLib field.
 * Whoever builds the page decides what get and set reach; the screen only ever calls these.
 *
 * <p><b>Set applies and saves at once.</b> There is no Save button anywhere in Mod Hub, so a value
 * from a library that batches its saves (YACL) applies the change and runs the library's save on
 * every set.
 */
public interface Value<T> {

    T get();

    void set(T value);

    T defaultValue();

    /** False greys the control out (YACL's "available"). */
    default boolean available() { return true; }

    default void reset() { set(defaultValue()); }

    /** A value from plain functions, the usual way to build one. */
    static <T> Value<T> of(Supplier<T> get, Consumer<T> set, T defaultValue) {
        return of(get, set, defaultValue, () -> true);
    }

    static <T> Value<T> of(Supplier<T> get, Consumer<T> set, T defaultValue, BooleanSupplier available) {
        return new Value<>() {
            @Override public T get() { return get.get(); }
            @Override public void set(T value) { set.accept(value); }
            @Override public T defaultValue() { return defaultValue; }
            @Override public boolean available() { return available.getAsBoolean(); }
        };
    }
}
