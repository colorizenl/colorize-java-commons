//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.Serializable;

/**
 * An interval between two integers. Ranges represented by this class are both
 * left-bounded and right-bounded, meaning both the start and end values are
 * included in the range.
 */
public final class Range implements Comparable<Range>, Serializable {

	private int start;
	private int end;
	
	private static final long serialVersionUID = 5;
	
	/**
	 * Creates a range from all integers between {@code start} (inclusive) and 
	 * {@code end} (also inclusive).
	 * @throws IllegalArgumentException if {@code end} is less than {@code start}.
	 */
	public Range(int start, int end) {
		if (end < start) {
			throw new IllegalArgumentException("Invalid range: " + start + ".." + end);
		}
		
		this.start = start;
		this.end = end;
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
	
	public int length() {
		return end - start + 1;
	}
	
	/**
	 * Returns true if {@code n} is included in this range.
	 */
	public boolean contains(int n) {
		return (n >= start) && (n <= end);
	}
	
	/**
	 * Returns true when all values in {@code r} are also included within this
	 * range.
	 */
	public boolean contains(Range r) {
		return (r.start >= start) && (r.end <= end);
	}
	
	/**
	 * Returns true if {@code r} and this range intersect. This is true when
	 * there is at least one value that is include in both {@code r} and this
	 * range.
	 */
	public boolean intersects(Range r) {
		return (r.start <= end) && (r.end >= start);
	}
	
	/**
	 * Returns the smallest possible range that includes all values from this
	 * range and all values from {@code r}.
	 */
	public Range span(Range r) {
		return new Range(Math.min(start, r.start), Math.max(end, r.end));
	}
	
	/**
	 * Returns a range that contains all values from this range, except the ones
	 * that are end points.
	 * @throws IllegalStateException if this range contains only one or two
	 *         integers, and therefore an interior range cannot be created.
	 */
	public Range interior() {
		if (length() <= 2) {
			throw new IllegalStateException("Range has no interior: " + this);
		}
		return new Range(start + 1, end - 1);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Range) {
			Range other = (Range) o;
			return (start == other.start) && (end == other.end);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return start * 1000000 + end;
	}
	
	@Override
	public String toString() {
		if (start == end) {
			return String.valueOf(start);
		}
		return start + ".." + end;
	}
	
	public int compareTo(Range other) {
		int startSignum = start - other.start;
		int endSignum = end - other.end;
		return startSignum != 0 ? startSignum : endSignum;
	}
}
