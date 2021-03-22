//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TupleTest {

    @Test
    public void testTuple() {
        Tuple<String,String> tuple = Tuple.of("first", "second");

        assertEquals("first", tuple.getLeft());
        assertEquals("second", tuple.getRight());
        assertEquals("(first, second)", tuple.toString());
        assertEquals("(second, first)", tuple.inverse().toString());
        assertEquals("(123, second)", tuple.withLeft("123").toString());
        assertEquals("(first, 2)", tuple.withRight("2").toString());
    }
    
    @Test
    public void testTupleEqualty() {
        Tuple<String, String> tuple = Tuple.of("first", "second");

        assertTrue(tuple.equals(new Tuple<>("first", "second")));
        assertFalse(tuple.equals(new Tuple<>("first", "third")));
        assertFalse(tuple.equals(new Tuple<>("second", "first")));
        assertTrue(tuple.hashCode() == new Tuple<>("first", "second").hashCode());
        assertFalse(tuple.hashCode() == new Tuple<>("second", "first").hashCode());
        assertFalse(tuple.hashCode() == new Tuple<>("first", "third").hashCode());
    }
}
