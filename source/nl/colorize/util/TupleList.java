//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convenience class that can be used as a shorthand for creating a list of
 * {@link Tuple}s.
 *
 * @param <L> Type of the first (left) element.
 * @param <R> Type of the second (right) element.
 */
public class TupleList<L, R> extends ForwardingList<Tuple<L, R>> {

    private List<Tuple<L, R>> tuples;

    public TupleList() {
        this.tuples = new ArrayList<>();
    }

    private TupleList(List<Tuple<L, R>> tuples) {
        this.tuples = tuples;
    }

    @Override
    protected List<Tuple<L, R>> delegate() {
        return tuples;
    }

    public void add(L left, R right) {
        tuples.add(Tuple.of(left, right));
    }

    /**
     * Returns a list consisting of the left element of every tuple.
     */
    public List<L> getLeft() {
        return tuples.stream()
            .map(Tuple::getLeft)
            .collect(Collectors.toList());
    }

    /**
     * Returns a list consisting of the right element of every tuple.
     */
    public List<R> getRight() {
        return tuples.stream()
            .map(Tuple::getRight)
            .collect(Collectors.toList());
    }

    public TupleList<R, L> inverse() {
        List<Tuple<R, L>> inverseTuples = tuples.stream()
            .map(Tuple::inverse)
            .collect(Collectors.toList());

        return new TupleList<R, L>(inverseTuples);
    }

    /**
     * Creates a new {@code TupleList} that contains the same elements as this
     * one, but does not allow modification. Attempting to modify the contents
     * will result in an {@link UnsupportedOperationException}.
     */
    public TupleList<L, R> immutable() {
        return new TupleList<>(ImmutableList.copyOf(tuples));
    }

    public static <L, R> TupleList<L, R> create() {
        return new TupleList<>();
    }

    /**
     * Creates a new {@code TupleList} that is initially empty, and will throw
     * an {@link UnsupportedOperationException} when trying to modify it.
     */
    public static <L, R> TupleList<L, R> empty() {
        return new TupleList<>(Collections.emptyList());
    }
}
