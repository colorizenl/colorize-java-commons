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

class StatisticsTest {

    private static final float EPSILON = 0.01f;

    @Test
    void sum() {
        assertEquals(1f, Statistics.sum(List.of(1)), EPSILON);
        assertEquals(3f, Statistics.sum(List.of(1, 2)), EPSILON);
    }

    @Test
    void min() {
        assertEquals(0f, Statistics.min(Collections.emptyList()), EPSILON);
        assertEquals(1f, Statistics.min(List.of(1)), EPSILON);
        assertEquals(1f, Statistics.min(List.of(1, 2)), EPSILON);
    }

    @Test
    void max() {
        assertEquals(0f, Statistics.max(Collections.emptyList()), EPSILON);
        assertEquals(1f, Statistics.max(List.of(1)), EPSILON);
        assertEquals(2f, Statistics.max(List.of(1, 2)), EPSILON);
    }

    @Test
    void average() {
        assertEquals(1f, Statistics.average(List.of(1)), EPSILON);
        assertEquals(1.5f, Statistics.average(List.of(1, 2)), EPSILON);
    }

    @Test
    void median() {
        assertEquals(1f, Statistics.median(List.of(1)), EPSILON);
        assertEquals(2f, Statistics.median(List.of(1, 2)), EPSILON);
        assertEquals(2f, Statistics.median(List.of(1, 2, 3)), EPSILON);
    }

    @Test
    void fraction() {
        assertEquals(0f, Statistics.fraction(0, 0), EPSILON);
        assertEquals(0f, Statistics.fraction(1, 0), EPSILON);
        assertEquals(0f, Statistics.fraction(0, 1), EPSILON);
        assertEquals(0.5f, Statistics.fraction(1, 2), EPSILON);
    }

    @Test
    void percentage() {
        assertEquals(0f, Statistics.percentage(0, 0), EPSILON);
        assertEquals(0f, Statistics.percentage(0, 1), EPSILON);
        assertEquals(50f, Statistics.percentage(1, 2), EPSILON);
    }

    @Test
    void percentile() {
        assertEquals(1f, Statistics.percentile(List.of(1, 2, 3, 4, 5), 0), EPSILON);
        assertEquals(4.96f, Statistics.percentile(List.of(1, 2, 3, 4, 5), 99), EPSILON);
        assertEquals(3f, Statistics.percentile(List.of(1, 2, 3, 4, 5), 50), EPSILON);
        assertEquals(4.2f, Statistics.percentile(List.of(1, 2, 3, 4, 5), 80), EPSILON);
        assertEquals(3.8f, Statistics.percentile(List.of(1, 2, 3, 4, 5), 70), EPSILON);
    }

    @Test
    void correlation() {
        List<Integer> original = List.of(1, 2, 3, 4, 5, 6);
        List<Integer> similar = List.of(1, 2, 3, 4, 8, 7);
        List<Integer> differentOrder = List.of(8, 7, 3, 4, 1, 2);
        List<Integer> dissimilar = List.of(1, 18, 213, 121, 38, 47);

        assertEquals(1f, Statistics.correlation(original, original), EPSILON);
        assertEquals(0.93f, Statistics.correlation(original, similar), EPSILON);
        assertEquals(-0.90f, Statistics.correlation(original, differentOrder), EPSILON);
        assertEquals(0.13f, Statistics.correlation(original, dissimilar), EPSILON);
    }
}
