//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InterpolationTest {

    private static final double EPSILON = 0.0001;

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
    void extrapolate() {
        assertEquals(10f, Interpolation.LINEAR.interpolate(10f, 20f, 0f));
        assertEquals(20f, Interpolation.LINEAR.interpolate(10f, 20f, 1f));

        assertEquals(25f, Interpolation.LINEAR.interpolate(10f, 20f, 1.5f));
        assertEquals(30f, Interpolation.LINEAR.interpolate(10f, 20f, 2f));

        assertEquals(0f, Interpolation.LINEAR.interpolate(10f, 20f, -1f));
        assertEquals(-10f, Interpolation.LINEAR.interpolate(10f, 20f, -2f));
        assertEquals(-20f, Interpolation.LINEAR.interpolate(10f, 20f, -3f));
    }

    @Test
    public void testSmootherstepInterpolation() {
        assertEquals(0f, Interpolation.SMOOTHERSTEP.interpolate(0f, 1f, 0f), EPSILON);
        assertEquals(0.1035f, Interpolation.SMOOTHERSTEP.interpolate(0f, 1f, 0.25f), EPSILON);
        assertEquals(0.5f, Interpolation.SMOOTHERSTEP.interpolate(0f, 1f, 0.5f), EPSILON);
        assertEquals(0.8964f, Interpolation.SMOOTHERSTEP.interpolate(0f, 1f, 0.75f), EPSILON);
        assertEquals(1f, Interpolation.SMOOTHERSTEP.interpolate(0f, 1f, 1f), EPSILON);
    }

    @Test
    void strictInterpolate() {
        assertEquals(30.0, Interpolation.LINEAR.interpolate(10, 20, 2.0));
        assertEquals(0.0, Interpolation.LINEAR.interpolate(10, 20, -1.0));

        assertEquals(20.0, Interpolation.LINEAR.strictInterpolate(10, 20, 2.0));
        assertEquals(10.0, Interpolation.LINEAR.strictInterpolate(10, 20, -1.0));
    }
}
