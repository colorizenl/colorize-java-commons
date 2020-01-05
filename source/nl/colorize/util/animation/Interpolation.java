//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Interpolation methods. Some of these were converted from Flash ActionScript 
 * implementations listed <a href="http://gizma.com/easing/">here</a>.
 */
public enum Interpolation {
    
    DISCRETE {
        @Override
        protected float apply(float x0, float x1, float delta) {
            return (delta >= 1f) ? x1 : x0;
        }
    },
    
    LINEAR {
        @Override
        protected float apply(float x0, float x1, float delta) {
            return x0 + delta * (x1 - x0);
        }
    },
        
    EASE {
        @Override
        protected float apply(float x0, float x1, float delta) {
            float delta2 = 3f - (delta * 2f);
            return x0 + (delta * delta * delta2) * (x1 - x0);
        }
    },
    
    CUBIC {
        @Override
        protected float apply(float x0, float x1, float delta) {
            delta /= 0.5f;
            if (delta < 1f) {
                return (x1 - x0) / 2f * delta * delta * delta + x0;
            } else {
                delta -= 2f;
                return (x1 - x0) / 2f * (delta * delta * delta + 2f) + x0;
            }
        }
    },
    
    QUADRATIC {
        @Override
        protected float apply(float x0, float x1, float delta) {
            delta /= 0.5f;
            if (delta < 1f) {
                return (x1 - x0) / 2f * delta * delta + x0;
            } else {
                delta--;
                return -(x1 - x0) / 2f * (delta * (delta - 2f) - 1f) + x0;
            }
        }
    },
    
    QUINTIC {
        @Override
        protected float apply(float x0, float x1, float delta) {
            delta /= 0.5f;
            if (delta < 1f) {
                return (x1 - x0) / 2f * delta * delta * delta * delta * delta + x0;
            } else {
                delta -= 2f;
                return (x1 - x0) / 2f * (delta * delta * delta * delta * delta + 2f) + x0;
            }
        }
    };

    /**
     * Returns the interpolated value between {@code x0} and {@code x1}.
     * @return The value of {@code x}, which is somewhere between {@code x0}
     *         and {@code x1} depending on how much of the animation has been
     *         completed.
     * @param delta A number between 0 and 1 describing how much of the
     *        animation has been completed. 
     */
    public float interpolate(float x0, float x1, float delta) {
        delta = Math.max(delta, 0f);
        delta = Math.min(delta, 1f);
        return apply(x0, x1, delta);
    }
    
    /**
     * Internal implementation of {@link #interpolate(float, float, float)}. The
     * value of {@code delta} is clamped between 0 and 1.
     */
    protected abstract float apply(float x0, float x1, float delta);
}
