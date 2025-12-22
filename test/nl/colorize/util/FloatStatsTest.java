//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloatStatsTest {

    private static final float EPSILON = 0.01f;

    @Test
    void createDataSet() {
        assertEquals("[1.0, 1.5, 2.0]", FloatStats.of(1f, 1.5f, 2f).toString());
        assertEquals("[1.0, 2.0]", FloatStats.of(List.of(1, 2)).toString());
        assertEquals("[]", FloatStats.of(Collections.emptyList()).toString());
    }

    @Test
    void sum() {
        assertEquals(1f, FloatStats.of(1).sum(), EPSILON);
        assertEquals(3f, FloatStats.of(1, 2).sum(), EPSILON);
    }

    @Test
    void min() {
        assertEquals(0f, FloatStats.of(Collections.emptyList()).min(), EPSILON);
        assertEquals(1f, FloatStats.of(1).min(), EPSILON);
        assertEquals(1f, FloatStats.of(1, 2).min(), EPSILON);
    }

    @Test
    void max() {
        assertEquals(0f, FloatStats.of(Collections.emptyList()).max(), EPSILON);
        assertEquals(1f, FloatStats.of(1).max(), EPSILON);
        assertEquals(2f, FloatStats.of(1, 2).max(), EPSILON);
    }

    @Test
    void average() {
        assertEquals(1f, FloatStats.of(1).average(), EPSILON);
        assertEquals(1.5f, FloatStats.of(1, 2).average(), EPSILON);
    }

    @Test
    void median() {
        assertEquals(0f, FloatStats.of().median(), EPSILON);
        assertEquals(1f, FloatStats.of(1).median(), EPSILON);
        assertEquals(2f, FloatStats.of(1, 2).median(), EPSILON);
        assertEquals(2f, FloatStats.of(1, 2, 3).median(), EPSILON);
    }

    @Test
    void percentage() {
        assertEquals(0f, FloatStats.percentage(0, 0), EPSILON);
        assertEquals(0f, FloatStats.percentage(0, 1), EPSILON);
        assertEquals(50f, FloatStats.percentage(1, 2), EPSILON);
        assertEquals(25f, FloatStats.of(2, 2).percentage(1), EPSILON);
    }

    @Test
    void percentile() {
        assertEquals(1f, FloatStats.of(1, 2, 3, 4, 5).percentile(0), EPSILON);
        assertEquals(4.96f, FloatStats.of(1, 2, 3, 4, 5).percentile(99), EPSILON);
        assertEquals(3f, FloatStats.of(1, 2, 3, 4, 5).percentile(50), EPSILON);
        assertEquals(4.2f, FloatStats.of(1, 2, 3, 4, 5).percentile(80), EPSILON);
        assertEquals(3.8f, FloatStats.of(1, 2, 3, 4, 5).percentile(70), EPSILON);
    }

    @Test
    void correlation() {
        FloatStats original = FloatStats.of(1, 2, 3, 4, 5, 6);
        FloatStats similar = FloatStats.of(1, 2, 3, 4, 8, 7);
        FloatStats differentOrder = FloatStats.of(8, 7, 3, 4, 1, 2);
        FloatStats dissimilar = FloatStats.of(1, 18, 213, 121, 38, 47);

        assertEquals(1f, original.pearsonCorrelation(original), EPSILON);
        assertEquals(0.93f, original.pearsonCorrelation(similar), EPSILON);
        assertEquals(-0.90f, original.pearsonCorrelation(differentOrder), EPSILON);
        assertEquals(0.13f, original.pearsonCorrelation(dissimilar), EPSILON);
    }

    @Test
    void multiplyPercentage() {
        assertEquals(0f, FloatStats.multiplyPercentage(0f, 0f), EPSILON);
        assertEquals(0f, FloatStats.multiplyPercentage(0f, 50f), EPSILON);
        assertEquals(100f, FloatStats.multiplyPercentage(100f, 100f), EPSILON);
        assertEquals(50f, FloatStats.multiplyPercentage(100f, 50f), EPSILON);
        assertEquals(25f, FloatStats.multiplyPercentage(50f, 50f), EPSILON);
    }
}
