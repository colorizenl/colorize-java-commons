//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Consumer;

/**
 * Allows subscribers to register themselves with a publisher that sends
 * (possibly asynchronous) events. This is an extension of the
 * {@link Publisher} interface that supports subscribers based on callback
 * functions, without needing to implement the full {@link Subscriber}
 * interface for every subscriber.
 *
 * @param <T> The type of event that can be subscribed to.
 * @see Subject
 */
public interface Subscribable<T> extends Publisher<T> {

    public void subscribe(Consumer<T> onEvent, Consumer<Throwable> onError, Runnable onComplete);

    public void subscribe(Consumer<T> onEvent, Consumer<Throwable> onError);

    public void subscribe(Consumer<T> onEvent);
}
