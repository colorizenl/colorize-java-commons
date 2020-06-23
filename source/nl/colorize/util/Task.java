//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents the result of a task that is being executed by an asynchronous
 * operation. The role and methods of this class are similar to {@code Future}
 * in general and {@code CompletableFuture} in particular. However, this class
 * is intended for environments in which conventional Java concurrency features
 * are not available or not supported and instead offer their own mechanisms
 * for performing asynchronous tasks, such as TeaVM.
 *
 * @param <T> The type of result that is returned by the asynchronous operation.
 */
public final class Task<T> {

    private OperationState state;
    private T result;
    private RuntimeException failure;
    private List<Consumer<Supplier<T>>> callbacks;

    private static enum OperationState {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public Task() {
        this.state = OperationState.PENDING;
        this.callbacks = new ArrayList<>();
    }

    /**
     * Returns the result of the asynchronous operation. If the operation has
     * succeeded, this will return the result. If the operation has failed,
     * this will throw the corresponding exception. If the operation has been
     * cancelled, this will also throw an exception.
     *
     * @throws IllegalStateException if the operation is still pending, or if
     *         the operation has been cancelled.
     * @throws RuntimeException if the operation has failed with an exception.
     */
    public T get() {
        return getResult();
    }

    /**
     * Adds the specified callback that should be notified once the asynchronous
     * operation has either completed or failed. Even if this is already the
     * case at the moment when the callback is added, it will still be notified.
     */
    public void then(Consumer<Supplier<T>> callback) {
        callbacks.add(callback);

        if (state != OperationState.PENDING) {
            callback.accept(this::get);
        }
    }

    /**
     * Indicates the asynchronous operation has been successfully completed, and
     * will use the provided value for all calls to {@link #get()}.
     *
     * @throws IllegalStateException if the operation has already completed or
     *         failed.
     */
    public void complete(T value) {
        resolve(value, null);
        notifyCallbacks();
    }

    /**
     * Indicates that the asynchronous operation has failed. Afterwards, calls
     * to {@link #get()} will throw the provided exception.
     *
     * @throws IllegalStateException if the operation has already completed or
     *         failed.
     */
    public void fail(Exception failure) {
        resolve(null, wrapException(failure));
        notifyCallbacks();
    }

    private RuntimeException wrapException(Exception failure) {
        if (failure instanceof RuntimeException) {
            return (RuntimeException) failure;
        } else {
            return new RuntimeException("Asynchronous operation failed", failure);
        }
    }

    /**
     * Cancels the asynchronous operation, meaning that the callbacks will never
     * be invoked.
     *
     * @throws IllegalStateException if the operation has already completed or
     *         failed.
     */
    public void cancel() {
        resolve(null, null);
    }

    private void notifyCallbacks() {
        for (Consumer<Supplier<T>> callback : callbacks) {
            callback.accept(this::get);
        }
    }

    private synchronized void resolve(T result, RuntimeException failure) {
        Preconditions.checkState(state == OperationState.PENDING,
            "Asynchronous operation has already been completed: " + state);

        this.result = result;
        this.failure = failure;

        if (result != null) {
            state = OperationState.COMPLETED;
        } else if (failure != null) {
            state = OperationState.FAILED;
        } else {
            state = OperationState.CANCELLED;
        }
    }

    private synchronized T getResult() {
        Preconditions.checkState(state != OperationState.PENDING, "Operating is still pending");
        Preconditions.checkState(state != OperationState.CANCELLED, "Operating has been cancelled");

        if (failure != null) {
            throw failure;
        }

        return result;
    }
}
