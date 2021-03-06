//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A set that includes integers between the start and end. Ranges represented by
 * this class are <em>inclusive</em>, meaning that the start and end are both
 * included in the range.
 * <p>
 * This class implements the {@link java.lang.Iterable} interface, so ranges can 
 * be used directly in foreach loops:
 *
 * <pre>
 *     for (Integer i : new Range(1, 4)) {
 *         System.out.println(i); // Prints 1, 2, 3, 4
 *     }
 * </pre>
 */
public final class Range implements Iterable<Integer>, Comparable<Range> {

    private int start;
    private int end;
    private boolean backwards;
    
    private List<Integer> cachedList;

    /**
     * Creates a range with all integers between {@code start} (inclusive) and 
     * {@code end} (also inclusive). If {@code start} and {@code end} are equal
     * the range will consist of a single number.
     */
    public Range(int start, int end) {
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
        this.backwards = end < start;
    }
    
    /**
     * Creates a range that consists of a single integer.
     */
    public Range(int n) {
        this(n, n);
    }
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }
    
    /**
     * Returns the number of integers in this range. For example, the range
     * {@code 3..7} will return 5, and ranges containing a single integer will
     * return 1.
     */
    public int getSize() {
        return Math.max(start, end) - Math.min(start, end) + 1;
    }
    
    /**
     * Returns true if {@code n} is included in this range.
     */
    public boolean contains(int n) {
        return n >= start && n <= end;
    }
    
    /**
     * Returns true if all numbers in the range {@code r} are also included 
     * within this range.
     */
    public boolean contains(Range r) {
        return r.start >= start && r.end <= end;
    }
    
    /**
     * Returns true if at least one number is included in both this range and
     * the range {@code r}.
     */
    public boolean intersects(Range r) {
        return r.start <= end && r.end >= start;
    }
    
    /**
     * Returns a list that contains all integers within this range.
     */
    public List<Integer> toList() {
        if (cachedList == null) {
            List<Integer> values = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                values.add(Integer.valueOf(i));
            }

            if (backwards) {
                Collections.reverse(values);
            }

            cachedList = ImmutableList.copyOf(values);
        }
        
        return cachedList;
    }
    
    /**
     * Returns an array that contains all integers within this range.
     */
    public int[] toArray() {
        List<Integer> list = toList();
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    @Override
    public Iterator<Integer> iterator() {
        return toList().iterator();
    }

    public Stream<Integer> stream() {
        return toList().stream();
    }
    
    /**
     * Returns a new range that contains all values from both this range and 
     * all values from {@code r}. For example, the span of the ranges (1..3)
     * and (2..7) would return (1..7).
     */
    public Range span(Range r) {
        return new Range(Math.min(start, r.start), Math.max(end, r.end));
    }
    
    @Override
    public int compareTo(Range other) {
        int startSignum = start - other.start;
        int endSignum = end - other.end;
        return startSignum != 0 ? startSignum : endSignum;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Range) {
            Range other = (Range) o;
            return start == other.start && end == other.end;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(start, end);
    }
    
    @Override
    public String toString() {
        if (start == end) {
            return String.valueOf(start);
        }
        return start + ".." + end;
    }
}
