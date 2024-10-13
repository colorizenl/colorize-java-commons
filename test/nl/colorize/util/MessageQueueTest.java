//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageQueueTest {

    @Test
    void pollQueue() {
        MessageQueue<String> queue = new MessageQueue<>();
        queue.offer("a");
        queue.offer("b");

        assertFalse(queue.isEmpty());
        assertEquals("a", queue.poll());
        assertFalse(queue.isEmpty());
        assertEquals("b", queue.poll());
        assertTrue(queue.isEmpty());
        assertNull(queue.poll());
    }

    @Test
    void flushQueue() {
        MessageQueue<String> queue = new MessageQueue<>();
        queue.offer("a");
        queue.offer("b");

        assertEquals("[a, b]", queue.flush().toString());
        assertEquals("[]", queue.flush().toString());
    }

    @Test
    void flushWithCallback() {
        MessageQueue<String> queue = new MessageQueue<>();
        queue.offer("a");
        queue.offer("b");

        List<String> buffer = new ArrayList<>();
        queue.flush(buffer::add);

        assertEquals(List.of("a", "b"), buffer);
        assertTrue(queue.isEmpty());
    }

    @Test
    void enforceLimit() {
        MessageQueue<String> queue = new MessageQueue<>();
        queue.limitCapacity(3);
        queue.offer("a");
        queue.offer("b");
        queue.offer("c");
        queue.offer("d");

        assertEquals("[a, b, c]", queue.flush().toString());
        assertEquals("[]", queue.flush().toString());
    }

    @Test
    void cannotLimitIfCapacityAlreadyExceeded() {
        MessageQueue<String> queue = new MessageQueue<>();
        queue.offer("a");
        queue.offer("b");
        queue.limitCapacity(3);
        queue.offer("c");
        assertThrows(IllegalStateException.class, () -> queue.limitCapacity(2));
    }

    @Test
    void fromSubscribable() {
        Subscribable<String> subscribable = Subscribable.of("a", "b", "c");
        MessageQueue<String> queue = MessageQueue.subscribe(subscribable);

        assertEquals("[a, b, c]", queue.flush().toString());
        assertEquals("[]", queue.flush().toString());

        subscribable.next("d");
        subscribable.next("e");

        assertEquals("[d, e]", queue.flush().toString());
        assertEquals("[]", queue.flush().toString());
    }

    @Test
    void actAsErrorQueue() {
        Subscribable<String> subscribable = new Subscribable<>();
        MessageQueue<Exception> errorQueue = MessageQueue.subscribeErrors(subscribable);

        subscribable.next(() -> "a");
        subscribable.next(() -> {
            throw new RuntimeException();
        });

        assertNotNull(errorQueue.poll());
        assertNull(errorQueue.poll());
    }

    @Test
    void remove() {
        Subscribable<String> subscribable = Subscribable.of("a", "b", "c");
        MessageQueue<String> queue = MessageQueue.subscribe(subscribable);
        queue.remove("b");

        assertEquals("[a, c]", queue.flush().toString());
    }
}
