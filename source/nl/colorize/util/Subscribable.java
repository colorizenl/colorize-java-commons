//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Proxy for accessing multiple events that are produced by a (possibly
 * asynchronous) operation, allowing for publish/subscribe workflows.
 * Subscribers can be notified for events, for errors, or for both.
 * <p>
 * Such workflows can also be created in other ways, most commonly using
 * {@code java.util.concurrent} and its Flow API. However, the Flow API is
 * not yet supported on all platforms that are supported by this library,
 * making this class a more portable alternative.
 * <p>
 * On platforms that do support concurrency, {@link Subscribable} instances
 * are thread-safe and can be accessed from multiple threads.
 *
 * @param <T> The type of event that can be subscribed to.
 */
public final class Subscribable<T> {

    private List<Subscriber<T>> subscribers;
    private List<Object> history;
    private boolean completed;

    private static final Logger LOGGER = LogHelper.getLogger(Subscribable.class);

    public Subscribable() {
        this.subscribers = prepareList();
        this.history = prepareList();
        this.completed = false;
    }

    private <S> List<S> prepareList() {
        if (Platform.isTeaVM()) {
            return new ArrayList<>();
        } else {
            return new CopyOnWriteArrayList<>();
        }
    }

    /**
     * Publishes the next event to all event subscribers. This method does
     * nothing if this {@link Subscribable} has already been marked as
     * completed.
     */
    public void next(T event) {
        if (completed) {
            return;
        }

        for (Subscriber<T> subscriber : subscribers) {
            if (subscriber.onEvent != null) {
                subscriber.onEvent.accept(event);
            }
        }

        history.add(event);
    }

    /**
     * Publishes the next error to all error subscribers. If no error
     * subscribers have been registered, this will invoke the default error
     * handler that will print a log messsage describing the unhandled error.
     * This method does nothing if this {@link Subscribable} has already been
     * marked as completed.
     */
    public void nextError(Exception error) {
        if (completed) {
            return;
        }

        boolean handled = false;

        for (Subscriber<T> subscriber : subscribers) {
            if (subscriber.onError != null) {
                subscriber.onError.accept(error);
                handled = true;
            }
        }

        if (!handled) {
            LOGGER.warning("Unhandled error: " + error.getMessage());
        }

        history.add(error);
    }

    /**
     * Performs the specified operation and publishes the resulting event to
     * subscribers. If the operation completes normally, the return value is
     * published to subscribers as an event. If an exception occurs during
     * the operation, this exception is published to error subscribers.
     * Does nothing if this {@link Subscribable} has already been marked as
     * completed.
     */
    public void next(Callable<T> operation) {
        if (completed) {
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
     * Attempts to perform an operation for the specified number of attempts,
     * automatically retrying the operation if the initial attempt(s) failed.
     * Note {@code attempts} includes the original attempt, so the number of
     * retries is basically {@code attempts - 1}.
     * <p>
     * If the operation is not successful, an error is sent to subscribers
     * based on the last failed attempt.
     */
    public void retry(Callable<T> operation, int attempts) {
        retry(operation, attempts, 0L);
    }

    /**
     * Attempts to perform an operation for the specified number of attempts,
     * automatically retrying the operation if the initial attempt(s) failed.
     * The specified time delay (in milliseconds) is applied in between
     * attempts. Note {@code attempts} includes the original attempt, so the
     * number of retries is basically {@code attempts - 1}.
     * <p>
     * If the operation is not successful, an error is sent to subscribers
     * based on the last failed attempt.
     */
    public void retry(Callable<T> operation, int attempts, long delay) {
        Preconditions.checkArgument(attempts >= 1, "Invalid number of attempts: " + attempts);
        Preconditions.checkArgument(delay >= 0L, "Invalid delay: " + delay);

        Exception thrown = null;

        for (int i = 0; i < attempts; i++) {
            try {
                T event = operation.call();
                next(event);
                return;
            } catch (Exception e) {
                thrown = e;
                LOGGER.log(Level.WARNING, "Operation failed, retrying");
            }

            if (delay > 0L) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOGGER.warning("Retry delay interrupted");
                }
            }
        }

        LOGGER.warning("Operation failed after " + attempts + " attempts");
        nextError(thrown);
    }

    /**
     * Registers the specified callback functions as event and error
     * subscribers. The subscribers will immediately be notified of previously
     * published events and/or errors. The {@code id} parameter can be used to
     * identify the subscriber.
     *
     * @return This {@link Subscribable}, for method chaining.
     */
    public Subscribable<T> subscribe(UUID id, Consumer<T> onEvent, Consumer<Exception> onError) {
        Subscriber<T> subscriber = new Subscriber<>(id, onEvent, onError);
        subscribers.add(subscriber);

        for (Object historicEvent : history) {
            if (onError != null && historicEvent instanceof Exception error) {
                onError.accept(error);
            } else if (onEvent != null) {
                onEvent.accept((T) historicEvent);
            }
        }

        return this;
    }

    /**
     * Registers the specified callback functions as event and error
     * subscribers. The subscribers will immediately be notified of previously
     * published events and/or errors.
     *
     * @return This {@link Subscribable}, for method chaining.
     */
    public Subscribable<T> subscribe(Consumer<T> onEvent, Consumer<Exception> onError) {
        return subscribe(UUID.randomUUID(), onEvent, onError);
    }

    /**
     * Registers the specified callback function as an event subscriber. The
     * subscriber will immediately be notified of previously published events.
     *
     * @return This {@link Subscribable}, for method chaining.
     */
    public Subscribable<T> subscribe(Consumer<T> onEvent) {
        return subscribe(onEvent, null);
    }

    /**
     * Registers the specified callback function as an error subscriber. The
     * subscriber will immediately be notified of previously published errors.
     *
     * @return This {@link Subscribable}, for method chaining.
     */
    public Subscribable<T> subscribeErrors(Consumer<Exception> onError) {
        return subscribe(null, onError);
    }

    /**
     * Subscribes the specified other {@link Subscribable} to listen for both
     * events and errors generated by this {@link Subscribable}. This
     * effectively means that events published by this instance will be
     * forwarded to {@code subscriber}'s subscribers.
     *
     * @return This {@link Subscribable}, for method chaining.
     */
    public Subscribable<T> subscribe(Subscribable<T> subscriber) {
        return subscribe(subscriber::next, subscriber::nextError);
    }

    /**
     * Removes a previously registered subscriber, identifying the subscriber
     * by its ID. This method does nothing if none of the current subscribers
     * match the ID.
     */
    public void unsubscribe(UUID id) {
        subscribers.removeIf(subscriber -> subscriber.id.equals(id));
    }

    /**
     * Marks this {@link Subscribable} as completed, meaning that no new events
     * or errors will be published to subscribers. However, <em>old</em> events
     * might still be published when additional subscribers are registered.
     */
    public void complete() {
        completed = true;
    }

    /**
     * Returns a new {@link Subscribable} that will forward events to its own
     * subscribers, but first uses the specified mapping function on each event.
     * Errors will be forwarded as-is.
     */
    public <S> Subscribable<S> map(Function<T, S> mapper) {
        Subscribable<S> mapped = new Subscribable<>();

        Consumer<T> onEvent = event -> {
            try {
                S mappedEvent = mapper.apply(event);
                mapped.next(mappedEvent);
            } catch (Exception e) {
                mapped.nextError(e);
            }
        };

        subscribe(onEvent, mapped::nextError);

        return mapped;
    }

    /**
     * Returns a new {@link Subscribable} that will forward events to its own
     * subscribers, but only if the event matches the specified predicate.
     * Errors will be forwarded as-is.
     */
    public Subscribable<T> filter(Predicate<T> predicate) {
        Subscribable<T> filtered = new Subscribable<>();

        Consumer<T> onEvent = event -> {
            if (predicate.test(event)) {
                filtered.next(event);
            }
        };

        subscribe(onEvent, filtered::nextError);

        return filtered;
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
    public static <T> Subscribable<T> runBackground(Callable<T> operation) {
        Subscribable<T> subscribable = new Subscribable<>();

        Thread backgroundThread = new Thread(() -> subscribable.next(operation),
            "Subscribable-" + UUID.randomUUID());
        backgroundThread.start();

        return subscribable;
    }

    /**
     * Internal data structure that creates a subscriber objects from callback
     * methods. Callback methods can be {@code null} if the subscriber is not
     * interested in certain events.
     */
    private record Subscriber<T>(UUID id, Consumer<T> onEvent, Consumer<Exception> onError) {
    }
}
