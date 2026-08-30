//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps an underlying mutable property, allowing subscribers to be notified
 * whenever the property's value changes.
 * <p>
 * Instances of this class are thread-safe. Multiple threads can have
 * concurrent read and/or write access to the underlying property, its value
 * can be considered a {@code volatile} field. It is also safe for multiple
 * threads to <em>subscribe</em> to property changes via {@link #getChanges()}.
 * However, do note that thread safe behavior also requires instances of
 * {@code T} to be immutable or thread safe.
 *
 * @param <T> The underlying property's type.
 */
public final class Signal<T> implements Supplier<T> {

    private AtomicReference<T> valueRef;
    @Getter Subject<T> changes;

    private Signal() {
        this.valueRef = new AtomicReference<>(null);
        this.changes = new Subject<>();
    }

    public void set(T newValue) {
        if (!Objects.equals(valueRef.get(), newValue)) {
            valueRef.set(newValue);
            changes.next(newValue);
        }
    }

    @Override
    public T get() {
        return valueRef.get();
    }

    public void update(Function<T, T> callback) {
        T oldValue = valueRef.get();
        T newValue = callback.apply(oldValue);
        set(newValue);
    }

    @Override
    public String toString() {
        return String.valueOf(valueRef.get());
    }

    /**
     * Creates a new {@link Signal} with the specified initial value.
     * Subscribers are not notified of the initial value.
     */
    public static <T> Signal<T> of(T initialValue) {
        Signal<T> signal = new Signal<>();
        signal.valueRef.set(initialValue);
        return signal;
    }

    /**
     * Creates a new {@link Signal} with the specified initial value, then
     * immediately notifies subscribers of this initial value.
     */
    public static <T> Signal<T> emit(T initialValue) {
        Signal<T> signal = new Signal<>();
        signal.valueRef.set(initialValue);
        signal.changes.next(initialValue);
        return signal;
    }
}
