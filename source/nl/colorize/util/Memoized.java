//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Wraps a computationally expensive operation so that the result is only
 * calculated on the first call, and the cached result is returned for
 * subsequent calls. Evaluation is lazy, the operation is not actually
 * performed until {@link #get()} is called for the first time.
 * <p>
 * This class is intended for caching single values. Use {@link Cache} for
 * caching multiple values based on a cache key.
 * <p>
 * Instances of this class are <strong>not</strong> thread-safe. This class
 * is not intended for situations where the expensive operation is performed
 * by one thread, and the result is processed by a different thread. In those
 * situations, use {@link Subject} or a similar asynchronous mechanism.
 *
 * @param <T> The type of value that is returned by the operation.
 */
public final class Memoized<T> implements Supplier<T> {

    private Supplier<T> operation;
    private AtomicBoolean called;
    private T result;

    private Memoized(Supplier<T> operation) {
        this.operation = operation;
        this.called = new AtomicBoolean(false);
    }

    @Override
    public T get() {
        if (!called.get()) {
            result = operation.get();
            called.set(true);
        }
        return result;
    }

    @Override
    public String toString() {
        if (!called.get()) {
            return "<lazy>";
        }
        return String.valueOf(result);
    }

    public static <T> Memoized<T> compute(Supplier<T> operation) {
        return new Memoized<>(operation);
    }
}
