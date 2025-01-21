//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.Iterators;
import lombok.Getter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SequencedCollection;
import java.util.stream.Stream;

/**
 * Wraps an existing collection to allow subscribers to be notified whenever
 * elements are added to or removed from the collection.
 *
 * @param <E> The type of element included in this collection.
 */
public class SubscribableCollection<E> extends ForwardingCollection<E> {

    private Collection<E> elements;
    @Getter private Subject<E> addedElements;
    @Getter private Subject<E> removedElements;

    private SubscribableCollection(Collection<E> elements) {
        this.elements = elements;
        this.addedElements = new Subject<>();
        this.removedElements = new Subject<>();
    }

    @Override
    protected Collection<E> delegate() {
        return elements;
    }

    @Override
    public boolean add(E element) {
        boolean result = elements.add(element);
        addedElements.next(element);
        return result;
    }

    @Override
    public boolean remove(Object element) {
        boolean result = elements.remove(element);
        if (result) {
            removedElements.next((E) element);
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> toRemove) {
        boolean result = elements.removeAll(toRemove);
        for (Object element : toRemove) { {
            removedElements.next((E) element);
        }}
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> toRetain) {
        List<E> toRemove = elements.stream()
            .filter(element -> !toRetain.contains(element))
            .toList();

        return removeAll(toRemove);
    }

    @Override
    public void clear() {
        for (E element : elements) {
            removedElements.next(element);
        }
        elements.clear();
    }

    /**
     * Returns an iterator for the elements in the underlying collection. The
     * returned iterator will not support {@link Iterator#remove()}, even if
     * the original collection does allow this.
     */
    @Override
    public Iterator<E> iterator() {
        return Iterators.unmodifiableIterator(elements.iterator());
    }

    public E getFirst() {
        if (elements instanceof SequencedCollection<E> sequence) {
            return sequence.getFirst();
        } else {
            return elements.iterator().next();
        }
    }

    /**
     * Removes all elements from this collection, then returns a stream
     * containing the elements that were just removed.
     */
    public Stream<E> flush() {
        List<E> snapshot = List.copyOf(elements);
        clear();
        return snapshot.stream();
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    /**
     * Returns a new {@link SubscribableCollection} instance that wraps the
     * specified underlying collection.
     */
    public static <E> SubscribableCollection<E> wrap(Collection<E> elements) {
        return new SubscribableCollection<>(elements);
    }
}
