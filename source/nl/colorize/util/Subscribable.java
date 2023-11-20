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
import java.util.function.Predicate;
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

    /**
     * Publishes the next event to all event subscribers. If subscriptions
     * have already been disposed, calling this method does nothing.
     */
    public void next(T event) {
        if (disposed) {
            return;
        }

        for (Consumer<T> subscriber : eventSubscribers) {
            subscriber.accept(event);
        }

        history.add(event);
    }

    /**
     * Publishes the next error to all error subscribers. If no error
     * subscribers exist, publishing an error will result in the error
     * being logged. If subscriptions have already been disposed, calling
     * this method does nothing.
     */
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
     * <p>
     * If subscriptions have already been disposed, calling this method does
     * nothing.
     */
    public void next(Callable<T> operation) {
        if (disposed) {
            return;
        }

        try {
            T event = operation.call();
            next(event);
        } catch (Exception e) {
            nextError(e);
        }
    }

    /**
     * Registers the specified callback functions as event and error subscribers,
     * respectively. The subscribers will immediately be notified of previously
     * published events and/or errors.
     *
     * @throws IllegalStateException when trying to subscribe when subscriptions
     *         have alreasdy been disposed.
     */
    public Subscribable<T> subscribe(Consumer<T> onEvent, Consumer<Exception> onError) {
        subscribe(onEvent);
        subscribeErrors(onError);
        return this;
    }

    /**
     * Registers the specified callback function as event subscriber. The
     * subscriber will immediately be notified of previously published events.
     *
     * @throws IllegalStateException when trying to subscribe when subscriptions
     *         have alreasdy been disposed.
     */
    public Subscribable<T> subscribe(Consumer<T> onEvent) {
        Preconditions.checkState(!disposed, "Subscribable has already been disposed");
        eventSubscribers.add(onEvent);
        history.forEach(onEvent);
        return this;
    }

    /**
     * Registers the specified callback function as error subscriber. The
     * subscriber will immediately be notified of previously published errors.
     *
     * @throws IllegalStateException when trying to subscribe when subscriptions
     *         have alreasdy been disposed.
     */
    public Subscribable<T> subscribeErrors(Consumer<Exception> onError) {
        Preconditions.checkState(!disposed, "Subscribable has already been disposed");
        errorSubscribers.add(onError);
        errorHistory.forEach(onError);
        return this;
    }

    /**
     * Subscribes the specified other {@link Subscribable} to listen for both
     * events and errors generated by this {@link Subscribable}.
     */
    public Subscribable<T> subscribe(Subscribable<T> subscriber) {
        subscribe(subscriber::next, subscriber::nextError);
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

    /**
     * Returns a new {@link Subscribable} that will forward events to its own
     * subscribers, but first uses the specified mapping function on each event.
     * Errors will be forwarded as-is.
     */
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
     * Returns a new {@link Subscribable} that will forward events to its own
     * subscribers, but only if the event matches the specified predicate.
     * Errors will be forwarded as-is.
     */
    public Subscribable<T> filter(Predicate<T> predicate) {
        Subscribable<T> filtered = new Subscribable<>();
        subscribe(event -> {
            if (predicate.test(event)) {
                filtered.next(event);
            }
        });
        subscribeErrors(filtered::nextError);
        return filtered;
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

    /**
     * Returns a {@link Subscribable} that will publish the specified error.
     */
    public static <T> Subscribable<T> fail(Exception error) {
        Subscribable<T> subscribable = new Subscribable<>();
        subscribable.nextError(error);
        return subscribable;
    }
}
