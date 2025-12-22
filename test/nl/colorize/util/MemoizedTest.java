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

class MemoizedTest {

    @Test
    void memoizeValue() {
        List<String> values = new ArrayList<>();
        values.add("one");
        values.add("two");
        values.add("three");

        Memoized<String> memoized = Memoized.compute(values::removeLast);

        assertEquals("three", memoized.get());
        assertEquals("three", memoized.get());
        assertEquals(List.of("one", "two"), values);
    }

    @Test
    void stringRepresentation() {
        Memoized<String> memoized = Memoized.compute(() -> "test");

        assertEquals("<lazy>", memoized.toString());
        assertEquals("test", memoized.get());
        assertEquals("test", memoized.toString());
    }
}
