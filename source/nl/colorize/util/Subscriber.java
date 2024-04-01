//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.logging.Logger;

/**
 * Interface for subscribing to (possibly asynchronous) events that are
 * published by a {@link Subscribable}.
 *
 * @param <T> The type of event that can be subscribed to.
 */
@FunctionalInterface
public interface Subscriber<T> {

    /**
     * Invoked by the {@link Subscribable} when an event has been received.
     */
    public void onEvent(T event);

    /**
     * Invoked by the {@link Subscribable} when an error has occurred while
     * publishing an event. This is an optional method, the default
     * implementation will log the error.
     */
    default void onError(Exception error) {
        Logger logger = LogHelper.getLogger(getClass());
        logger.warning("Sunscriber:");
    }

    /**
     * Invoked when the {@link Subscribable} is marked as completed, after
     * which this {@link Subscriber} will no longer receive events. This is
     * an optional method, the default implementation does nothing.
     */
    default void onComplete() {
    }
}
