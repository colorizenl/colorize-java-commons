//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ForwardingList;

/**
 * A set where all elements are tuples with the same types. Even though a relation
 * is a set it behaves like a {@link java.util.List}, allowing duplicate elements
 * and maintaining the order in which elements were inserted. 
 * @param <L> Type of the tuples' first (left) element.
 * @param <R> Type of the tuples' second (right) element.
 */
public class Relation<L, R> extends ForwardingList<Tuple<L, R>> implements Serializable {
    
    private List<Tuple<L, R>> tuples;
    
    private static final long serialVersionUID = 3;
    
    /**
     * Creates a relation that is based on the provided underlying list.
     */
    protected Relation(List<Tuple<L, R>> tuples) {
        this.tuples = tuples;
    }
    
    /**
     * Creates a relation that is based on a {@link java.util.ArrayList}.
     */
    public Relation() {
        this.tuples = new ArrayList<Tuple<L, R>>();
    }

    @Override
    protected List<Tuple<L, R>> delegate() {
        return tuples;
    }
    
    public void add(L left, R right) {
        Tuple<L, R> element = Tuple.of(left, right);
        tuples.add(element);
    }
    
    /**
     * Adds all elements from a map to this relation. The map's keys will be
     * used as the domain, and the maps values as the range.
     */
    public void addAll(Map<L, R> pairs) {
        for (Map.Entry<L, R> entry : pairs.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Returns a relation where each tuple has its left and right elements swapped.
     */
    public Relation<R, L> inverse() {
        List<Tuple<R, L>> inversedTuples = new ArrayList<Tuple<R, L>>();
        for (Tuple<L, R> tuple : tuples) {
            inversedTuples.add(tuple.inverse());
        }
        return new Relation<R, L>(inversedTuples);
    }
    
    /**
     * Returns a set containing the left element of all tuples in this relation.
     */
    public Set<L> domain() {
        return new LinkedHashSet<L>(domainList());
    }
    
    /**
     * Returns a list containing the left element of all tuples in this relation.
     */
    public List<L> domainList() {
        List<L> domain = new ArrayList<L>();
        for (Tuple<L, R> tuple : tuples) {
            domain.add(tuple.getLeft());
        }
        return domain;
    }
    
    /**
     * Returns a set containing the right element of all tuples in this relation.
     */
    public Set<R> range() {
        return new LinkedHashSet<R>(rangeList());
    }
    
    /**
     * Returns a list containing the right element of all tuples in this relation.
     */
    public List<R> rangeList() {
        List<R> range = new ArrayList<R>();
        for (Tuple<L, R> tuple : tuples) {
            range.add(tuple.getRight());
        }
        return range;
    }
    
    public Tuple<L, R> first() {
        return tuples.isEmpty() ? null : tuples.get(0);
    }
    
    public Tuple<L, R> last() {
        return tuples.isEmpty() ? null : tuples.get(tuples.size() - 1);
    }
    
    public Tuple<L, R> removeFirst() {
        if (tuples.isEmpty()) {
            return null;
        }
        Tuple<L, R> first = tuples.get(0);
        tuples.remove(0);
        return first;
    }
    
    public Tuple<L, R> removeLast() {
        if (tuples.isEmpty()) {
            return null;
        }
        Tuple<L, R> last = tuples.get(tuples.size() - 1);
        tuples.remove(tuples.size() - 1);
        return last;
    }
    
    /**
     * Returns the first tuple that has a left element equal to {@code needle},
     * or {@code null} when no match was found.
     */
    public Tuple<L, R> findInDomain(L needle) {
        for (Tuple<L, R> tuple : tuples) {
            if (tuple.getLeft().equals(needle)) {
                return tuple;
            }
        }
        return null;
    }
    
    /**
     * Returns the first tuple that has a right element equal to {@code needle},
     * or {@code null} when no match was found.
     */
    public Tuple<L, R> findInRange(R needle) {
        for (Tuple<L, R> tuple : tuples) {
            if (tuple.getRight().equals(needle)) {
                return tuple;
            }
        }
        return null;
    }
    
    /**
     * Factory method for creating relations that prevents having to type the 
     * generic types twice pre-Java 7.
     */
    public static <L, R> Relation<L, R> of() {
        return new Relation<L, R>();
    }
    
    /**
     * Creates a relation that is based on the specified list. The relation is
     * a view, any changes to its contents will be made to the underlying list. 
     */
    public static <L, R> Relation<L, R> from(List<Tuple<L, R>> underlyingList) {
        return new Relation<L, R>(underlyingList);
    }
    
    /**
     * Creates a relation from an existing map. The elements in the relation
     * will be equal to the key/value pairs in the map. The created relation
     * is <em>not</em> a view of the underlying map, changes to the map will
     * therefore not be reflected in the relation.
     */
    public static <L, R> Relation<L, R> fromMap(Map<L, R> map) {
        Relation<L, R> relation = Relation.of();
        for (Map.Entry<L, R> entry : map.entrySet()) {
            relation.add(Tuple.of(entry.getKey(), entry.getValue()));
        }
        return relation;
    }
}
