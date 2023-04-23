//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Objects;

/**
 * Data structure that consists of two ordered values, sometimes also referred
 * to as a pair. Tuples are immutable and may contain {@code null} values. 
 *
 * @param <L> Type of the first (left) element.
 * @param <R> Type of the second (right) element.
 */
public record Tuple<L, R>(L left, R right) {

    /**
     * Returns true if {@code element} is one of this tuple's elements.
     */
    public boolean contains(Object element) {
        return Objects.equal(left, element) || Objects.equal(right, element);
    }
    
    /**
     * Returns a new tuple with the inverse of this tuple's elements.
     */
    public Tuple<R, L> inverse() {
        return new Tuple<>(right, left);
    }
    
    /**
     * Returns a new tuple {@code (newLeft, getRight())}.
     */
    public Tuple<L, R> withLeft(L newLeft) {
        return new Tuple<>(newLeft, right);
    }
    
    /**
     * Returns a new tuple {@code (getLeft(), newRight)}.
     */
    public Tuple<L, R> withRight(R newRight) {
        return new Tuple<>(left, newRight);
    }

    /**
     * Returns the string representation of this tuple. The returned string is
     * in the format "(X, Y)", where X and Y are determined by the string
     * representation of the elements of this tuple.
     */
    @Override
    public String toString() {
        return String.format("(%s, %s)", left, right);
    }
    
    public static <L, R> Tuple<L, R> of(L left, R right) {
        return new Tuple<>(left, right);
    }
}