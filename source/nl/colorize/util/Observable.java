//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Used for publish/subscribe communication between objects without requiring
 * explicit dependencies between publishers and subscribers. Consumers can
 * subscribe to events, and are then notified whenever such an event occurs.
 * <p>
 * The purpose of this class is similar to Guava's {@code EventBus}. However,
 * this class does not use reflection, since that is not (fully) available in
 * some more limited environments.
 *
 * @param <E> The type of event used in the publish/subscribe communication.
 */
public class Observable<E> {

    private List<Consumer<E>> subscribers;

    public Observable() {
        this.subscribers = new ArrayList<>();
    }

    public void subscribe(Consumer<E> subscriber) {
        subscribers.add(0, subscriber);
    }

    public void unsubscribe(Consumer<E> subscriber) {
        subscribers.remove(subscriber);
    }

    public void emit(E event) {
        for (int i = subscribers.size() - 1; i >= 0; i--) {
            subscribers.get(i).accept(event);
        }
    }
}
