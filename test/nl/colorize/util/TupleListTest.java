//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TupleListTest {

    @Test
    void getLeftAndRight() {
        TupleList<String, Integer> list = new TupleList<>();
        list.add("a", 2);
        list.add("b", 3);

        assertEquals(ImmutableList.of("a", "b"), list.getLeft());
        assertEquals(ImmutableList.of(2, 3), list.getRight());
    }

    @Test
    void inverse() {
        TupleList<String, Integer> list = new TupleList<>();
        list.add("a", 2);
        list.add("b", 3);

        assertEquals("[(2, a), (3, b)]", list.inverse().toString());
    }

    @Test
    void empty() {
        TupleList<String, Integer> list = TupleList.empty();
        assertThrows(UnsupportedOperationException.class, () -> list.add("b", 2));
    }

    @Test
    void immutable() {
        TupleList<String, Integer> list = new TupleList<>();
        list.add("a", 2);
        TupleList<String, Integer> immutableVersion = list.immutable();
        list.add("b", 3);

        assertEquals("[(a, 2), (b, 3)]", list.toString());
        assertEquals("[(a, 2)]", immutableVersion.toString());
        assertThrows(UnsupportedOperationException.class, () -> immutableVersion.add("c", 4));
    }
}