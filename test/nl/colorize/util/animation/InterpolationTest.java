//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InterpolationTest {

    private static final float EPSILON = 0.0001f;

    @Test
    public void testDiscreteInterpolation() {
        assertEquals(2f, Interpolation.DISCRETE.interpolate(2f, 9f, 0f), EPSILON);
        assertEquals(2f, Interpolation.DISCRETE.interpolate(2f, 9f, 0.5f), 0.01f);
        assertEquals(2f, Interpolation.DISCRETE.interpolate(2f, 9f, 0.9f), 0.01f);
        assertEquals(9f, Interpolation.DISCRETE.interpolate(2f, 9f, 1f), 0.01f);
    }

    @Test
    public void testLinearInterpolation() {
        assertEquals(3f, Interpolation.LINEAR.interpolate(3f, 5f, 0f), EPSILON);
        assertEquals(3.5f, Interpolation.LINEAR.interpolate(3f, 5f, 0.25f), EPSILON);
        assertEquals(4f, Interpolation.LINEAR.interpolate(3f, 5f, 0.5f), EPSILON);
        assertEquals(4.5f, Interpolation.LINEAR.interpolate(3f, 5f, 0.75f), EPSILON);
        assertEquals(5f, Interpolation.LINEAR.interpolate(3f, 5f, 1f), EPSILON);
    }

    @Test
    public void testEaseInterpolation() {
        assertEquals(3f, Interpolation.EASE.interpolate(3f, 5f, 0f), EPSILON);
        assertEquals(3.3125f, Interpolation.EASE.interpolate(3f, 5f, 0.25f), EPSILON);
        assertEquals(4f, Interpolation.EASE.interpolate(3f, 5f, 0.5f), EPSILON);
        assertEquals(4.6875f, Interpolation.EASE.interpolate(3f, 5f, 0.75f), EPSILON);
        assertEquals(5f, Interpolation.EASE.interpolate(3f, 5f, 1f), EPSILON);
    }

    @Test
    public void testCubicInterpolation() {
        assertEquals(0f, Interpolation.CUBIC.interpolate(0f, 1f, 0f), EPSILON);
        assertEquals(0.0625f, Interpolation.CUBIC.interpolate(0f, 1f, 0.25f), EPSILON);
        assertEquals(0.5f, Interpolation.CUBIC.interpolate(0f, 1f, 0.5f), EPSILON);
        assertEquals(0.9375f, Interpolation.CUBIC.interpolate(0f, 1f, 0.75f), EPSILON);
        assertEquals(1f, Interpolation.CUBIC.interpolate(0f, 1f, 1f), EPSILON);
    }

    @Test
    public void testQuadraticInterpolation() {
        assertEquals(0f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0f), EPSILON);
        assertEquals(0.125f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0.25f), EPSILON);
        assertEquals(0.5f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0.5f), EPSILON);
        assertEquals(0.875f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0.75f), EPSILON);
        assertEquals(1f, Interpolation.QUADRATIC.interpolate(0f, 1f, 1f), EPSILON);
    }
}
