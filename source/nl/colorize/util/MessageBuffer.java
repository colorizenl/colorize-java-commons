//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Data structure that acts as a communication channel between a message producer
 * and consumer that operate at different intervals. The producer adds messages
 * to the buffer as they come in. The consumer then periodically flushes the
 * message buffer to process the received messages.
 * <p>
 * This class is thread safe, the producer and consumer can access the message
 * buffer when operating from different threads.
 *
 * @param <T> The type of message that is stored by this message buffer.
 */
public class MessageBuffer<T> {

    private List<T> messages;

    public MessageBuffer() {
        this.messages = new ArrayList<>();
    }

    public synchronized void add(T message) {
        messages.add(message);
    }

    /**
     * Clears the message buffer and returns all messages that have been added
     * since the last time this method was called.
     */
    public synchronized List<T> flush() {
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> contents = ImmutableList.copyOf(messages);
        messages.clear();
        return contents;
    }

    /**
     * Calls {@link #flush()} and operates the provided callback on every
     * received message.
     */
    public void flush(Consumer<T> callback) {
        flush().forEach(callback);
    }

    public synchronized void reset() {
        messages.clear();
    }
}
