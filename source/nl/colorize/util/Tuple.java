//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Objects;

import java.util.Map;

/**
 * Data structure that consists of two ordered values, sometimes also referred
 * to as a pair. Tuples are immutable and may contain {@code null} values. 
 *
 * @param <L> Type of the first (left) element.
 * @param <R> Type of the second (right) element.
 */
public class Tuple<L, R> implements Map.Entry<L, R> {
    
    private L left;
    private R right;

    public Tuple(L left, R right) {
        this.left = left;
        this.right = right;
    }
    
    public L getLeft() {
        return left;
    }
    
    public R getRight() {
        return right;
    }

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }

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
        return new Tuple<R, L>(right, left);
    }
    
    /**
     * Returns a new tuple {@code (newLeft, this.getRight())}.
     */
    public Tuple<L, R> withLeft(L newLeft) {
        return new Tuple<L, R>(newLeft, right);
    }
    
    /**
     * Returns a new tuple {@code (this.getLeft(), newRight)}.
     */
    public Tuple<L, R> withRight(R newRight) {
        return new Tuple<L, R>(left, newRight);
    }
    
    /**
     * Returns if this tuple is equal to {@code o}. Two tuples are considered
     * equal if {@link java.lang.Object#equals(Object)} returns true for both
     * of their elements, and if the elements are in the same order.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Tuple) {
            Tuple<?, ?> other = (Tuple<?, ?>) o;
            return Objects.equal(left, other.left) && Objects.equal(right, other.right);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(left, right);
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
        return new Tuple<L, R>(left, right);
    }
}
