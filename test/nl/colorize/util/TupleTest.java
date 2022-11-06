//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TupleTest {

    @Test
    public void testTuple() {
        Tuple<String,String> tuple = Tuple.of("first", "second");

        assertEquals("first", tuple.left());
        assertEquals("second", tuple.right());
        assertEquals("(first, second)", tuple.toString());
        assertEquals("(second, first)", tuple.inverse().toString());
        assertEquals("(123, second)", tuple.withLeft("123").toString());
        assertEquals("(first, 2)", tuple.withRight("2").toString());
    }
    
    @Test
    public void testTupleEqualty() {
        Tuple<String, String> tuple = Tuple.of("first", "second");

        assertEquals(tuple, new Tuple<>("first", "second"));
        assertNotEquals(tuple, new Tuple<>("first", "third"));
        assertNotEquals(tuple, new Tuple<>("second", "first"));
        assertEquals(tuple.hashCode(), new Tuple<>("first", "second").hashCode());
        assertNotEquals(tuple.hashCode(), new Tuple<>("second", "first").hashCode());
        assertNotEquals(tuple.hashCode(), new Tuple<>("first", "third").hashCode());
    }
}
