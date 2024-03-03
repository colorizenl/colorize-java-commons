//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregateTest {

    private static final float EPSILON = 0.01f;

    @Test
    void sum() {
        assertEquals(1f, Aggregate.sum(List.of(1)), EPSILON);
        assertEquals(3f, Aggregate.sum(List.of(1, 2)), EPSILON);
    }

    @Test
    void min() {
        assertEquals(0f, Aggregate.min(Collections.emptyList()), EPSILON);
        assertEquals(1f, Aggregate.min(List.of(1)), EPSILON);
        assertEquals(1f, Aggregate.min(List.of(1, 2)), EPSILON);
    }

    @Test
    void max() {
        assertEquals(0f, Aggregate.max(Collections.emptyList()), EPSILON);
        assertEquals(1f, Aggregate.max(List.of(1)), EPSILON);
        assertEquals(2f, Aggregate.max(List.of(1, 2)), EPSILON);
    }

    @Test
    void average() {
        assertEquals(1f, Aggregate.average(List.of(1)), EPSILON);
        assertEquals(1.5f, Aggregate.average(List.of(1, 2)), EPSILON);
    }

    @Test
    void median() {
        assertEquals(1f, Aggregate.median(List.of(1)), EPSILON);
        assertEquals(2f, Aggregate.median(List.of(1, 2)), EPSILON);
        assertEquals(2f, Aggregate.median(List.of(1, 2, 3)), EPSILON);
    }

    @Test
    void fraction() {
        assertEquals(0f, Aggregate.fraction(0, 0), EPSILON);
        assertEquals(0f, Aggregate.fraction(1, 0), EPSILON);
        assertEquals(0f, Aggregate.fraction(0, 1), EPSILON);
        assertEquals(0.5f, Aggregate.fraction(1, 2), EPSILON);
    }

    @Test
    void percentage() {
        assertEquals(0f, Aggregate.percentage(0, 0), EPSILON);
        assertEquals(0f, Aggregate.percentage(0, 1), EPSILON);
        assertEquals(50f, Aggregate.percentage(1, 2), EPSILON);
    }

    @Test
    void percentile() {
        assertEquals(1f, Aggregate.percentile(List.of(1, 2, 3, 4, 5), 0), EPSILON);
        assertEquals(4.96f, Aggregate.percentile(List.of(1, 2, 3, 4, 5), 99), EPSILON);
        assertEquals(3f, Aggregate.percentile(List.of(1, 2, 3, 4, 5), 50), EPSILON);
        assertEquals(4.2f, Aggregate.percentile(List.of(1, 2, 3, 4, 5), 80), EPSILON);
        assertEquals(3.8f, Aggregate.percentile(List.of(1, 2, 3, 4, 5), 70), EPSILON);
    }

    @Test
    void correlation() {
        List<Integer> original = List.of(1, 2, 3, 4, 5, 6);
        List<Integer> similar = List.of(1, 2, 3, 4, 8, 7);
        List<Integer> differentOrder = List.of(8, 7, 3, 4, 1, 2);
        List<Integer> dissimilar = List.of(1, 18, 213, 121, 38, 47);

        assertEquals(1f, Aggregate.correlation(original, original), EPSILON);
        assertEquals(0.93f, Aggregate.correlation(original, similar), EPSILON);
        assertEquals(-0.90f, Aggregate.correlation(original, differentOrder), EPSILON);
        assertEquals(0.13f, Aggregate.correlation(original, dissimilar), EPSILON);
    }
}
