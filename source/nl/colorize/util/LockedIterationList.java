//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.ForwardingList;

/**
 * List that delays modifications made while the list is being iterated until
 * after that iteration is complete. This behavior is different from the standard
 * list implementations, which either (a) cannot be modified during iteration
 * (i.e. {@code ArrayList}), (b) are thread-safe, or (c) copy the underlying data 
 * for every modification (i.e. {@code CopyOnwriteArrayList}). 
 * <p>
 * Note that even though this list supports concurrent modification it is 
 * <em>not</em> thread safe.
 */
public class LockedIterationList<E> extends ForwardingList<E> {
	
	private List<E> elements;
	private List<E> snapshot;
	
	public LockedIterationList() {
		elements = new ArrayList<E>();
		snapshot = new CopyOnWriteArrayList<E>();
	}
	
	@Override
	protected List<E> delegate() {
		return elements;
	}
	
	@Override
	public Iterator<E> iterator() {
		snapshot.clear();
		snapshot.addAll(elements);
		return new ImmutableSnapshotIterator<E>(snapshot.iterator());
	}
	
	/**
	 * Iterates over a snapshot of the list's contents. The snapshot is immutable,
	 * which means that this iterator does not support {@link #remove()}.
	 */
	private static class ImmutableSnapshotIterator<E> extends ForwardingIterator<E> {
		
		private Iterator<E> delegate;
		
		public ImmutableSnapshotIterator(Iterator<E> delegate) {
			this.delegate = delegate;
		}

		@Override
		protected Iterator<E> delegate() {
			return delegate;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
