//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import nl.colorize.util.Range;
import nl.colorize.util.Relation;
import nl.colorize.util.Tuple;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the data structure and collections classes included in this
 * library: {@code Tuple}, {@code Relation}, {@code Range}, and 
 * {@code LockedIterationList}.
 */
public class TestCollections {

	@Test
	public void testTuple() {
		Tuple<String,String> tuple = Tuple.of("first", "second");
		assertEquals("first", tuple.getLeft());
		assertEquals("second", tuple.getRight());
		assertEquals("<first, second>", tuple.toString());
		assertEquals("<second, first>", tuple.inverse().toString());
		assertEquals("<123, second>", tuple.withLeft("123").toString());
		assertEquals("<first, 2>", tuple.withRight("2").toString());
		assertEquals("<a, 2>", Tuple.fromEntry(
				ImmutableMap.of("a", 2).entrySet().iterator().next()).toString());
	}
	
	@Test
	public void testTupleEqualty() {
		Tuple<String, String> tuple = Tuple.of("first", "second");
		assertTrue(tuple.equals(new Tuple<String, String>("first", "second")));
		assertFalse(tuple.equals(new Tuple<String, String>("first", "third")));
		assertFalse(tuple.equals(new Tuple<String, String>("second", "first")));
		assertFalse(tuple.equals("second"));
		assertTrue(tuple.hashCode() == new Tuple<String, String>("first", "second").hashCode());
		assertFalse(tuple.hashCode() == new Tuple<String, String>("second", "first").hashCode());
		assertFalse(tuple.hashCode() == new Tuple<String, String>("first", "third").hashCode());
	}
	
	@Test
	public void testRelation() {
		Relation<String, Integer> rel = new Relation<String, Integer>();
		rel.add(Tuple.of("a", 2));
		rel.add(Tuple.of("b", 1));
		rel.add(Tuple.of("a", 9));
		assertEquals("[<a, 2>, <b, 1>, <a, 9>]", rel.toString());
		assertEquals(3, rel.size());
		assertTrue(rel.contains(Tuple.of("a", 2)));
		assertEquals("[<2, a>, <1, b>, <9, a>]", rel.inverse().toString());
		assertEquals(ImmutableSet.of("a", "b"), rel.domain());
		assertEquals(ImmutableSet.of(2, 1, 9), rel.range());
		assertEquals(Tuple.of("a", 2), rel.findInDomain("a"));
		assertEquals(Tuple.of("a", 9), rel.findInRange(9));
		assertNull(rel.findInRange(11));
		
		rel.addAll(ImmutableMap.of("r", 17, "p", 18));
		assertEquals("[<a, 2>, <b, 1>, <a, 9>, <r, 17>, <p, 18>]", rel.toString());
		
		Tuple<String, Integer> first = rel.removeFirst();
		assertEquals(Tuple.of("a", 2), first);
		assertEquals("[<b, 1>, <a, 9>, <r, 17>, <p, 18>]", rel.toString());
		
		Tuple<String, Integer> last = rel.removeLast();
		assertEquals(Tuple.of("p", 18), last);
		assertEquals("[<b, 1>, <a, 9>, <r, 17>]", rel.toString());
		
		assertEquals("[<a, 1>, <b, 2>]", Relation.fromMap(ImmutableMap.of("a", 1, "b", 2)).toString());
	}
	
	@Test
	public void testRange() {
		Range range = new Range(2, 6);
		
		assertEquals(2, range.getStart());
		assertEquals(6, range.getEnd());
		assertEquals(5, range.length());
		assertEquals("2..6", range.toString());
		assertTrue(range.contains(2));
		assertTrue(range.contains(3));
		assertTrue(range.contains(6));
		assertFalse(range.contains(7));
		assertTrue(range.contains(2));
		assertFalse(range.contains(7));
		assertArrayEquals(new int[] { 2, 3, 4, 5, 6 }, range.toArray());
		assertEquals(ImmutableList.of(2, 3, 4, 5, 6), range.toList());
	}
	
	@Test
	public void testSingleValueRange() {
		Range single = new Range(2);
		assertEquals(1, single.length());
		assertEquals("2", single.toString());
	}
	
	@Test
	public void testRangeComparator() {
		List<Range> ranges = Arrays.asList(new Range(2, 6), new Range(3, 4), new Range(3, 5),
				new Range(4, 7), new Range(10, 20));
		Collections.sort(ranges);
		assertEquals(new Range(2, 6), ranges.get(0));
		assertEquals(new Range(3, 4), ranges.get(1));
		assertEquals(new Range(3, 5), ranges.get(2));
		assertEquals(new Range(4, 7), ranges.get(3));
		assertEquals(new Range(10, 20), ranges.get(4));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNegativeRangeNotAllowed() {
		new Range(2, 1);
	}
	
	@Test
	public void testRangeContains() {
		assertTrue(new Range(2, 7).contains(new Range(3, 4)));
		assertFalse(new Range(2, 7).contains(new Range(8, 9)));
		assertFalse(new Range(2, 7).contains(new Range(-4, 2)));
		assertFalse(new Range(2, 7).contains(new Range(6, 9)));
		assertTrue(new Range(2, 7).contains(new Range(2, 7)));
		assertTrue(new Range(2, 7).contains(new Range(2, 2)));
	}
	
	@Test
	public void testRangeIntersects() {
		assertTrue(new Range(2, 7).intersects(new Range(3, 4)));
		assertFalse(new Range(2, 7).intersects(new Range(8, 9)));
		assertTrue(new Range(2, 7).intersects(new Range(-4, 2)));
		assertTrue(new Range(2, 7).intersects(new Range(6, 9)));
		assertTrue(new Range(2, 7).intersects(new Range(2, 7)));
		assertTrue(new Range(2, 7).intersects(new Range(2, 2)));
	}
	
	@Test
	public void testRangeSpan() {
		assertEquals(new Range(2, 7), new Range(2, 7).span(new Range(3, 4)));
		assertEquals(new Range(-1, 9), new Range(2, 7).span(new Range(-1, 9)));
		assertEquals(new Range(2, 9), new Range(2, 7).span(new Range(5, 9)));
		assertEquals(new Range(2, 7), new Range(2, 7).span(new Range(2, 2)));
	}
	
	@Test
	public void testRangeInterior() {
		assertEquals(new Range(3, 5), new Range(2, 6).interior());
		assertEquals(new Range(-5, -4), new Range(-6, -3).interior());
	}
	
	@Test
	public void testRangeIterator() {
		Range range = new Range(2, 4);
		Iterator<Integer> iterator = range.iterator();
		
		assertEquals(2, iterator.next().intValue());
		assertEquals(3, iterator.next().intValue());
		assertEquals(4, iterator.next().intValue());
		assertFalse(iterator.hasNext());
	}
}
