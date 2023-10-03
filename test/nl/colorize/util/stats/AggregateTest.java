//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static nl.colorize.util.stats.Aggregate.AVERAGE;
import static nl.colorize.util.stats.Aggregate.COUNT;
import static nl.colorize.util.stats.Aggregate.MAX;
import static nl.colorize.util.stats.Aggregate.MEDIAN;
import static nl.colorize.util.stats.Aggregate.MIN;
import static nl.colorize.util.stats.Aggregate.SUM;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregateTest {

    private static final float EPSILON = 0.01f;

    @Test
    void count() {
        assertEquals(0f, COUNT.calc(), EPSILON);
        assertEquals(1f, COUNT.calc(1), EPSILON);
        assertEquals(2f, COUNT.calc(1, 2), EPSILON);
    }

    @Test
    void sum() {
        assertEquals(1f, SUM.calc(List.of(1)), EPSILON);
        assertEquals(3f, SUM.calc(List.of(1, 2)), EPSILON);
    }

    @Test
    void min() {
        assertEquals(0f, MIN.calc(Collections.emptyList()), EPSILON);
        assertEquals(1f, MIN.calc(List.of(1)), EPSILON);
        assertEquals(1f, MIN.calc(List.of(1, 2)), EPSILON);
    }

    @Test
    void max() {
        assertEquals(0f, MAX.calc(Collections.emptyList()), EPSILON);
        assertEquals(1f, MAX.calc(List.of(1)), EPSILON);
        assertEquals(2f, MAX.calc(List.of(1, 2)), EPSILON);
    }

    @Test
    void average() {
        assertEquals(1f, AVERAGE.calc(List.of(1)), EPSILON);
        assertEquals(1.5f, AVERAGE.calc(List.of(1, 2)), EPSILON);
    }

    @Test
    void median() {
        assertEquals(1f, MEDIAN.calc(List.of(1)), EPSILON);
        assertEquals(2f, MEDIAN.calc(List.of(1, 2)), EPSILON);
        assertEquals(2f, MEDIAN.calc(List.of(1, 2, 3)), EPSILON);
    }

    @Test
    void percentage() {
        assertEquals(0f, Aggregate.toPercentage(0, 0), EPSILON);
        assertEquals(0f, Aggregate.toPercentage(0, 1), EPSILON);
        assertEquals(50f, Aggregate.toPercentage(1, 2), EPSILON);
    }
}
