//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import com.google.common.base.Preconditions;

/**
 * Animation that is active for a predefined duration.
 */
public class TimedAnimation implements Animatable {

    private Animatable delegate;
    private float time;
    private float duration;

    private TimedAnimation(Animatable delegate, float duration) {
        Preconditions.checkArgument(duration > 0f, "Invalid duration: " + duration);

        this.delegate = delegate;
        this.time = 0f;
        this.duration = duration;
    }

    @Override
    public void onFrame(float deltaTime) {
        time += deltaTime;
        delegate.onFrame(deltaTime);
    }

    @Override
    public boolean isCompleted() {
        return time >= duration;
    }

    public void reset() {
        time = 0f;
    }

    /**
     * Wraps another animation, and plays it for the specified duration.
     * @param duration Describes how long the animation should play, in seconds.
     */
    public static TimedAnimation from(Animatable anim, float duration) {
        return new TimedAnimation(anim, duration);
    }
}
