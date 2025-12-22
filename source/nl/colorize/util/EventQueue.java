//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

/**
 * Subscribes to asynchronous events and accumulates all received events and
 * errors in queue, so that they can be processed at a later time.
 * <p>
 * Instances of this class are thread-safe: Events can be received and
 * processed on different threads.
 *
 * @param <T> The type of event that can be subscribed to.
 * @see Subject
 */
public class EventQueue<T> implements Subscriber<T> {

    private Subscription subscription;
    private List<Object> received;

    public EventQueue() {
        this.received = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onSubscribe(Subscription newSubscription) {
        Preconditions.checkState(subscription == null, "Event queue is already subscribed");
        this.subscription = newSubscription;
    }

    @Override
    public void onNext(T event) {
        received.add(event);
    }

    @Override
    public void onError(Throwable error) {
        received.add(error);
    }

    @Override
    public void onComplete() {
        subscription = null;
        received.clear();
    }

    /**
     * Flushes the event queue, invoking the specified callback functions for
     * all events and errors that have been received since the last time the
     * event queue was flushed. Events and errors are processed in the order
     * they were received.
     */
    @SuppressWarnings("unchecked")
    public void flush(Consumer<T> onEvent, Consumer<Exception> onError) {
        List<Object> snapshot = List.copyOf(received);
        received.clear();

        for (Object event : snapshot) {
            if (event instanceof Exception error) {
                onError.accept(error);
            } else {
                onEvent.accept((T) event);
            }
        }
    }

    /**
     * Clears this event queue, without processing the events and errors that
     * are currently in it.
     */
    public void clear() {
        received.clear();
    }

    /**
     * Factory method that creates an {@link EventQueue} and immediately
     * subscribes it to the specified {@link Subject}.
     */
    public static <T> EventQueue<T> subscribe(Subject<T> subject) {
        EventQueue<T> eventQueue = new EventQueue<>();
        subject.subscribe(eventQueue);
        return eventQueue;
    }
}
