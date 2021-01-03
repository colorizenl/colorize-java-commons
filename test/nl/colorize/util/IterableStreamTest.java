//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IterableStreamTest {

    @Test
    void iterateOverStream() {
        List<String> list = ImmutableList.of("a", "b", "c");
        Iterator<String> iterator = IterableStream.wrap(list.stream()).iterator();

        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("b", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("c", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void iterateOverCollection() {
        List<String> list = ImmutableList.of("a", "b", "c");
        Iterator<String> iterator = IterableStream.wrap(list).iterator();

        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("b", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("c", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void multipleIterationsThrowsException() {
        List<String> list = ImmutableList.of("a", "b", "c");
        IterableStream<String> iterable = IterableStream.wrap(list.stream());
        iterable.iterator();

        assertThrows(IllegalStateException.class, () -> iterable.iterator());
    }

    @Test
    void count() {
        List<String> list = ImmutableList.of("a", "b", "c");
        IterableStream<String> iterable = IterableStream.wrap(list.stream());

        assertEquals(3, iterable.count());
    }
}
