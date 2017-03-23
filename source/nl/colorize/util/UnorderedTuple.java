//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * Data structure that consists of two unordered values. Unlike regular 
 * {@link Tuple}s, the order of the elements is not relevant, meaning the
 * tuple (A, B) is considered equal to (B, A). Because of this, both elements
 * are required to be of the same type.
 * @param <T> Type of both elements in the tuple.
 */
public class UnorderedTuple<T> extends Tuple<T, T> implements Serializable {
	
	private static final long serialVersionUID = 1;

	public UnorderedTuple(T left, T right) {
		super(left, right);
	}

	@Override	
	public UnorderedTuple<T> inverse() {
		return this;
	}

	@Override	
	public UnorderedTuple<T> withLeft(T newLeft) {
		return new UnorderedTuple<T>(newLeft, getRight());
	}
	
	/**
	 * Returns a new tuple {@code (this.getLeft(), newRight)}.
	 */
	public UnorderedTuple<T> withRight(T newRight) {
		return new UnorderedTuple<T>(getLeft(), newRight);
	}
	
	/**
	 * Returns if this tuple is equal to {@code o}. Two tuples are considered
	 * equal if {@link java.lang.Object#equals(Object)} returns true for both
	 * of their elements, and if the elements are in the same order.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof UnorderedTuple) {
			UnorderedTuple<?> other = (UnorderedTuple<?>) o;
			return (Objects.equal(getLeft(), other.getLeft()) && 
					Objects.equal(getRight(), other.getRight()) || 
					Objects.equal(getLeft(), other.getRight()) && 
					Objects.equal(getRight(), other.getLeft()));
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		long hash = Objects.hashCode(getLeft()) + Objects.hashCode(getRight());
		if (hash > Integer.MAX_VALUE) {
			hash %= Integer.MAX_VALUE;
		}
		return (int) hash;
	}
	
	public static <T> UnorderedTuple<T> ofUnordered(T left, T right) {
		return new UnorderedTuple<T>(left, right);
	}
}
