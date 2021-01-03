//-----------------------------------------------------------------------------
// Ape Attack
// Copyright 2005-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Acts as a mediator for code expecting {@link Stream}s and code expecting
 * {@link Iterable}s. Streams intentionally do not implement the {@code Iterable}
 * interface, as explained in
 * https://stackoverflow.com/questions/20129762/why-does-streamt-not-implement-iterablet.
 * {@code Iterable} specifies that multiple iterators can be produces, while streams
 * can typically be used only once. However, this is often impractical since there is
 * a lot of existing code, mainly pre-Java 8, that still expects {@link Iterable}s.
 * This class therefore mediates between the two, though obviously it cannot solve
 * the original limitation: it can generate one iterator from a stream, attempting
 * to obtain another iterator afterwards will result in a {@code IllegalStateException}.
 */
public class IterableStream<T> implements Iterable<T> {

    private Stream<T> stream;
    private boolean expired;

    private IterableStream(Stream<T> stream) {
        this.stream = stream;
        this.expired = false;
    }

    @Override
    public Iterator<T> iterator() {
        expire();
        return stream.iterator();
    }

    public Stream<T> stream() {
        expire();
        return stream;
    }

    public List<T> toList() {
        expire();
        return stream.collect(Collectors.toList());
    }

    public Set<T> toSet() {
        expire();
        return stream.collect(Collectors.toSet());
    }

    public long count() {
        expire();
        return stream.count();
    }

    /**
     * Returns the number of elements in the underlying stream.
     * @deprecated Use {@link #count()} instead. The name of this method is too
     *             similar to {@code Collection.count}, but unlike that method
     *             it cannot be called multiple times.
     */
    @Deprecated
    public int size() {
        return (int) count();
    }

    private void expire() {
        Preconditions.checkState(!expired, "Stream has already been iterated has has expired");
        expired = true;
    }

    public static <T> IterableStream<T> wrap(Stream<T> stream) {
        return new IterableStream<T>(stream);
    }

    public static <T> IterableStream<T> wrap(Collection<T> data) {
        return new IterableStream<T>(data.stream());
    }
}
