//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
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

    private List<Integer> cachedList;

    /**
     * Creates a range with all integers between {@code start} (inclusive) and 
     * {@code end} (also inclusive). If {@code start} and {@code end} are equal
     * the range will consist of a single number.
     *
     * @throws IllegalArgumentException if the value for {@code start} is greater
     *         than the value for {@code end}.
     */
    public Range(int start, int end) {
        Preconditions.checkArgument(end >= start,
            "Invalid range: " + start + ".." + end);

        this.start = start;
        this.end = end;

        if (start == end) {
            cachedList = ImmutableList.of(start);
        } else {
            cachedList = ImmutableList.copyOf(IntStream.range(start, end + 1).iterator());
        }
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
     * Returns the index of the specified value within this range. For example,
     * for the range (2..7) the value 2 will have an index of 0, the value 3
     * will have an index of 1, and so on.
     *
     * @throws IllegalArgumentException if the value is not included within
     *         this range.
     */
    public int index(int value) {
        Preconditions.checkArgument(value >= start && value <= end,
            "Value outside of range: " + value);

        return value - start;
    }

    /**
     * Returns a list that contains all integers within this range.
     */
    public List<Integer> toList() {
        return cachedList;
    }
    
    /**
     * Returns an array that contains all integers within this range.
     */
    public int[] toArray() {
        return Ints.toArray(cachedList);
    }

    @Override
    public Iterator<Integer> iterator() {
        return cachedList.iterator();
    }

    public Stream<Integer> stream() {
        return cachedList.stream();
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
        } else {
            return false;
        }
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

    /**
     * Creates a range that is based on the minimum and maximum values from the
     * specified list.
     *
     * @throws IllegalArgumentException if the list is empty.
     */
    public static Range span(List<Integer> list) {
        return span(list.stream().mapToInt(value -> value));
    }

    /**
     * Creates a range that is based on the minimum and maximum values from the
     * specified stream.
     *
     * @throws IllegalArgumentException if the stream is empty.
     */
    public static Range span(IntStream stream) {
        Iterator<Integer> iterator = stream.iterator();
        Preconditions.checkArgument(iterator.hasNext(), "Cannnot create empty range");

        int min = iterator.next();
        int max = min;

        while (iterator.hasNext()) {
            int value = iterator.next();
            min = Math.min(value, min);
            max = Math.max(value, max);
        }

        return new Range(min, max);
    }
}
