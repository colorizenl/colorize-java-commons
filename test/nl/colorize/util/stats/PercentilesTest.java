//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PercentilesTest {

    private static final float EPSILON = 0.1f;

    @Test
    void getPercentile() {
        List<Integer> values = List.of(0, 0, 1, 1, 2, 3, 5, 8, 13, 21, 34);
        Percentiles percentileData = new Percentiles(values);

        assertEquals(0f, percentileData.getPercentile(-1f), EPSILON);
        assertEquals(10f, percentileData.getPercentile(0f), EPSILON);
        assertEquals(30f, percentileData.getPercentile(1f), EPSILON);
        assertEquals(40f, percentileData.getPercentile(2f), EPSILON);
        assertEquals(50f, percentileData.getPercentile(3f), EPSILON);
        assertEquals(90f, percentileData.getPercentile(21f), EPSILON);
        assertEquals(100f, percentileData.getPercentile(34f), EPSILON);
        assertEquals(100f, percentileData.getPercentile(100f), EPSILON);
    }

    @Test
    void getValueForPercentile() {
        List<Integer> values = List.of(0, 0, 1, 1, 2, 3, 5, 8, 13, 21, 34);
        Percentiles percentileData = new Percentiles(values);

        assertEquals(0f, percentileData.getValueForPercentile(1), EPSILON);
        assertEquals(0f, percentileData.getValueForPercentile(10), EPSILON);
        assertEquals(1f, percentileData.getValueForPercentile(30), EPSILON);
        assertEquals(2f, percentileData.getValueForPercentile(40), EPSILON);
        assertEquals(3f, percentileData.getValueForPercentile(50), EPSILON);
        assertEquals(21f, percentileData.getValueForPercentile(90), EPSILON);
        assertEquals(34f, percentileData.getValueForPercentile(100), EPSILON);
    }

    @Test
    void emptyDataSetProducesZero() {
        Percentiles percentileData = new Percentiles(Collections.emptyList());

        assertEquals(0f, percentileData.getPercentile(10), EPSILON);
        assertEquals(0f, percentileData.getValueForPercentile(10), EPSILON);
    }

    @Test
    void singleValueInDataSet() {
        Percentiles percentileData = new Percentiles(List.of(10f));

        assertEquals(0f, percentileData.getPercentile(10), EPSILON);
        assertEquals(10f, percentileData.getValueForPercentile(10), EPSILON);
    }

    @Test
    void interpolateValue() {
        List<Integer> values = List.of(0, 0, 1, 1, 2, 3, 5, 8, 13, 21, 34);
        Percentiles percentileData = new Percentiles(values);

        assertEquals(1f, percentileData.interpolateValue(30), EPSILON);
        assertEquals(1.5f, percentileData.interpolateValue(35), EPSILON);
        assertEquals(2f, percentileData.interpolateValue(40), EPSILON);
        assertEquals(17f, percentileData.interpolateValue(85), EPSILON);
    }
}
