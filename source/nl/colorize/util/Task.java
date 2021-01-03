//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private Exception failure;
    private List<Consumer<T>> resultCallbacks;
    private List<Consumer<Exception>> failureCallbacks;

    private static final Logger LOGGER = LogHelper.getLogger(Task.class);

    private static enum OperationState {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public Task() {
        this.state = OperationState.PENDING;
        this.resultCallbacks = new ArrayList<>();
        this.failureCallbacks = new ArrayList<>();
    }

    /**
     * Indicates the asynchronous operation has been successfully completed, and
     * will notify all registered callback functions with the results.
     *
     * @throws IllegalStateException if the operation has already completed or
     *         failed.
     */
    public void complete(T value) {
        resolve(value, null);
    }

    /**
     * Indicates that the asynchronous operation has failed, and will notify all
     * registered failure callback function with the exception.
     *
     * @throws IllegalStateException if the operation has already completed or
     *         failed.
     */
    public void fail(Exception failure) {
        resolve(null, failure);
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

    private synchronized void resolve(T result, Exception failure) {
        Preconditions.checkState(state == OperationState.PENDING,
            "Asynchronous operation has already been completed: " + state);

        this.result = result;
        this.failure = failure;

        if (result != null) {
            state = OperationState.COMPLETED;
            resultCallbacks.forEach(callback -> callback.accept(result));
        } else if (failure != null) {
            state = OperationState.FAILED;
            failureCallbacks.forEach(callback -> callback.accept(failure));
        } else {
            state = OperationState.CANCELLED;
        }
    }

    /**
     * Returns the result of the asynchronous operation. If the operation has
     * succeeded, this will return the result. If the operation has failed,
     * this will throw the corresponding exception. If the operation has been
     * cancelled, this will also throw an exception.
     * <p>
     * In general, it is preferred to obtain the task's results by registering
     * a callback function using {@link #then(Consumer, Consumer)}, instead of
     * accessing the result in a synchronous way using this method.
     *
     * @throws IllegalStateException if the operation is still pending, or if
     *         the operation has been cancelled.
     */
    public T get() {
        return getResult();
    }

    private synchronized T getResult() {
        Preconditions.checkState(state != OperationState.PENDING, "Operation is still pending");
        Preconditions.checkState(state != OperationState.CANCELLED, "Operation has been cancelled");
        Preconditions.checkState(state != OperationState.FAILED, "Operation has failed");

        return result;
    }

    /**
     * Adds the specified callbacks that should be notified when the asynchronous
     * operation has either completed or failed. Even if this is already the
     * case at the moment when the callback is added, it will still be notified.
     *
     * @return This task, for method chaining.
     */
    public Task<T> then(Consumer<T> resultCallback, Consumer<Exception> failureCallback) {
        resultCallbacks.add(resultCallback);
        failureCallbacks.add(failureCallback);

        if (state == OperationState.COMPLETED) {
            resultCallback.accept(result);
        } else if (state == OperationState.FAILED) {
            failureCallback.accept(failure);
        }

        return this;
    }

    /**
     * Adds the specified callback that should be notified when the asynchronous
     * operation has completed. Even if this is already the case at the moment
     * when the callback is added, it will still be notified.
     *
     * @return This task, for method chaining.
     */
    public Task<T> then(Consumer<T> resultCallback) {
        return then(resultCallback,
            e -> LOGGER.log(Level.WARNING, "Asynchronous operation failed", e));
    }

    /**
     * Adds the specified callback that will be notified when the asynchronous
     * operation has failed.
     *
     * @return This task, for method chaining.
     */
    public Task<T> error(Consumer<Exception> failureCallback) {
        Consumer<T> noOpCallback = result -> {};
        return then(noOpCallback, failureCallback);
    }

    /**
     * Converts the result of this task using the specified function, and wraps
     * this into a new {@code Task} instance that can be used to process the
     * result.
     *
     * @return The new task that can be used to process the converted value.
     */
    public <U> Task<U> pipe(Function<T, U> converter) {
        Task<U> pipedTask = new Task<>();
        then(response -> pipedTask.complete(converter.apply(response)));
        error(pipedTask::fail);
        return pipedTask;
    }
}
