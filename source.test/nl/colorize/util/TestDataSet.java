//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@code DataSet} class.
 */
public class TestDataSet {
	
	private static final float EPSILON = 0.001f;

	@Test
	public void testCreateDataSet() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("a", ImmutableMap.of("1", 1, "2", 2));
		dataSet.addDataPoint("a", "3", 3);
		dataSet.addDataPoint("b", "4", 4);
		dataSet.addDataPoint("c", "1", 7);
		
		assertFalse(dataSet.isEmpty());
		assertEquals(3, dataSet.getNumDataPoints());
		assertEquals(ImmutableList.of("a", "b", "c"), dataSet.getRows());
		assertEquals("{a=1, c=7}", dataSet.select("1").toString());
		assertEquals(1, dataSet.getData("a", "1"));
		assertArrayEquals(new double[] { 3 }, dataSet.toValuesArray(dataSet.select("3")), EPSILON);
		assertEquals(ImmutableSet.of("1", "2", "3", "4"), dataSet.getColumns());
		assertTrue(dataSet.containsDataPoint("a", "3"));
		assertFalse(dataSet.containsDataPoint("a", "999"));
		assertTrue(dataSet.containsRow("c"));
		assertFalse(dataSet.containsRow("z"));
		assertTrue(dataSet.containsColumn("1"));
		assertFalse(dataSet.containsColumn("999"));
	}
	
	@Test
	public void testMetadata() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addMetadata("a", ImmutableMap.of("1", "2", "3", "4"));
		dataSet.addMetadata("a", "5", "6");
		dataSet.addMetadata("b", "5", "7");
		
		assertEquals(ImmutableMap.of("1", "2", "3", "4", "5", "6"), dataSet.getMetadata("a"));
		assertEquals("7", dataSet.getMetadata("b", "5"));
	}
	
	@Test
	public void testCustomSelection() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("aa", "", 1);
		dataSet.addDataPoint("ab", "", 2);
		dataSet.addDataPoint("zz", "", 3);
		
		assertEquals(ImmutableSet.of("aa", "ab", "zz"), dataSet.select("").keySet());
		assertEquals(ImmutableSet.of("aa", "ab"), dataSet.select("", new Predicate<String>() {
			public boolean apply(String input) {
				return input.startsWith("a");
			}
		}).keySet());
	}
	
	@Test
	public void testSelectFirst() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("second", "a", 2);
		
		assertEquals(Tuple.of("first", 1), dataSet.selectFirst("a"));
	}
	
	@Test
	public void testSelectLast() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("second", "a", 2);
		
		assertEquals(Tuple.of("second", 2), dataSet.selectLast("a"));
	}
	
	@Test
	public void testSelectExactly() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1.3);
		dataSet.addDataPoint("second", "a", 1.31);
		dataSet.addDataPoint("third", "a", 1.29);
		dataSet.addDataPoint("fourth", "a", 1.30005);
		
		assertEquals(ImmutableMap.of("first", 1.3, "fourth", 1.30005), 
				dataSet.selectExactly("a", 1.3, 0.001));
	}
	
	@Test
	public void testSelectAtLeast() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 11);
		dataSet.addDataPoint("second", "a", 12.0);
		dataSet.addDataPoint("third", "a", 17);
		
		assertEquals(ImmutableMap.of("second", 12.0, "third", 17), dataSet.selectAtLeast("a", 12));
	}
	
	@Test
	public void testSelectAtMost() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 11);
		dataSet.addDataPoint("second", "a", 12.0);
		dataSet.addDataPoint("third", "a", 17);
		
		assertEquals(ImmutableMap.of("first", 11, "second", 12.0), dataSet.selectAtMost("a", 12));
	}
	
	@Test
	public void testCalculateSum() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("first", "b", 2);
		dataSet.addDataPoint("second", "a", 3);
		dataSet.addDataPoint("third", "a", 4);
		
		assertEquals(8f, dataSet.calculateSum("a").floatValue(), EPSILON);
	}
	
	@Test
	public void testCalculateMin() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("second", "a", 17);
		dataSet.addDataPoint("third", "a", 4);
		
		assertEquals("<first, 1.0>", dataSet.calculateMin("a").toString());
	}
	
	@Test
	public void testCalculateMax() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("second", "a", 17);
		dataSet.addDataPoint("third", "a", 4);
		
		assertEquals("<second, 17.0>", dataSet.calculateMax("a").toString());
	}
	
	@Test
	public void testCalculateAverage() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("second", "a", 7);
		dataSet.addDataPoint("third", "a", 11);
		dataSet.addDataPoint("fourth", "a", 3.5);
		dataSet.addDataPoint("fifth", "a", 1.2);
		
		assertEquals(4.74f, dataSet.calculateAverage("a").floatValue(), EPSILON);
	}
	
	@Test
	public void testCalculateMedian() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("second", "a", 7);
		dataSet.addDataPoint("third", "a", 11);
		dataSet.addDataPoint("fourth", "a", 3.5);
		dataSet.addDataPoint("fifth", "a", 1.2);
		
		assertEquals(3.5f, dataSet.calculateMedian("a").floatValue(), EPSILON);
	}
	
	@Test
	public void testCalculateWeightedAverage() { 
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 2);
		dataSet.addDataPoint("first", "b", 1);
		dataSet.addDataPoint("second", "a", 7);
		dataSet.addDataPoint("second", "b", 2);
		
		assertEquals(4.5f, dataSet.calculateAverage("a").floatValue(), EPSILON);
		assertEquals(5.333f, dataSet.calculateWeightedAverage("a", "b").floatValue(), EPSILON);
	}
	
	@Test
	public void testCalculateWeightedAverageWithAllWeightsZero() { 
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 2);
		dataSet.addDataPoint("second", "a", 6);
		dataSet.fillMissing("b", 0);
		
		assertEquals(4f, dataSet.calculateWeightedAverage("a", "b").floatValue(), EPSILON);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testCannotCalculateForEmptyDataSet() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.calculateMax("a");
	}
	
	@Test(expected=IllegalStateException.class)
	public void testCannotCalculateForEmptySubSet() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 2);
		dataSet.calculateMax("b");
	}
	
	@Test
	public void testCustomCalculation() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 7);
		dataSet.addDataPoint("second", "a", 6);
		dataSet.addDataPoint("third", "a", 10);
		
		assertEquals(21, dataSet.calculate("a", new Function<Map<String, Number>, Number>() {
			public Number apply(Map<String, Number> input) {
				return input.size() * 7;
			}
		}));
	}
	
	@Test
	public void testNormalize() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 7);
		dataSet.addDataPoint("second", "a", 6);
		dataSet.addDataPoint("third", "a", 10);
		
		Map<String, Number> selection = dataSet.select("a");
		Map<String, Number> normalized = dataSet.normalize(selection);
		
		assertEquals(ImmutableMap.of("first", 0.7, "second", 0.6, "third", 1.0), normalized);
	}
	
	@Test
	public void testSortOnColumn() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 7);
		dataSet.addDataPoint("second", "a", 11);
		dataSet.addDataPoint("third", "a", 1);
		dataSet.addDataPoint("fourth", "a", null);
		dataSet.sortAscending("a");
		
		assertEquals(ImmutableList.of("third", "first", "second", "fourth"), dataSet.getRows());
	}
	
	@Test
	public void testReverseSortOnColumn() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 7);
		dataSet.addDataPoint("second", "a", 11);
		dataSet.addDataPoint("third", "a", 1);
		dataSet.addDataPoint("fourth", "a", null);
		dataSet.sortDescending("a");
		
		assertEquals(ImmutableList.of("second", "first", "third", "fourth"), dataSet.getRows());
	}
	
	@Test
	public void testFill() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("a", "1", 123);
		dataSet.addDataPoint("a", "2", 123);
		dataSet.addDataPoint("b", "1", 123);
		dataSet.fill("2", 0);
		
		assertEquals(0, dataSet.getData("a", "2"));
		assertEquals(123, dataSet.getData("b", "1"));
		assertEquals(0, dataSet.getData("b", "2"));
	}
	
	@Test
	public void testFillMissing() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("a", "1", 123);
		dataSet.addDataPoint("a", "2", 123);
		dataSet.addDataPoint("b", "1", 123);
		dataSet.fillMissing("2", 0);
		
		assertEquals(123, dataSet.getData("a", "2"));
		assertEquals(123, dataSet.getData("b", "1"));
		assertEquals(0, dataSet.getData("b", "2"));
	}
	
	@Test
	public void testCreateCopy() {
		DataSet<String, String> dataSet = new DataSet<>();
		dataSet.addDataPoint("first", "a", 1);
		dataSet.addDataPoint("second", "a", 2);
		
		DataSet<String, String> copy = dataSet.copy();
		copy.removeDataPoint("first");
		
		assertEquals(ImmutableList.of("first", "second"), dataSet.getRows());
		assertEquals(ImmutableList.of("second"), copy.getRows());
	}
}
