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
     * Creates a new {@link Signal} with the specified initial value. Note
     * that subscribers will <em>also</em> be notified of this initial value.
     */
    public static <T> Signal<T> of(T initialValue) {
        Signal<T> signal = new Signal<>();
        signal.set(initialValue);
        return signal;
    }
}
