//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregatesTest {

    private static final float EPSILON = 0.01f;

    @Test
    void sum() {
        assertEquals(1f, Aggregates.sum(List.of(1)), EPSILON);
        assertEquals(3f, Aggregates.sum(List.of(1, 2)), EPSILON);
    }

    @Test
    void average() {
        assertEquals(1f, Aggregates.average(List.of(1)), EPSILON);
        assertEquals(1.5f, Aggregates.average(List.of(1, 2)), EPSILON);
    }

    @Test
    void median() {
        assertEquals(1f, Aggregates.median(List.of(1)), EPSILON);
        assertEquals(2f, Aggregates.median(List.of(1, 2)), EPSILON);
        assertEquals(2f, Aggregates.median(List.of(1, 2, 3)), EPSILON);
    }

    @Test
    void percentage() {
        assertEquals(0f, Aggregates.percentage(0, 0), EPSILON);
        assertEquals(0f, Aggregates.percentage(0, 1), EPSILON);
        assertEquals(50f, Aggregates.percentage(1, 2), EPSILON);
    }
}
