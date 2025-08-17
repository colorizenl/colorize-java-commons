//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
     * Adds a tuple to the list, then returns this {@link TupleList} instance.
     * This method is similar to {@link #add(Object, Object)} but can be used
     * for method chaining.
     */
    public TupleList<L, R> append(L left, R right) {
        add(left, right);
        return this;
    }

    /**
     * Returns a list consisting of the left element of every tuple.
     */
    public List<L> getLeft() {
        return tuples.stream()
            .map(Tuple::left)
            .toList();
    }

    /**
     * Returns a list consisting of the right element of every tuple.
     */
    public List<R> getRight() {
        return tuples.stream()
            .map(Tuple::right)
            .toList();
    }

    public void forEach(BiConsumer<L, R> callback) {
        for (Tuple<L, R> entry : tuples) {
            callback.accept(entry.left(), entry.right());
        }
    }

    /**
     * Returns a new {@link TupleList} where every tuple is the inverse of the
     * tuples in this list. Note the order of the tuples <em>within</em> the
     * list will not change, this will only affect the tuples themselves.
     */
    public TupleList<R, L> inverse() {
        List<Tuple<R, L>> inverseTuples = tuples.stream()
            .map(Tuple::inverse)
            .toList();

        return new TupleList<>(inverseTuples);
    }

    /**
     * Returns a new {@link TupleList} that will contain all tuples from this
     * list, followed by all tuples from {@code other}. This method is
     * different from {@link #addAll(Collection)} in that it does <em>not</em>
     * modify the current list, and instead always creates and returns a new
     * {@link TupleList} instance.
     */
    public TupleList<L, R> concat(TupleList<L, R> other) {
        TupleList<L, R> result = new TupleList<>();
        result.addAll(this);
        result.addAll(other);
        return result;
    }

    public <L2, R2> TupleList<L2, R2> map(Function<L, L2> leftMapper, Function<R, R2> rightMapper) {
        List<Tuple<L2, R2>> mappedTuples = tuples.stream()
            .map(tuple -> tuple.map(leftMapper, rightMapper))
            .toList();

        return new TupleList<>(mappedTuples);
    }

    /**
     * Creates a new {@code TupleList} that contains the same elements as this
     * one, but does not allow modification. Attempting to modify the contents
     * will result in an {@link UnsupportedOperationException}.
     */
    public TupleList<L, R> immutable() {
        return new TupleList<>(List.copyOf(tuples));
    }

    /**
     * Factory method that creates a {@link TupleList} that is mutable and
     * initially empty.
     */
    public static <L, R> TupleList<L, R> create() {
        return new TupleList<>();
    }

    public static <L, R> TupleList<L, R> of(L left, R right) {
        TupleList<L, R> result = TupleList.create();
        result.add(left, right);
        return result;
    }

    public static <L, R> TupleList<L, R> of(L leftA, R rightA, L leftB, R rightB) {
        TupleList<L, R> result = TupleList.create();
        result.add(leftA, rightA);
        result.add(leftB, rightB);
        return result;
    }

    public static <L, R> TupleList<L, R> of(L leftA, R rightA, L leftB, R rightB, L leftC, R rightC) {
        TupleList<L, R> result = TupleList.create();
        result.add(leftA, rightA);
        result.add(leftB, rightB);
        result.add(leftC, rightC);
        return result;
    }

    /**
     * Factory method that creates a mutable {@link TupleList} from an
     * existing list of tuples.
     */
    public static <L, R> TupleList<L, R> copyOf(List<Tuple<L, R>> entries) {
        return new TupleList<>(entries);
    }

    /**
     * Factory method that creates a mutable {@link TupleList} from an
     * existing stream of tuples.
     */
    public static <L, R> TupleList<L, R> fromStream(Stream<Tuple<L, R>> tuples) {
        return new TupleList<>(tuples.toList());
    }

    /**
     * Factory method that creates a mutable {@link TupleList} from a map.
     * The map keys will act as the left element in each tuple, with the
     * corresponding values acting as the right element.
     */
    public static <L, R> TupleList<L, R> fromMap(Map<L, R> values) {
        List<Tuple<L, R>> tuples = values.entrySet().stream()
            .map(entry -> Tuple.of(entry.getKey(), entry.getValue()))
            .toList();

        return new TupleList<>(tuples);
    }

    /**
     * Creates a {@code TupleList} by combining two lists. Each tuple in the
     * result will consist of an element from each list.
     *
     * @throws IllegalArgumentException if the two lists do not have the same
     *         length.
     */
    public static <L, R> TupleList<L, R> combine(List<L> leftEntries, List<R> rightEntries) {
        Preconditions.checkArgument(leftEntries.size() == rightEntries.size(),
            "Lists have different length: " + leftEntries.size() + " versus" + rightEntries.size());

        TupleList<L, R> tuples = create();

        for (int i = 0; i < leftEntries.size(); i++) {
            L left = leftEntries.get(i);
            R right = rightEntries.get(i);
            tuples.add(left, right);
        }

        return tuples;
    }

    /**
     * Creates a new {@code TupleList} that is initially empty, and will throw
     * an {@link UnsupportedOperationException} when trying to modify it.
     */
    public static <L, R> TupleList<L, R> empty() {
        return new TupleList<>(Collections.emptyList());
    }
}
