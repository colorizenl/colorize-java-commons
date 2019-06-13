//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

/**
 * Unit tests for the {@code Tuple} and associated {@code UnorderedTuple} and 
 * {@code Relation} classes.
 */
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
        assertTrue(tuple.equals(new Tuple<String, String>("first", "second")));
        assertFalse(tuple.equals(new Tuple<String, String>("first", "third")));
        assertFalse(tuple.equals(new Tuple<String, String>("second", "first")));
        assertTrue(tuple.hashCode() == new Tuple<String, String>("first", "second").hashCode());
        assertFalse(tuple.hashCode() == new Tuple<String, String>("second", "first").hashCode());
        assertFalse(tuple.hashCode() == new Tuple<String, String>("first", "third").hashCode());
    }
    
    @Test
    public void testRelation() {
        Relation<String, Integer> rel = new Relation<String, Integer>();
        rel.add(Tuple.of("a", 2));
        rel.add(Tuple.of("b", 1));
        rel.add(Tuple.of("a", 9));
        assertEquals("[(a, 2), (b, 1), (a, 9)]", rel.toString());
        assertEquals(3, rel.size());
        assertTrue(rel.contains(Tuple.of("a", 2)));
        assertEquals("[(2, a), (1, b), (9, a)]", rel.inverse().toString());
        assertEquals(ImmutableSet.of("a", "b"), rel.domain());
        assertEquals(ImmutableSet.of(2, 1, 9), rel.range());
        assertEquals(Tuple.of("a", 2), rel.findInDomain("a"));
        assertEquals(Tuple.of("a", 9), rel.findInRange(9));
        assertNull(rel.findInRange(11));
        
        rel.addAll(ImmutableMap.of("r", 17, "p", 18));
        assertEquals("[(a, 2), (b, 1), (a, 9), (r, 17), (p, 18)]", rel.toString());
        
        Tuple<String, Integer> first = rel.removeFirst();
        assertEquals(Tuple.of("a", 2), first);
        assertEquals("[(b, 1), (a, 9), (r, 17), (p, 18)]", rel.toString());
        
        Tuple<String, Integer> last = rel.removeLast();
        assertEquals(Tuple.of("p", 18), last);
        assertEquals("[(b, 1), (a, 9), (r, 17)]", rel.toString());
        
        assertEquals("[(a, 1), (b, 2)]", Relation.fromMap(ImmutableMap.of("a", 1, "b", 2)).toString());
    }
}
