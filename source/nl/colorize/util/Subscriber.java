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
 * <p>
 * This class is part of a portable framework for
 * <a href="https://en.wikipedia.org/wiki/Publish-subscribe_pattern">publish/subscribe</a>
 * communication. This framework can be used across different platforms,
 * including platforms where {@code java.util.concurrent} is not available,
 * such as <a href="https://teavm.org">TeaVM</a>.
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
        logger.warning("Unhandled subscriber error: " + error.getMessage());
    }

    /**
     * Invoked when the {@link Subscribable} is marked as completed, after
     * which this {@link Subscriber} will no longer receive events. This is
     * an optional method, the default implementation does nothing.
     */
    default void onComplete() {
    }
}
