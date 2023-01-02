//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Interpolation methods. Some of these were converted from Flash ActionScript 
 * implementations explained in the article
 * <a href="http://gizma.com/easing/">http://gizma.com/easing/</a>.
 * <p>
 * The JavaDoc for each interpolation method includes a graphical example in
 * ASCII art to visualize the interpolation curve.
 */
@FunctionalInterface
public interface Interpolation {

    /**
     * Discrete interpolation.
     * <p>
     * <pre>
     * . . . . . . . . x
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * x x x x x x x x .
     * </pre>
     */
    public static final Interpolation DISCRETE = (x0, x1, delta) -> (delta >= 1f) ? x1 : x0;

    /**
     * Linear interpolation.
     * <p>
     * <pre>
     * . . . . . . . . x
     * . . . . . . . x .
     * . . . . . . x . .
     * . . . . . x . . .
     * . . . . x . . . .
     * . . . x . . . . .
     * . . x . . . . . .
     * . x . . . . . . .
     * x . . . . . . . .
     * </pre>
     */
    public static final Interpolation LINEAR = (x0, x1, delta) -> {
        return x0 + clampDelta(delta) * (x1 - x0);
    };

    /**
     * Easing interpolation.
     * <p>
     * <pre>
     * . . . . . . . x x
     * . . . . . . x . .
     * . . . . . . . . .
     * . . . . . x . . .
     * . . . . x . . . .
     * . . . . . . . . .
     * . . . x . . . . .
     * . . x . . . . . .
     * x x . . . . . . .
     * </pre>
     */
    public static final Interpolation EASE = (x0, x1, delta) -> {
        float delta2 = 3f - (clampDelta(delta) * 2f);
        return x0 + (delta * delta * delta2) * (x1 - x0);
    };

    /**
     * Cubic easing interpolation.
     * <p>
     * <pre>
     * . . . . . . x x x
     * . . . . . . . . .
     * . . . . . x . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . . x . . . .
     * . . . . . . . . .
     * . . . x . . . . .
     * x x x . . . . . .
     * </pre>
     */
    public static final Interpolation CUBIC = (x0, x1, delta) -> {
        delta = clampDelta(delta) / 0.5f;
        if (delta < 1f) {
            return (x1 - x0) / 2f * delta * delta * delta + x0;
        } else {
            delta -= 2f;
            return (x1 - x0) / 2f * (delta * delta * delta + 2f) + x0;
        }
    };

    /**
     * Quadratic easing interpolation.
     * <p>
     * <pre>
     * . . . . . . . x x
     * . . . . . . x . .
     * . . . . . . . . .
     * . . . . . x . . .
     * . . . . x . . . .
     * . . . . . . . . .
     * . . . x . . . . .
     * . . . . . . . . .
     * x x x . . . . . .
     * </pre>
     */
    public static final Interpolation QUADRATIC = (x0, x1, delta) -> {
        delta = clampDelta(delta) / 0.5f;
        if (delta < 1f) {
            return (x1 - x0) / 2f * delta * delta + x0;
        } else {
            delta--;
            return -(x1 - x0) / 2f * (delta * (delta - 2f) - 1f) + x0;
        }
    };

    /**
     * Quantic easing interpolation.
     * <p>
     * <pre>
     * . . . . . . x x x
     * . . . . . x . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . . x . . . .
     * . . . . . . . . .
     * . . . . . . . . .
     * . . . x . . . . .
     * x x x . . . . . .
     * </pre>
     */
    public static final Interpolation QUINTIC = (x0, x1, delta) -> {
        delta = clampDelta(delta) / 0.5f;
        if (delta < 1f) {
            return (x1 - x0) / 2f * delta * delta * delta * delta * delta + x0;
        } else {
            delta -= 2f;
            return (x1 - x0) / 2f * (delta * delta * delta * delta * delta + 2f) + x0;
        }
    };

    /**
     * Returns the interpolated value between {@code x0} and {@code x1}. The value
     * of {@code delta} is a number between 0 and 1 describing how much of the
     * animation has been completed.
     */
    public float interpolate(float x0, float x1, float delta);

    private static float clampDelta(float delta) {
        return Math.min(Math.max(delta, 0f), 1f);
    }
}
