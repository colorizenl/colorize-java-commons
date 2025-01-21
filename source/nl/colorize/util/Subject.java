//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publishes (possibly asynchronous) events to registered subscribers. This
 * class implements the <a href="https://openjdk.org/jeps/266">Flow API</a>.
 * It can be used on all platforms supported by this library, including
 * <a href="https://teavm.org">TeaVM</a>. The term "subject" originates from
 * the original Gang of Four's description of the
 * <a href="https://en.wikipedia.org/wiki/Observer_pattern">observer pattern</a>.
 * <p>
 * {@link Subject} instances are thread-safe and can be accessed from different
 * threads. This facilitates workflows where publishers and subscribers operate
 * on different threads.
 *
 * @param <T> The type of event that can be subscribed to.
 */
public final class Subject<T> implements Publisher<T> {

    private List<SubjectSubscription> subscriptions;
    private List<Object> history;
    private boolean completed;

    private static final Logger LOGGER = LogHelper.getLogger(Subject.class);

    public Subject() {
        this.completed = false;
        this.subscriptions = new CopyOnWriteArrayList<>();
        this.history = new CopyOnWriteArrayList<>();
    }

    /**
     * Publishes the next event to all event subscribers. This method does
     * nothing if this {@link Subject} has already been marked as
     * completed.
     */
    public void next(T event) {
        if (!completed) {
            for (SubjectSubscription subscription : subscriptions) {
                subscription.subscriber.onNext(event);
            }

            history.add(event);
        }
    }

    /**
     * Publishes the next error to all error subscribers. If no error
     * subscribers have been registered, this will invoke the default error
     * handler that will print a log messsage describing the unhandled error.
     * This method does nothing if this {@link Subject} has already been
     * marked as completed.
     */
    public void nextError(Throwable error) {
        if (!completed) {
            for (SubjectSubscription subscription : subscriptions) {
                subscription.subscriber.onError(error);
            }

            history.add(error);
        }
    }

    /**
     * Performs the specified operation and publishes the resulting event to
     * subscribers. If the operation completes normally, the return value is
     * published to subscribers as an event. If an exception occurs during
     * the operation, this exception is published to error subscribers.
     * Does nothing if this {@link Subject} has already been marked as
     * completed.
     */
    public void next(Callable<T> operation) {
        if (!completed) {
            try {
                T event = operation.call();
                next(event);
            } catch (Exception e) {
                nextError(e);
            }
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
     * Registers the specified subscriber to receive published events and
     * errors. The subscriber will immediately be notified of previously
     * published events.
     */
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        registerSubscription(subscriber);
    }

    private Subscription registerSubscription(Subscriber<? super T> subscriber) {
        Preconditions.checkNotNull(subscriber, "Null subscriber");

        SubjectSubscription subscription = new SubjectSubscription(subscriber);
        subscriptions.add(subscription);
        subscriber.onSubscribe(subscription);
        sendHistoryEvents(subscriber);
        return subscription;
    }

    private void sendHistoryEvents(Subscriber<? super T> subscriber) {
        for (Object historicEvent : history) {
            if (historicEvent instanceof Exception error) {
                subscriber.onError(error);
            } else {
                subscriber.onNext((T) historicEvent);
            }
        }
    }

    /**
     * Registers the specified callback function as a subscriber for events.
     * The subscriber will log errors, but not explicitly handle them.
     * Returns a {@link Subscription} for the registered subscriber.
     */
    public Subscription subscribe(Consumer<T> eventCallback) {
        Subscriber<T> subscriber = createSubscriber(eventCallback);
        return registerSubscription(subscriber);
    }

    /**
     * Registers the specified callback functions as event and error
     * subscribers. The subscribers will immediately be notified of
     * previously published events and/or errors. Returns a
     * {@link Subscription} for the registered subscriber.
     */
    public Subscription subscribe(Consumer<T> eventCallback, Consumer<Throwable> errorCallback) {
        Subscriber<T> subscriber = createSubscriber(eventCallback, errorCallback);
        return registerSubscription(subscriber);
    }

    /**
     * Registers the specified callback function as an error subscriber. The
     * subscriber will immediately be notified of previously published errors.
     * Returns a {@link Subscription} for the registered subscriber.
     */
    public Subscription subscribeErrors(Consumer<Throwable> onError) {
        Consumer<T> onEvent = event -> {};
        Subscriber<T> subscriber = createSubscriber(onEvent, onError);
        return registerSubscription(subscriber);
    }

    /**
     * Subscribes the specified other {@link Subject} to listen for both
     * events and errors generated by this {@link Subject}. This
     * effectively means that events published by this instance will be
     * forwarded to {@code subscriber}'s subscribers. Returns a
     * {@link Subscription} for the registered subscriber.
     */
    public Subscription subscribe(Subject<T> subscriber) {
        return subscribe(subscriber::next, subscriber::nextError);
    }

    /**
     * Creates and registers a {@link Subscriber} that collects received events
     * into the specified queue. Consumers can then poll this queue to
     * periodically process events.
     */
    public void collect(Queue<T> messageQueue) {
        Subscriber<T> subscriber = createSubscriber(messageQueue::offer);
        subscribe(subscriber);
    }

    /**
     * Unsubscribes the specified subscription. If the subscriber is not
     * currently registered with this {@link Subject}, this method does
     * nothing.
     *
     * @deprecated Prefer using {@link Subscription#cancel()} instead.
     */
    @Deprecated
    public void unsubscribe(Subscription subscription) {
        subscription.cancel();
    }

    /**
     * Removes a previously registered subscriber. If the subscriber is not
     * currently registered with this {@link Subject}, this method does
     * nothing.
     *
     * @deprecated Prefer using {@link Subscription#cancel()} instead.
     */
    @Deprecated
    public void unsubscribe(Subscriber<? super T> subscriber) {
        subscriptions.removeIf(subscription -> subscription.subscriber.equals(subscriber));
    }

    /**
     * Marks this {@link Subject} as completed, meaning that no new events
     * or errors will be published to subscribers. However, <em>old</em> events
     * might still be published when additional subscribers are registered.
     */
    public void complete() {
        if (!completed) {
            completed = true;

            for (SubjectSubscription subscription : subscriptions) {
                subscription.subscriber.onComplete();
            }
        }
    }

    /**
     * Returns a new {@link Subject} that will forward events to its own
     * subscribers, but first uses the specified mapping function on each event.
     * Errors will be forwarded as-is.
     */
    public <S> Subject<S> map(Function<T, S> mapper) {
        Subject<S> mapped = new Subject<>();

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
     * Returns a new {@link Subject} that will forward events to its own
     * subscribers, but only if the event matches the specified predicate.
     * Errors will be forwarded as-is.
     */
    public Subject<T> filter(Predicate<T> predicate) {
        Subject<T> filtered = new Subject<>();

        Consumer<T> onEvent = event -> {
            if (predicate.test(event)) {
                filtered.next(event);
            }
        };

        subscribe(onEvent, filtered::nextError);

        return filtered;
    }

    /**
     * Creates a {@link Subject} that will publish the specified values
     * to its subscribers.
     */
    @SafeVarargs
    public static <T> Subject<T> of(T... values) {
        Subject<T> subject = new Subject<>();
        for (T value : values) {
            subject.next(value);
        }
        return subject;
    }

    /**
     * Creates a {@link Subject} that will publish the specified values
     * to its subscribers.
     */
    public static <T> Subject<T> of(Iterable<T> values) {
        Subject<T> subject = new Subject<>();
        for (T value : values) {
            subject.next(value);
        }
        return subject;
    }

    /**
     * Performs the specified operation and returns a {@link Subject} that
     * can be used to subscribe to the results.
     */
    public static <T> Subject<T> run(Callable<T> operation) {
        Subject<T> subject = new Subject<>();
        subject.next(operation);
        return subject;
    }

    /**
     * Performs the specified operation in a new background thread, and
     * returns a {@link Subject} that can be used to subscribe to
     * the background operation.
     */
    public static <T> Subject<T> runBackground(Callable<T> operation) {
        Subject<T> subject = new Subject<>();

        Thread backgroundThread = new Thread(() -> subject.next(operation),
            "Subscribable-" + UUID.randomUUID());
        backgroundThread.start();

        return subject;
    }

    /**
     * Returns an implementation of the {@link Subscriber} interface that
     * implements {@link Subscriber#onNext(Object)} using a callback method.
     * Errors will be logged, all other methods use no-op implementations.
     */
    public static <T> Subscriber<T> createSubscriber(Consumer<T> eventCallback) {
        return createSubscriber(
            eventCallback,
            e -> LOGGER.warning("Unhandled subscriber error: " + e.getMessage())
        );
    }

    /**
     * Returns an implementation of the {@link Subscriber} interface that
     * implements {@link Subscriber#onNext(Object)} and
     * {@link Subscriber#onError(Throwable)} using callback methods. The
     * other methods use no-op implementations.
     */
    public static <T> Subscriber<T> createSubscriber(Consumer<T> eventFn, Consumer<Throwable> errorFn) {
        return new CallbackSubscriber<>(eventFn, errorFn);
    }

    /**
     * Implementation of the {@link Subscription} interface that allows a
     * subscriber to unsubscribe itself.
     */
    @AllArgsConstructor
    private class SubjectSubscription implements Subscription {

        private Subscriber<? super T> subscriber;

        @Override
        public void request(long n) {
            // {@link Subject} already informs subscribers
            // of historic events, so requesting additional
            // events has no meaning in this implementation.
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {
            subscriptions.removeIf(this::equals);
        }
    }

    /**
     * Implementation of the {@link Subscriber} interface that uses callback
     * methods to process incoming events and errors.
     */
    @AllArgsConstructor
    private static class CallbackSubscriber<T> implements Subscriber<T> {

        private Consumer<T> eventCallback;
        private Consumer<Throwable> errorCallback;

        @Override
        public void onSubscribe(Subscription subscription) {
        }

        @Override
        public void onNext(T event) {
            eventCallback.accept(event);
        }

        @Override
        public void onError(Throwable error) {
            errorCallback.accept(error);
        }

        @Override
        public void onComplete() {
        }
    }
}
