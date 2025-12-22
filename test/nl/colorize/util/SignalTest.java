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
        assertEquals(List.of("1", "2"), events);
    }
}
