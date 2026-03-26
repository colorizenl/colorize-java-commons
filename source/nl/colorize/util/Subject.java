//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
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
 * {@link Subject} instances are thread-safe and can be accessed from
 * different threads. This facilitates workflows where publishers and
 * subscribers operate on different threads.
 *
 * @param <T> The type of event that can be subscribed to.
 */
public final class Subject<T> implements Publisher<T> {

    private List<SubjectSubscription> subscriptions;
    private List<Object> undelivered;
    private boolean completed;

    private static final Logger LOGGER = LogHelper.getLogger(Subject.class);

    public Subject() {
        this.completed = false;
        this.subscriptions = new CopyOnWriteArrayList<>();
        this.undelivered = new CopyOnWriteArrayList<>();
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

        if (subscriptions.isEmpty()) {
            undelivered.add(event);
            return;
        }

        for (SubjectSubscription subscription : subscriptions) {
            subscription.subscriber.onNext(event);
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

        if (subscriptions.isEmpty()) {
            undelivered.add(error);
            return;
        }

        for (SubjectSubscription subscription : subscriptions) {
            subscription.subscriber.onError(error);
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

    private Subscription registerSubscription(Subscriber<? super T> subscriber) {
        Preconditions.checkNotNull(subscriber, "Null subscriber");

        SubjectSubscription subscription = new SubjectSubscription(subscriber);
        subscriptions.add(subscription);
        subscriber.onSubscribe(subscription);
        sendUndelivered(subscriber);
        return subscription;
    }

    @SuppressWarnings("unchecked")
    private void sendUndelivered(Subscriber<? super T> subscriber) {
        for (Object undeliveredEvent : undelivered) {
            if (undeliveredEvent instanceof Exception error) {
                subscriber.onError(error);
            } else {
                subscriber.onNext((T) undeliveredEvent);
            }
        }

        undelivered.clear();
    }

    /**
     * Registers the specified callback function as a subscriber for events.
     * The subscriber will log errors, but not explicitly handle them.
     * Returns a {@link Subscription} for the registered subscriber.
     */
    public Subscription subscribe(Consumer<T> onEvent) {
        Consumer<Throwable> onError = e -> LOGGER.log(Level.SEVERE, "Unhandled subscriber error", e);
        Subscriber<T> subscriber = new CallbackSubscriber<>(onEvent, onError);
        return registerSubscription(subscriber);
    }

    /**
     * Registers the specified callback functions as event and error
     * subscribers. If there are currently undelivered events or errors,
     * the new subscriber will immediately be notified of them.
     *
     * @return A {@link Subscription} for the registered subscriber.
     */
    public Subscription subscribe(Consumer<T> onEvent, Consumer<Throwable> onError) {
        Subscriber<T> subscriber = new CallbackSubscriber<>(onEvent, onError);
        return registerSubscription(subscriber);
    }

    /**
     * Registers the specified callback function as an error subscriber. If
     * there are currently undelivered events or errors, the new subscriber
     * will immediately be notified of them.
     *
     * @return A {@link Subscription} for the registered subscriber.
     */
    public Subscription subscribeErrors(Consumer<Throwable> onError) {
        Consumer<T> onEvent = _ -> {};
        Subscriber<T> subscriber = new CallbackSubscriber<>(onEvent, onError);
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
     * Marks this {@link Subject} as completed, meaning that no new events
     * or errors will be published to subscribers.
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
     * Implementation of the {@link Subscription} interface that allows a
     * subscriber to unsubscribe itself.
     */
    @AllArgsConstructor
    private class SubjectSubscription implements Subscription {

        private Subscriber<? super T> subscriber;

        @Override
        public void request(long n) {
            // Subjects already inform subscribers of historic
            // events, so requesting additional events has no
            // meaning in this implementation.
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
