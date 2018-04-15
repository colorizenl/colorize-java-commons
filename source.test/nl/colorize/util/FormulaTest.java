//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for the {@code Formula} class.
 */
public class FormulaTest {

    private static final double EPSILON = 0.001;

    @Test
    public void testCount() {
        assertEquals(0, Formula.COUNT.calculate(), EPSILON);
        assertEquals(1, Formula.COUNT.calculate(1.0), EPSILON);
        assertEquals(2, Formula.COUNT.calculate(1.0, 2.0), EPSILON);
    }

    @Test
    public void testMinimum() {
        assertEquals(0, Formula.MINIMUM.calculate(), EPSILON);
        assertEquals(1.0, Formula.MINIMUM.calculate(1.0), EPSILON);
        assertEquals(1.0, Formula.MINIMUM.calculate(1.0, 2.0), EPSILON);
        assertEquals(-2.0, Formula.MINIMUM.calculate(1.0, -2.0), EPSILON);
    }

    @Test
    public void testMaximum() {
        assertEquals(0, Formula.MAXIMUM.calculate(), EPSILON);
        assertEquals(1.0, Formula.MAXIMUM.calculate(1.0), EPSILON);
        assertEquals(2.0, Formula.MAXIMUM.calculate(1.0, 2.0), EPSILON);
    }

    @Test
    public void testSum() {
        assertEquals(0, Formula.SUM.calculate(), EPSILON);
        assertEquals(1.0, Formula.SUM.calculate(1.0), EPSILON);
        assertEquals(3.0, Formula.SUM.calculate(1.0, 2.0), EPSILON);
    }

    @Test
    public void testAverage() {
        assertEquals(0, Formula.AVERAGE.calculate(), EPSILON);
        assertEquals(1.0, Formula.AVERAGE.calculate(1.0), EPSILON);
        assertEquals(1.5, Formula.AVERAGE.calculate(1.0, 2.0), EPSILON);
        assertEquals(2.0, Formula.AVERAGE.calculate(1.0, 2.0, 3.0), EPSILON);
    }

    @Test
    public void testMedian() {
        assertEquals(0, Formula.MEDIAN.calculate(), EPSILON);
        assertEquals(1.0, Formula.MEDIAN.calculate(1.0), EPSILON);
        assertEquals(2.0, Formula.MEDIAN.calculate(1.0, 2.0), EPSILON);
        assertEquals(2.0, Formula.MEDIAN.calculate(1.0, 2.0, 3.0), EPSILON);
        assertEquals(2.0, Formula.MEDIAN.calculate(1.0, 7.0, 2.0), EPSILON);
    }

    @Test
    public void testWeightedAverage() {
        assertEquals(0, Formula.WEIGHTED_AVERAGE.calculate(), EPSILON);
        assertEquals(1.0, Formula.WEIGHTED_AVERAGE.calculate(
                ImmutableList.of(1.0), ImmutableList.of(1.0)), EPSILON);
        assertEquals(1.5, Formula.WEIGHTED_AVERAGE.calculate(
            ImmutableList.of(1.0, 2.0), ImmutableList.of(1.0, 1.0)), EPSILON);
        assertEquals(1.75, Formula.WEIGHTED_AVERAGE.calculate(
            ImmutableList.of(1.0, 2.0), ImmutableList.of(1.0, 3.0)), EPSILON);
        assertEquals(0.0, Formula.WEIGHTED_AVERAGE.calculate(
            ImmutableList.of(1.0, 2.0), ImmutableList.of(0.0, 0.0)), EPSILON);
    }
}
