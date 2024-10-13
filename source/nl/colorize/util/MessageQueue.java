//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Accumulates messages received from a possibly asynchronous operation in a
 * queue. Messages remain in the queue until they are retrieved by recipients.
 * The message queue is created with a limit. Once this limit has been
 * exceeded, received messages will no longer be added to the queue.
 * <p>
 * Instances of this class are thread-safe, the queue can be accessed from
 * multiple threads. This is in fact the intended use case of this class,
 * with publisher and recipient operating on different threads.
 * <p>
 * This class is part of a portable framework for
 * <a href="https://en.wikipedia.org/wiki/Publish-subscribe_pattern">publish/subscribe</a>
 * communication. This framework can be used across different platforms,
 * including platforms where {@code java.util.concurrent} is not available,
 * such as <a href="https://teavm.org">TeaVM</a>.
 *
 * @param <T> The type of message that can be stored in the queue.
 */
public final class MessageQueue<T> {

    private Queue<T> received;
    private int limit;

    private static final Logger LOGGER = LogHelper.getLogger(MessageQueue.class);

    public MessageQueue() {
        this.limit = -1;

        // This needs to be in a block so this class can be
        // compiled and used from within TeaVM.
        if (Platform.isTeaVM()) {
            received = new LinkedList<>();
        } else {
            received = new ConcurrentLinkedQueue<>();
        }
    }

    /**
     * Limits the queue capacity to the specified limit. Once this limit has
     * been reached, the queue will no longer be able to accept new messages.
     *
     * @throws IllegalStateException if the queue capacity already exceeds the
     *         requested new limit.
     */
    public void limitCapacity(int limit) {
        Preconditions.checkArgument(limit == -1 || limit >= 1, "Invalid limit: " + limit);
        Preconditions.checkState(received.size() <= limit, "Limit already exceeded");
        this.limit = limit;
    }

    /**
     * Offers a new message to the queue. The message will be added to the back
     * of the queue. Returns true if the message was added, returns false if the
     * queue limit has been reached and the message was <em>not</em> added.
     */
    public boolean offer(T message) {
        if (limit == -1 || received.size() < limit) {
            received.add(message);
            return true;
        } else {
            LOGGER.warning("Message queue limit (" + limit + ") exceeded");
            return false;
        }
    }

    /**
     * Removes, then returns the oldest message from the queue. Returns
     * {@code null} if the queue is currently empty.
     */
    public T poll() {
        return received.poll();
    }

    /**
     * Removes, then returns all messages currently in the queue. This is
     * effectively a bulk version of {@link #poll()} that operates on
     * <em>all</em> messages, instead of processing them one by one.
     */
    public Iterable<T> flush() {
        List<T> buffer = List.copyOf(received);
        received.clear();
        return buffer;
    }

    /**
     * Invokes the specified callback function for all messages, then removes
     * all messages from the queue. This is effectively a bulk version of
     * {@link #poll()} that operates on <em>all</em> messages, instead of
     * processing them one by one.
     */
    public void flush(Consumer<T> callback) {
        received.forEach(callback);
        received.clear();
    }

    /**
     * Removes the specified message from the queue, without flushing the
     * remaining queue contents.
     *
     * @deprecated Selectively removing messages goes against the intended
     *             publish/subscribe workflow. Prefer explicitly handling
     *             <em>all</em> received messages, without selectively trying
     *             to modify the queue.
     */
    @Deprecated
    public void remove(T message) {
        received.remove(message);
    }

    public boolean isEmpty() {
        return received.isEmpty();
    }

    /**
     * Factory method that creates a {@link MessageQueue} which is subscribed
     * to events from the specified {@link Subscribable}.
     */
    public static <T> MessageQueue<T> subscribe(Subscribable<T> subscribable) {
        MessageQueue<T> messageQueue = new MessageQueue<>();
        subscribable.subscribe(messageQueue::offer);
        return messageQueue;
    }

    /**
     * Factory method that creates a {@link MessageQueue} which is subscribed
     * to <em>errors</em>> from the specified {@link Subscribable}.
     */
    public static MessageQueue<Exception> subscribeErrors(Subscribable<?> subscribable) {
        MessageQueue<Exception> errorQueue = new MessageQueue<>();
        subscribable.subscribeErrors(errorQueue::offer);
        return errorQueue;
    }
}
