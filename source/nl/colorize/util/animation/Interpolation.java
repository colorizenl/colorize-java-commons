//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Interpolation methods. Some of these were converted from Flash ActionScript 
 * implementations explained in the article
 * <a href="http://gizma.com/easing/">http://gizma.com/easing/</a>.
 * <p>
 * The documentation for each interpolation method includes a graphical
 * example in ASCII art to visualize the interpolation curve.
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
    public static final Interpolation DISCRETE = (x0, x1, delta) -> delta >= 1.0 ? x1 : x0;

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
        return x0 + delta * (x1 - x0);
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
        double delta2 = 3.0 - (delta * 2.0);
        return x0 + (delta * delta * delta2) * (x1 - x0);
    };

    /**
     * Smootherstep interpolation.
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
    public static final Interpolation SMOOTHERSTEP = (x0, x1, delta) -> {
        double cubicDelta = delta * delta * delta * (delta * (delta * 6.0 - 15.0) + 10.0);
        return x0 + cubicDelta * (x1 - x0);
    };

    /**
     * Returns the interpolated value between {@code x0} and {@code x1}.
     * If the value of {@code delta} is 0, it represents the start of the
     * animation. If the value of {@code delta} is 1, it represents the
     * end of the animation.
     */
    public double interpolate(double x0, double x1, double delta);

    /**
     * Returns the interpolated value between {@code x0} and {@code x1}.
     * Unlike {@link #interpolate(double, double, double)}, the value of
     * {@code delta} is clamped to the range between 0 and 1, meaning
     * this method can only be used to interpolate, not to extrapolate.
     */
    default double strictInterpolate(double x0, double x1, double delta) {
        return interpolate(x0, x1, Math.clamp(delta, 0.0, 1.0));
    }
}
