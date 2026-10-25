//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Publishes (possibly asynchronous) events to registered subscribers. This
 * class provides an implementation of a publisher in the {@link Flow} API.
 * It can be used on all platforms supported by this library, including
 * <a href="https://teavm.org">TeaVM</a>.
 * <p>
 * This is an extension of the {@link Publisher} interface that supports
 * subscribers based on callback functions, without needing to implement
 * the full {@link Subscriber} interface for every subscriber.
 * <p>
 * {@link Subject} instances are thread-safe and can be accessed from
 * different threads. This facilitates workflows where publishers and
 * subscribers operate on different threads.
 *
 * @param <T> The type of event that can be subscribed to.
 */
public final class Subject<T> implements Publisher<T> {

    private List<Subscriber<? super T>> subscribers;
    private List<Object> undelivered;
    private boolean completed;

    private static final Logger LOGGER = LogHelper.getLogger(Subject.class);

    public Subject() {
        this.subscribers = new CopyOnWriteArrayList<>();
        this.undelivered = new CopyOnWriteArrayList<>();
        this.completed = false;
    }

    /**
     * Publishes the next event to all event subscribers. This method does
     * nothing if this {@link Subject} has already been marked as
     * completed.
     */
    public void next(T event) {
        if (completed) {
            return;
        }

        if (subscribers.isEmpty()) {
            undelivered.add(event);
            return;
        }

        for (Subscriber<? super T> subscriber : subscribers) {
            subscriber.onNext(event);
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
        if (completed) {
            return;
        }

        if (subscribers.isEmpty()) {
            undelivered.add(error);
            return;
        }

        for (Subscriber<? super T> subscriber : subscribers) {
            subscriber.onError(error);
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
     * Registers the specified subscriber to receive published events and
     * errors. If there are currently undelivered events or errors, the new
     * subscriber will immediately be notified of them.
     */
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        registerSubscription(subscriber);
    }

    private void registerSubscription(Subscriber<? super T> subscriber) {
        subscribers.add(subscriber);
        Subscription subscription = makeSubscription(subscriber);
        subscriber.onSubscribe(subscription);
        if (!undelivered.isEmpty()) {
            sendUndelivered(subscriber);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendUndelivered(Subscriber<? super T> subscriber) {
        for (Object undeliveredEvent : undelivered) {
            if (undeliveredEvent instanceof Throwable error) {
                subscriber.onError(error);
            } else {
                subscriber.onNext((T) undeliveredEvent);
            }
        }
    }

    private Subscription makeSubscription(Subscriber<? super T> subscriber) {
        return new Subscription() {
            @Override
            public void request(long n) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void cancel() {
                subscribers.remove(subscriber);
            }
        };
    }

    /**
     * Registers a subscriber that will invoke the specified callback functions
     * on events, errors, and completion, respectively. The new subscriber will
     * immediately be notified of any undelivered events.
     *
     * @return This {@link Subject}, for method chaining.
     */
    public Subject<T> subscribe(Consumer<T> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
        Subscriber<T> subscriber = new CallbackSubscriber<>(onEvent, onError, onComplete);
        registerSubscription(subscriber);
        return this;
    }

    /**
     * Registers a subscriber that will invoke the specified callback functions
     * on events and errors, respectively. The new subscriber will immediately
     * be notified of any undelivered events.
     *
     * @return This {@link Subject}, for method chaining.
     */
    public Subject<T> subscribe(Consumer<T> onEvent, Consumer<Throwable> onError) {
        Subscriber<T> subscriber = new CallbackSubscriber<>(onEvent, onError, () -> {});
        registerSubscription(subscriber);
        return this;
    }

    /**
     * Registers a subscriber that will invoke the specified callback functions
     * on events. The new subscriber will immediately be notified of any
     * undelivered events.
     *
     * @return This {@link Subject}, for method chaining.
     */
    public Subject<T> subscribe(Consumer<T> onEvent) {
        Consumer<Throwable> onError = e -> LOGGER.log(Level.SEVERE, "Unhandled subscriber error", e);
        Subscriber<T> subscriber = new CallbackSubscriber<>(onEvent, onError, () -> {});
        registerSubscription(subscriber);
        return this;
    }

    /**
     * Registers a subscriber that will invoke the specified callback function
     * on errors. The new subscriber will immediately be notified of any
     * undelivered errors.
     *
     * @return This {@link Subject}, for method chaining.
     */
    public Subject<T> subscribeErrors(Consumer<Throwable> onError) {
        Subscriber<T> subscriber = new CallbackSubscriber<>(_ -> {}, onError, () -> {});
        registerSubscription(subscriber);
        return this;
    }

    /**
     * Registers a subscriber that will invoke the specified callback function
     * when this {@link Subject} is marked as completed.
     *
     * @return This {@link Subject}, for method chaining.
     */
    public Subject<T> subscribeComplete(Runnable onComplete) {
        Subscriber<T> subscriber = new CallbackSubscriber<>(_ -> {}, _ -> {}, onComplete);
        registerSubscription(subscriber);
        return this;
    }

    /**
     * Subscribes the specified other {@link Subject} to listen for both
     * events and errors generated by this {@link Subject}. This
     * effectively means that events published by this instance will be
     * forwarded to {@code subscriber}'s subscribers.
     * <p>
     * Unlike other {@code subscribe} methods, this does <em>not</em> return
     * a value to avoid confusion on whether {@link Subject} is returned
     * or {@code otherSubscriber} is returned.
     */
    public void subscribe(Subject<T> otherSubscriber) {
        subscribe(otherSubscriber::next, otherSubscriber::nextError);
    }

    /**
     * Registers a subscriber that blocks the current thread until the next
     * event has been received. Returns an empty result if the specified
     * duration (in milliseconds) has been reached without receiving an event.
     *
     * @throws IllegalArgumentException if {@code timeout} is negative or zero.
     */
    public Optional<T> block(long timeout) {
        Preconditions.checkArgument(timeout > 0L, "Invalid timeout: " + timeout);

        BlockingQueue<T> queue = new ArrayBlockingQueue<>(1);
        subscribe(queue::add);
        try {
            T result = queue.poll(timeout, TimeUnit.MILLISECONDS);
            return Optional.ofNullable(result);
        } catch (InterruptedException e) {
            LOGGER.warning("Thread was interrupted awaiting asynchronous event");
            return Optional.empty();
        }
    }

    /**
     * Marks this {@link Subject} as completed, meaning that no new events
     * or errors will be published to subscribers.
     */
    public void complete() {
        if (!completed) {
            completed = true;

            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onComplete();
            }
        }
    }

    /**
     * Returns a {@link Subject} that forwards events to its own subscribers,
     * first applying the specified mapper function to each event. Intended
     * as the equivalent of {@link Stream#map(Function)} for asynchronous
     * events.
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
     * Returns a {@link Subject} that forwards events to its own subscribers,
     * first applying the specified mapper function to each event. Intended
     * as the equivalent of {@link Stream#flatMap(Function)} for asynchronous
     * events.
     */
    public <S> Subject<S> flatMap(Function<T, Stream<S>> mapper) {
        Subject<S> mapped = new Subject<>();

        Consumer<T> onEvent = event -> {
            try {
                mapper.apply(event).forEach(mapped::next);
            } catch (Exception e) {
                mapped.nextError(e);
            }
        };

        subscribe(onEvent, mapped::nextError);

        return mapped;
    }

    /**
     * Returns a {@link Subject} that forwards events to its own subscribers,
     * though only events that match the specified predicate. Intended as the
     * equivalent of {@link Stream#filter(Predicate)} for asynchronous events.
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
     * Creates a {@link Subject} that will immediately publish the specified
     * values to its subscribers.
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
     * Creates a {@link Subject} that will immediately publish the specified
     * values to its subscribers.
     */
    public static <T> Subject<T> of(Iterable<T> values) {
        Subject<T> subject = new Subject<>();
        for (T value : values) {
            subject.next(value);
        }
        return subject;
    }

    /**
     * Performs the specified operation in the current thread, returns a
     * {@link Subject} that can be used to subscribe to its results.
     */
    public static <T> Subject<T> run(Callable<T> operation) {
        Subject<T> subject = new Subject<>();
        subject.next(operation);
        return subject;
    }

    /**
     * Performs the specified operation in a new thread, returns a
     * {@link Subject} that can be used to subscribe to its results.
     */
    public static <T> Subject<T> runAsync(Callable<T> operation) {
        Subject<T> subject = new Subject<>();

        Thread backgroundThread = new Thread(() -> subject.next(operation),
            "Subject-" + UUID.randomUUID());
        backgroundThread.start();

        return subject;
    }

    /**
     * Performs the specified operation, automatically retrying multiple times
     * until the operation succeeds. Returns a {@link Subject} that can be used
     * to subscribe to the results.
     * <p>
     * Note that intermediate failures will <em>not</em> be propagated to
     * subscribers. If the operation does not succeed within the requested
     * number of attempts, the last failure is sent to subscribers.
     *
     * @param attempts The number of attempts before considering the operation
     *                 as failed.
     *
     * @throws IllegalArgumentException if {@code attempts} is less than 2. A
     *         value of 1 is not allowed as that effectively means there is no
     *         retry. In that situation, use {@link #runAsync(Callable)}
     *         instead.
     */
    public static <T> Subject<T> retry(Callable<T> operation, int attempts) {
        Subject<T> subject = new Subject<>();
        retryOperation(operation, attempts, subject);
        return subject;
    }

    /**
     * Performs the specified operation in a new thread, automatically retrying
     * multiple times until the operation succeeds. Returns a {@link Subject}
     * that can be used to subscribe to the results.
     * <p>
     * Note that intermediate failures will <em>not</em> be propagated to
     * subscribers. If the operation does not succeed within the requested
     * number of attempts, the last failure is sent to subscribers.
     *
     * @param attempts The number of attempts before considering the operation
     *                 as failed.
     *
     * @throws IllegalArgumentException if {@code attempts} is less than 2. A
     *         value of 1 is not allowed as that effectively means there is no
     *         retry. In that situation, use {@link #runAsync(Callable)}
     *         instead.
     */
    public static <T> Subject<T> retryAsync(Callable<T> operation, int attempts) {
        Subject<T> subject = new Subject<>();
        Thread backgroundThread = new Thread(() -> retryOperation(operation, attempts, subject),
            "Subject-" + UUID.randomUUID());
        backgroundThread.start();
        return subject;
    }

    private static <T> void retryOperation(Callable<T> operation, int attempts, Subject<T> subject) {
        Preconditions.checkArgument(attempts >= 2, "Invalid number of attempts: " + attempts);

        Exception lastError = null;

        for (int i = 0; i < attempts; i++) {
            try {
                T result = operation.call();
                subject.next(result);
                return;
            } catch (Exception e) {
                LOGGER.warning("Asynchronous operation failed, retrying");
                lastError = e;
            }
        }

        subject.nextError(lastError);
    }

    /**
     * Implementation of the {@link Subscriber} interface that uses callback
     * methods to process incoming events and errors.
     */
    @AllArgsConstructor
    private static class CallbackSubscriber<T> implements Subscriber<T> {

        private Consumer<T> onEvent;
        private Consumer<Throwable> onError;
        private Runnable onComplete;

        @Override
        public void onSubscribe(Subscription subscription) {
        }

        @Override
        public void onNext(T event) {
            onEvent.accept(event);
        }

        @Override
        public void onError(Throwable error) {
            onError.accept(error);
        }

        @Override
        public void onComplete() {
            onComplete.run();
        }
    }
}
