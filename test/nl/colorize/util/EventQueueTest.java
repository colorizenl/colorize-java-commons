//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventQueueTest {

    @Test
    void flush() {
        Subject<String> subject = new Subject<>();
        subject.next("a");
        subject.next("b");

        List<String> events = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        EventQueue<String> eventQueue = new EventQueue<>();
        subject.subscribe(eventQueue);
        eventQueue.flush(events::add, errors::add);

        assertEquals(List.of("a", "b"), events);
        assertEquals(0, errors.size());

        subject.next("c");

        assertEquals(List.of("a", "b"), events);
        assertEquals(0, errors.size());

        eventQueue.flush(events::add, errors::add);

        assertEquals(List.of("a", "b", "c"), events);
        assertEquals(0, errors.size());
    }

    @Test
    void clear() {
        Subject<String> subject = new Subject<>();
        subject.next("a");
        subject.next("b");

        List<String> events = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        EventQueue<String> eventQueue = new EventQueue<>();
        subject.subscribe(eventQueue);
        eventQueue.clear();
        subject.next("c");
        eventQueue.flush(events::add, errors::add);

        assertEquals(List.of("c"), events);
        assertEquals(0, errors.size());
    }

    @Test
    void cannotSubscribeToMultiple() {
        Subject<String> subjectA = new Subject<>();
        Subject<String> subjectB = new Subject<>();

        EventQueue<String> eventQueue = new EventQueue<>();
        subjectA.subscribe(eventQueue);

        assertThrows(IllegalStateException.class, () -> subjectB.subscribe(eventQueue));
    }

    @Test
    void fromSubject() {
        Subject<String> subject = new Subject<>();
        subject.next("a");

        List<String> events = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        EventQueue<String> eventQueue = EventQueue.subscribe(subject);
        eventQueue.flush(events::add, errors::add);

        assertEquals(List.of("a"), events);
        assertEquals(0, errors.size());
    }
}
