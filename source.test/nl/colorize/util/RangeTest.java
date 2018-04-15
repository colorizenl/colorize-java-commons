//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

/**
 * Unit test for the {@code Range} class.
 */
public class RangeTest {

    @Test
    public void testRange() {
        Range range = new Range(2, 6);
        
        assertEquals("2..6", range.toString());
        assertTrue(range.contains(2));
        assertTrue(range.contains(3));
        assertTrue(range.contains(6));
        assertFalse(range.contains(7));
        assertTrue(range.contains(2));
        assertFalse(range.contains(7));
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 }, range.toArray());
        assertEquals(ImmutableList.of(2, 3, 4, 5, 6), range.toList());
    }
    
    @Test
    public void testSingleValueRange() {
        Range single = new Range(2);
        assertEquals("2", single.toString());
        assertEquals(ImmutableList.of(2), single.toList());
    }
    
    @Test
    public void testRangeComparator() {
        List<Range> ranges = Arrays.asList(new Range(2, 6), new Range(3, 4), new Range(3, 5),
                new Range(4, 7), new Range(10, 20));
        Collections.sort(ranges);
        assertEquals(new Range(2, 6), ranges.get(0));
        assertEquals(new Range(3, 4), ranges.get(1));
        assertEquals(new Range(3, 5), ranges.get(2));
        assertEquals(new Range(4, 7), ranges.get(3));
        assertEquals(new Range(10, 20), ranges.get(4));
    }
    
    @Test
    public void testContains() {
        assertTrue(new Range(2, 7).contains(new Range(3, 4)));
        assertFalse(new Range(2, 7).contains(new Range(8, 9)));
        assertFalse(new Range(2, 7).contains(new Range(-4, 2)));
        assertFalse(new Range(2, 7).contains(new Range(6, 9)));
        assertTrue(new Range(2, 7).contains(new Range(2, 7)));
        assertTrue(new Range(2, 7).contains(new Range(2, 2)));
    }
    
    @Test
    public void testIntersects() {
        assertTrue(new Range(2, 7).intersects(new Range(3, 4)));
        assertFalse(new Range(2, 7).intersects(new Range(8, 9)));
        assertTrue(new Range(2, 7).intersects(new Range(-4, 2)));
        assertTrue(new Range(2, 7).intersects(new Range(6, 9)));
        assertTrue(new Range(2, 7).intersects(new Range(2, 7)));
        assertTrue(new Range(2, 7).intersects(new Range(2, 2)));
    }
    
    @Test
    public void testSpan() {
        assertEquals(new Range(2, 7), new Range(2, 7).span(new Range(3, 4)));
        assertEquals(new Range(-1, 9), new Range(2, 7).span(new Range(-1, 9)));
        assertEquals(new Range(2, 9), new Range(2, 7).span(new Range(5, 9)));
        assertEquals(new Range(2, 7), new Range(2, 7).span(new Range(2, 2)));
    }
    
    @Test
    public void testIterator() {
        Range range = new Range(2, 4);
        Iterator<Integer> iterator = range.iterator();
        
        assertEquals(2, iterator.next().intValue());
        assertEquals(3, iterator.next().intValue());
        assertEquals(4, iterator.next().intValue());
        assertFalse(iterator.hasNext());
    }
    
    @Test
    public void testNegativeRange() {
        assertEquals(ImmutableList.of(-6, -5, -4), new Range(-6, -4).toList());
        assertEquals(ImmutableList.of(2, 3, 4), new Range(4, 2).toList());
    }
    
    @Test
    public void testSize() throws Exception {
        assertEquals(6, new Range(2, 7).getSize());
        assertEquals(2, new Range(2, 3).getSize());
        assertEquals(1, new Range(2).getSize());
        assertEquals(3, new Range(4, 2).getSize());
    }
}
