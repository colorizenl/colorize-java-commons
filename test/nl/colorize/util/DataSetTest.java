//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataSetTest {
    
    private static final float EPSILON = 0.001f;

    @Test
    public void testCreateDataSet() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("a", ImmutableMap.of("1", 1, "2", 2));
        dataSet.add("a", "3", 3);
        dataSet.add("b", "4", 4);
        dataSet.add("c", "1", 7);
        
        assertFalse(dataSet.isEmpty());
        assertEquals(3, dataSet.getNumDataPoints());
        assertEquals(ImmutableList.of("a", "b", "c"), dataSet.getRows());
        assertEquals("{a=1, c=7}", dataSet.select("1").toString());
        assertEquals(1, dataSet.get("a", "1"));
        assertNull(dataSet.get("q", "1"));
        assertEquals(0, dataSet.get("q", "1", 0));
        assertArrayEquals(new double[] { 3 }, dataSet.toValuesArray(dataSet.select("3")), EPSILON);
        assertEquals(ImmutableList.of("1", "2", "3", "4"), dataSet.getColumns());
        assertTrue(dataSet.contains("a", "3"));
        assertFalse(dataSet.contains("a", "999"));
        assertTrue(dataSet.containsRow("c"));
        assertFalse(dataSet.containsRow("z"));
        assertTrue(dataSet.containsColumn("1"));
        assertFalse(dataSet.containsColumn("999"));
    }
    
    @Test
    public void testGetColumn() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("a", "1", 100);
        dataSet.add("b", "2", 200);
        dataSet.add("b", "1", 300);
        dataSet.add("c", "2", 400);
        
        assertEquals(ImmutableList.of("a", "b", "c"), dataSet.getRows());
        assertEquals(ImmutableList.of("1", "2"), dataSet.getColumns());
        assertEquals(ImmutableMap.of("2", 400), dataSet.getRow("c"));
        assertEquals(ImmutableMap.of("a", 100, "b", 300), dataSet.getColumn("1"));
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
        dataSet.add("aa", "", 1);
        dataSet.add("ab", "", 2);
        dataSet.add("zz", "", 3);
        
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
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 2);
        
        assertEquals(Tuple.of("first", 1), dataSet.selectFirst("a"));
    }
    
    @Test
    public void testSelectLast() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 2);
        
        assertEquals(Tuple.of("second", 2), dataSet.selectLast("a"));
    }
    
    @Test
    public void testSelectExactly() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1.3);
        dataSet.add("second", "a", 1.31);
        dataSet.add("third", "a", 1.29);
        dataSet.add("fourth", "a", 1.30005);
        
        assertEquals(ImmutableMap.of("first", 1.3, "fourth", 1.30005), 
                dataSet.selectExactly("a", 1.3, 0.001));
    }
    
    @Test
    public void testSelectAtLeast() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 11);
        dataSet.add("second", "a", 12.0);
        dataSet.add("third", "a", 17);
        
        assertEquals(ImmutableMap.of("second", 12.0, "third", 17), dataSet.selectAtLeast("a", 12));
    }
    
    @Test
    public void testSelectAtMost() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 11);
        dataSet.add("second", "a", 12.0);
        dataSet.add("third", "a", 17);
        
        assertEquals(ImmutableMap.of("first", 11, "second", 12.0), dataSet.selectAtMost("a", 12));
    }
    
    @Test
    public void testCalculateSum() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("first", "b", 2);
        dataSet.add("second", "a", 3);
        dataSet.add("third", "a", 4);
        
        assertEquals(8f, dataSet.calculateSum("a"), EPSILON);
    }
    
    @Test
    public void testCalculateMin() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 17);
        dataSet.add("third", "a", 4);
        
        assertEquals("(first, 1.0)", dataSet.calculateMin("a").toString());
    }
    
    @Test
    public void testCalculateMax() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 17);
        dataSet.add("third", "a", 4);
        
        assertEquals("(second, 17.0)", dataSet.calculateMax("a").toString());
    }
    
    @Test
    public void testCalculateAverage() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 7);
        dataSet.add("third", "a", 11);
        dataSet.add("fourth", "a", 3.5);
        dataSet.add("fifth", "a", 1.2);
        
        assertEquals(4.74f, dataSet.calculateAverage("a"), EPSILON);
    }
    
    @Test
    public void testCalculateAverageWithOneValue() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        
        assertEquals(1f, dataSet.calculateAverage("a"), EPSILON);
    }

    @Test
    public void testAverageWithMultipleIdenticalValues() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 2);
        dataSet.add("third", "a", 2);

        assertEquals(1.667f, dataSet.calculateAverage("a"), EPSILON);
    }

    @Test
    public void testCalculateMedian() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 7);
        dataSet.add("third", "a", 11);
        dataSet.add("fourth", "a", 3.5);
        dataSet.add("fifth", "a", 1.2);
        
        assertEquals(3.5f, dataSet.calculateMedian("a"), EPSILON);
    }
    
    @Test
    public void testCalculateWeightedAverage() { 
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 2);
        dataSet.add("first", "b", 1);
        dataSet.add("second", "a", 7);
        dataSet.add("second", "b", 2);
        
        assertEquals(4.5f, dataSet.calculateAverage("a"), EPSILON);
        assertEquals(5.333f, dataSet.calculateWeightedAverage("a", "b"), EPSILON);
    }
    
    @Test
    public void testCalculateWeightedAverageWithAllWeightsZero() { 
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 2);
        dataSet.add("second", "a", 6);
        dataSet.fillMissing("b", 0);
        
        assertEquals(0f, dataSet.calculateWeightedAverage("a", "b"), EPSILON);
    }
    
    @Test
    public void testCalculateWeightedAverageWithOneValue() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("first", "b", 1);
        
        assertEquals(1f, dataSet.calculateWeightedAverage("a", "b"), EPSILON);
    }
    
    @Test
    public void testCalculatePercentiles() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 2);
        dataSet.add("second", "a", 6);
        dataSet.add("third", "a", 3);
        dataSet.add("fourth", "a", -4);
        dataSet.add("fifth", "a", null);
        
        Map<String, Number> percentiles = dataSet.calculatePercentiles("a");
        
        assertEquals(4, percentiles.size());
        assertEquals(100.0, percentiles.get("second").doubleValue(), EPSILON);
        assertEquals(66.667, percentiles.get("third").doubleValue(), EPSILON);
        assertEquals(33.333, percentiles.get("first").doubleValue(), EPSILON);
        assertEquals(0.0, percentiles.get("fourth").doubleValue(), EPSILON);
    }
    
    @Test
    public void testNoPercentilesForTooSmallAmountOfData() {
        DataSet<String, String> dataSet = new DataSet<>();
        Map<String, Number> percentiles = dataSet.calculatePercentiles("a");
        
        assertTrue(percentiles.isEmpty());
    }
    
    @Test
    public void testCalculateNormalized() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 7);
        dataSet.add("second", "a", 6);
        dataSet.add("third", "a", 10);
        
        Map<String, Number> normalized = dataSet.calculateNormalized("a");
        
        assertEquals(ImmutableMap.of("first", 0.7, "second", 0.6, "third", 1.0), normalized);
    }
    
    @Test
    public void testCannotCalculateForEmptyDataSet() {
        DataSet<String, String> dataSet = new DataSet<>();
        assertThrows(IllegalStateException.class, () -> dataSet.calculateMax("a"));
    }
    
    @Test
    public void testCannotCalculateForEmptySubSet() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 2);
        assertThrows(IllegalStateException.class, () -> dataSet.calculateMax("b"));
    }
    
    @Test
    public void testCalculateForSingleDataPoint() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 2);
        
        assertEquals(2, dataSet.calculateAverage("a"), EPSILON);
        assertEquals(2, dataSet.calculateMedian("a"), EPSILON);
        assertEquals(100, dataSet.calculatePercentile("first", "a"), EPSILON);
    }
    
    @Test
    public void testCalculatePercentage() {
        final DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 2);
        dataSet.add("second", "a", 6);
        dataSet.add("third", "a", 3);
        
        assertEquals(100, dataSet.calculatePercentage(Predicates.<String>alwaysTrue()), EPSILON);
        assertEquals(66.667, dataSet.calculatePercentage(row ->dataSet.get(row, "a").intValue() >= 3), EPSILON);
    }

    @Test
    public void testSortOnColumn() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 7);
        dataSet.add("second", "a", 11);
        dataSet.add("third", "a", 1);
        dataSet.add("fourth", "a", null);
        dataSet.sortAscending("a");
        
        assertEquals(ImmutableList.of("third", "first", "second", "fourth"), dataSet.getRows());
    }
    
    @Test
    public void testReverseSortOnColumn() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 7);
        dataSet.add("second", "a", 11);
        dataSet.add("third", "a", 1);
        dataSet.add("fourth", "a", null);
        dataSet.sortDescending("a");
        
        assertEquals(ImmutableList.of("second", "first", "third", "fourth"), dataSet.getRows());
    }
    
    @Test
    public void testFill() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("a", "1", 123);
        dataSet.add("a", "2", 123);
        dataSet.add("b", "1", 123);
        dataSet.fill("2", 0);
        
        assertEquals(0, dataSet.get("a", "2"));
        assertEquals(123, dataSet.get("b", "1"));
        assertEquals(0, dataSet.get("b", "2"));
    }
    
    @Test
    public void testFillMissing() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("a", "1", 123);
        dataSet.add("a", "2", 123);
        dataSet.add("b", "1", 123);
        dataSet.fillMissing("2", 0);
        
        assertEquals(123, dataSet.get("a", "2"));
        assertEquals(123, dataSet.get("b", "1"));
        assertEquals(0, dataSet.get("b", "2"));
    }
    
    @Test
    public void testCreateCopy() {
        DataSet<String, String> dataSet = new DataSet<>();
        dataSet.add("first", "a", 1);
        dataSet.add("second", "a", 2);
        
        DataSet<String, String> copy = dataSet.copy();
        copy.remove("first");
        
        assertEquals(ImmutableList.of("first", "second"), dataSet.getRows());
        assertEquals(ImmutableList.of("second"), copy.getRows());
    }
}
