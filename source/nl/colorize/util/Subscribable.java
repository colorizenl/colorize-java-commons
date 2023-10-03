//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Proxy for accessing multiple messages/values/events that are produced
 * by a (possibly asynchronous) operation, allowing for publish/subscribe
 * workflows. It is possible to subscribe to events, to errors, or to both.
 * <p>
 * Such workflows can also be created in other ways, most commonly using
 * {@code java.util.concurrent}. The main benefit of this class is that
 * it can be used on all platforms supported by this library, including
 * platforms that do not support the {@code java.util.concurrent} API,
 * such as TeaVM.
 * <p>
 * On platforms that <em>do</em> support concurrency, {@link Subscribable}
 * instances are thread-safe and can be accessed from multiple threads.
 *
 * @param <T> The type of event that can be subscribed to.
 */
public final class Subscribable<T> {

    private List<Consumer<T>> eventSubscribers;
    private List<Consumer<Exception>> errorSubscribers;

    private List<T> history;
    private List<Exception> errorHistory;

    private boolean disposed;

    private static final Logger LOGGER = LogHelper.getLogger(Subscribable.class);

    public Subscribable() {
        this.eventSubscribers = prepareList();
        this.errorSubscribers = prepareList();

        this.history = prepareList();
        this.errorHistory = prepareList();

        this.disposed = false;
    }

    private <S> List<S> prepareList() {
        if (Platform.isTeaVM()) {
            return new ArrayList<>();
        }
        return new CopyOnWriteArrayList<>();
    }

    public void next(T event) {
        if (disposed) {
            return;
        }

        for (Consumer<T> subscriber : eventSubscribers) {
            subscriber.accept(event);
        }

        history.add(event);
    }

    public void nextError(Exception error) {
        if (disposed) {
            return;
        }

        for (Consumer<Exception> subscriber : errorSubscribers) {
            subscriber.accept(error);
        }

        if (errorSubscribers.isEmpty()) {
            LOGGER.warning("Unhandled error: " + error.getMessage());
        }

        errorHistory.add(error);
    }

    /**
     * Performs the specified operation and publishes the resulting event to
     * subscribers. If the operation completes normally, the return value is
     * published to subscribers. If the operation produces an exception, this
     * exception is published to error subscribers.
     */
    public void next(Callable<T> operation) {
        try {
            T event = operation.call();
            next(event);
        } catch (Exception e) {
            nextError(e);
        }
    }

    public Subscribable<T> subscribe(Consumer<T> onEvent, Consumer<Exception> onError) {
        subscribe(onEvent);
        subscribeErrors(onError);
        return this;
    }

    public Subscribable<T> subscribe(Consumer<T> onEvent) {
        Preconditions.checkState(!disposed, "Subscribable has already been disposed");
        eventSubscribers.add(onEvent);
        history.forEach(onEvent);
        return this;
    }

    public Subscribable<T> subscribeErrors(Consumer<Exception> onError) {
        Preconditions.checkState(!disposed, "Subscribable has already been disposed");
        errorSubscribers.add(onError);
        errorHistory.forEach(onError);
        return this;
    }

    //TODO unsubscribe

    /**
     * Cancels all existing subscriptions, and blocks new subscribers from
     * being added.
     */
    public void dispose() {
        Preconditions.checkState(!disposed, "Subscribable has already been disposed");
        disposed = true;
        eventSubscribers.clear();
        errorSubscribers.clear();
    }

    public <S> Subscribable<S> map(Function<T, S> mapper) {
        Subscribable<S> mapped = new Subscribable<>();
        subscribe(event -> {
            try {
                S mappedEvent = mapper.apply(event);
                mapped.next(mappedEvent);
            } catch (Exception e) {
                mapped.nextError(e);
            }
        });
        subscribeErrors(mapped::nextError);
        return mapped;
    }

    /**
     * Returns a {@link Promise} that is based on the first event or the first
     * error produced by this {@link Subscribable}. If <em>multiple</em> events
     * are produced, only the first is represented by the returned promise.
     */
    public Promise<T> toPromise() {
        return Promise.from(this);
    }

    /**
     * Creates a {@link Subscribable} that will publish the specified values
     * to its subscribers.
     */
    @SafeVarargs
    public static <T> Subscribable<T> of(T... values) {
        Subscribable<T> subscribable = new Subscribable<>();
        for (T value : values) {
            subscribable.next(value);
        }
        return subscribable;
    }

    /**
     * Creates a {@link Subscribable} that will publish the specified values
     * to its subscribers.
     */
    public static <T> Subscribable<T> of(Iterable<T> values) {
        Subscribable<T> subscribable = new Subscribable<>();
        for (T value : values) {
            subscribable.next(value);
        }
        return subscribable;
    }

    /**
     * Performs the specified operation and returns a {@link Subscribable} that
     * can be used to subscribe to the results.
     */
    public static <T> Subscribable<T> run(Callable<T> operation) {
        Subscribable<T> subscribable = new Subscribable<>();
        subscribable.next(operation);
        return subscribable;
    }

    /**
     * Performs the specified operation in a new background thread, and
     * returns a {@link Subscribable} that can be used to subscribe to
     * the background operation.
     */
    public static <T> Subscribable<T> runAsync(Callable<T> operation) {
        Subscribable<T> subscribable = new Subscribable<>();

        Thread backgroundThread = new Thread(() -> subscribable.next(operation),
            "Subscribable-" + UUID.randomUUID());
        backgroundThread.start();

        return subscribable;
    }
}
