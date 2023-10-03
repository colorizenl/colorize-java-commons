//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.Optional;

/**
 * Proxy for accessing a value that is provided by a (possibily asynchronous)
 * operation in the future, but is not yet known when the {@link Promise} is
 * created. The {@link Promise} subscribes to operation and can be used to
 * access its eventual success or failure.
 * <p>
 * This class has a similar purpose as {@code Future}, but is also available on
 * platforms where {@code java.util.concurrent} is not supported, such as TeaVM.
 * When used on a platform that <em>does</em> support concurrency, this class is
 * thread-safe and can be shared between multiple threads.
 *
 * @param <T> The type of value that is produced by the operation.
 */
public final class Promise<T> {

    private Subscribable<T> subject;
    private T value;
    private Exception error;
    private final Object lock;

    private Promise(Subscribable<T> subject) {
        this.subject = subject;
        this.lock = new Object();
    }

    public Subscribable<T> getSubject() {
        return subject;
    }

    private void fulfill(T value) {
        synchronized (lock) {
            if (!isCompleted()) {
                this.value = value;
            }
        }
    }

    private void reject(Exception error) {
        synchronized (lock) {
            if (!isCompleted()) {
                this.error = error;
            }
        }
    }

    private boolean isCompleted() {
        return value != null || error != null;
    }

    /**
     * Returns the value of the operation for which this {@link Promise} is
     * a proxy. If the operation is <em>pending</em>, this will return an
     * empty optional. If the operation has <em>fulfilled</em>, this will
     * return the operation's result. If the operation has <em>rejected</em>,
     * calling this method will throw an exception.
     *
     * @throws RuntimeException if the {@link Promise} was rejected. The cause
     *         of the thrown exception will be the exception that was the
     *         reason for the rejection.
     */
    public Optional<T> getValue() {
        synchronized (lock) {
            if (error != null) {
                throw new RuntimeException("Promise produced an error", error);
            }

            return Optional.ofNullable(value);
        }
    }

    /**
     * Creates a {@link Promise} that will be fulfilled or rejected based on
     * the outcome of the specified operation.
     */
    public static <T> Promise<T> from(Subscribable<T> subject) {
        Promise<T> promise = new Promise<>(subject);
        subject.subscribe(promise::fulfill, promise::reject);
        return promise;
    }
}
