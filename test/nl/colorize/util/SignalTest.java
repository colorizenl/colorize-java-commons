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

class SignalTest {

    @Test
    void subscribeToChanges() {
        Signal<String> signal = Signal.of("1");
        List<String> events = new ArrayList<>();
        signal.getChanges().subscribe(events::add);
        signal.set("2");

        assertEquals("2", signal.get());
        assertEquals("2", signal.toString());
        assertEquals(List.of("2"), events);
    }

    @Test
    void emit() {
        Signal<String> signal = Signal.emit("1");
        List<String> events = new ArrayList<>();
        signal.getChanges().subscribe(events::add);

        assertEquals("1", signal.get());
        assertEquals(List.of("1"), events);
    }

    @Test
    void updateValue() {
        Signal<Integer> signal = Signal.of(1);
        signal.update(value -> value + 2);

        assertEquals(3, signal.get());
    }

    @Test
    void doNotConsiderChangeIfSameValue() {
        Signal<String> signal = Signal.of("1");
        List<String> events = new ArrayList<>();
        signal.getChanges().subscribe(events::add);

        signal.set("2");
        signal.set("2");
        signal.set(null);
        signal.set(null);
        signal.set("3");

        assertEquals(3, events.size());
        assertEquals("2", events.get(0));
        assertEquals(null, events.get(1));
        assertEquals("3", events.get(2));
    }
}
