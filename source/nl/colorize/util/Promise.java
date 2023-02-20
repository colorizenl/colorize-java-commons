//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Proxy for accessing a value produced by an asynchronous operation. Callback
 * functions can be registered to handle the operation's eventual success or
 * failure, which are then invoked at some point in the future.
 * <p>
 * The promise can exist in one of three possible states:
 * <ul>
 *   <li>Pending: Asynchronous operation is still in progress.</li>
 *   <li>Fulfilled: Asynchronous operation has completed.</li>
 *   <li>Rejected: Asynchronous operation has failed.</li>
 *   <li>Cancelled: Operation will proceed, but abort all result and error handlers.</li>
 * </ul>
 * <p>
 * This class has a similar purpose as {@code Future}, but is also available on
 * platforms where {@code java.util.concurrent} is not supported. The promise
 * design pattern is fairly common in other languages, with the JavaScript
 * implementation being the most well-known.
 * <p>
 * More information:
 * <ul>
 *   <li>
 *     <a href="https://java-design-patterns.com/patterns/promise/">
 *       The Promise design pattern
 *     </a>
 *   </li>
 *   <li>
 *     <a href="https://en.wikipedia.org/wiki/Futures_and_promises">
 *       Future versus Promise on Wikipedia
 *     </a>
 *   </li>
 *   <li>
 *     <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise">
 *       JavaScript Promise on MDN
 *     </a>
 *   </li>
 * </ul>
 *
 * @param <T> The type of value that is produced by the asynchronous operation.
 */
public class Promise<T> {

    private State state;
    private T value;
    private Exception error;
    private List<Consumer<T>> resultHandlers;
    private List<Consumer<Exception>> errorHandlers;

    public Promise() {
        this.state = State.PENDING;
        this.resultHandlers = new ArrayList<>();
        this.errorHandlers = new ArrayList<>();
    }

    /**
     * Fulfills this {@link Promise} with the specified value. This method
     * should be called from the asynchronous operation.
     */
    public void resolve(T value) {
        Preconditions.checkState(state == State.PENDING || state == State.CANCELLED,
            "Cannot fulfill Promise in state " + state);

        if (state == State.PENDING) {
            this.state = State.FULFILLED;
            this.value = value;
            checkHandlers();
        }
    }

    /**
     * Fulfills this {@link Promise} with the specified error. This method
     * should be called from the asynchronous operation.
     */
    public void reject(Exception error) {
        Preconditions.checkState(state == State.PENDING || state == State.CANCELLED,
            "Cannot fulfill Promise in state " + state);

        if (state == State.PENDING) {
            this.state = State.REJECTED;
            this.error = error;
            checkHandlers();
        }
    }

    /**
     * Runs the specified operation, and uses the resulting value to resolve
     * this promise. If an error occurs while performing the operation, the
     * error is used to reject the promise. This method should be called from
     * the asynchronous operation.
     */
    public void fulfill(Supplier<T> operation) {
        try {
            T value = operation.get();
            resolve(value);
        } catch (Exception e) {
            reject(e);
        }
    }

    /**
     * Cancels this {@link Promise}. This will <em>not</em> stop the
     * asynchronous operation itself, but it will cancel result and error
     * handlers.
     */
    public void cancel() {
        state = State.CANCELLED;
    }

    private void checkHandlers() {
        if (state == State.FULFILLED) {
            resultHandlers.forEach(handler -> handler.accept(value));
            resultHandlers.clear();
        } else if (state == State.REJECTED) {
            errorHandlers.forEach(handler -> handler.accept(error));
            errorHandlers.clear();
        }
    }

    public Promise<T> then(Consumer<T> resultHandler, Consumer<Exception> errorHandler) {
        resultHandlers.add(resultHandler);
        errorHandlers.add(errorHandler);
        checkHandlers();
        return this;
    }

    public Promise<T> then(Consumer<T> resultHandler) {
        resultHandlers.add(resultHandler);
        checkHandlers();
        return this;
    }

    public Promise<T> thenCatch(Consumer<Exception> errorHandler) {
        errorHandlers.add(errorHandler);
        checkHandlers();
        return this;
    }

    /**
     * Adds a result handler that operates on the {@link Promise}'s value, and
     * then returns a new {@link Promise} for the new value.
     */
    public <U> Promise<U> thenMap(Function<T, U> resultMapper) {
        Promise<U> mappedPromise = new Promise<>();

        resultHandlers.add(originalValue -> {
            try {
                U mappedValue = resultMapper.apply(originalValue);
                mappedPromise.resolve(mappedValue);
            } catch (Exception e) {
                mappedPromise.reject(e);
            }
        });

        checkHandlers();
        return mappedPromise;
    }

    /**
     * Creates a new {@link Promise} that will immediately be fulfilled with
     * the specified value. This allows for interoperability between synchronous
     * code and code that expects a {@link Promise}.
     */
    public static <T> Promise<T> resolveWith(T value) {
        Promise<T> promise = new Promise<>();
        promise.resolve(value);
        return promise;
    }

    /**
     * Creates a new {@link Promise} that will immediately be fulfilled with
     * the specified error. This allows for interoperability between synchronous
     * code and code that expects a {@link Promise}.
     */
    public static <T> Promise<T> rejectWith(Exception error) {
        Promise<T> promise = new Promise<>();
        promise.reject(error);
        return promise;
    }

    /**
     * Creates a new {@link Promise} that will immediately perform the specified
     * (synchronous) operation, and either resolve or reject itself depending on
     * the outcome.
     */
    public static <T> Promise<T> fulfillWith(Supplier<T> operation) {
        Promise<T> promise = new Promise<>();
        promise.fulfill(operation);
        return promise;
    }

    private static enum State {
        PENDING,
        FULFILLED,
        REJECTED,
        CANCELLED
    }
}
