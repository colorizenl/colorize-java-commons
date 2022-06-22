//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(ImmutableList.of(2), new Range(2, 2).toList());
    }
    
    @Test
    public void testSize() {
        assertEquals(6, new Range(2, 7).getSize());
        assertEquals(2, new Range(2, 3).getSize());
        assertEquals(1, new Range(2).getSize());
    }

    @Test
    void within() {
        List<Integer> list = ImmutableList.of(2, 3, 4);

        assertEquals("2..4", Range.span(list.stream().mapToInt(e -> e)).toString());
    }

    @Test
    void cannotCreateFromEmptySupplier() {
        assertThrows(IllegalArgumentException.class, () -> {
            Range.span(Collections.emptyList());
        });
    }

    @Test
    void index() {
        Range range = new Range(-2, 4);

        assertEquals(0, range.index(-2));
        assertEquals(1, range.index(-1));
        assertEquals(2, range.index(0));
        assertEquals(3, range.index(1));
        assertEquals(4, range.index(2));
        assertEquals(5, range.index(3));
        assertEquals(6, range.index(4));
    }
}
