//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import lombok.Getter;

import java.util.function.Supplier;

/**
 * Wraps an underlying mutable property, allowing subscribers to be notified
 * whenever the property's value changes.
 * <p>
 * Instances of this class are <strong>not</strong> thread-safe. This class is
 * not intended for situations where different threads need to update the value
 * of the same underlying property. However, it is safe for multiple threads to
 * <em>subscribe</em> to property changes via {@link #getChanges()}.
 *
 * @param <T> The underlying property's type.
 */
public final class Signal<T> implements Supplier<T> {

    private T value;
    @Getter Subject<T> changes;

    private Signal() {
        this.changes = new Subject<>();
    }

    public void set(T newValue) {
        value = newValue;
        changes.next(newValue);
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Creates a new {@link Signal} with the specified initial value.
     * Subscribers are not notified of the initial value.
     */
    public static <T> Signal<T> of(T initialValue) {
        Signal<T> signal = new Signal<>();
        signal.value = initialValue;
        return signal;
    }

    /**
     * Creates a new {@link Signal} with the specified initial value, then
     * immediately notifies subscribers of this initial value.
     */
    public static <T> Signal<T> emit(T initialValue) {
        Signal<T> signal = new Signal<>();
        signal.set(initialValue);
        return signal;
    }
}
