//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Animated object that will only animate for a limited time.
 */
public interface TimedAnimatable extends Animatable {

    /**
     * Returns false while this animation is playing, returns true when the
     * animation has been completed. Once completed, the animation will no
     * longer receive frame updates.
     */
    public boolean isCompleted();

    /**
     * Resets the state of this animation to the beginning, so that it can
     * be played again.
     */
    public void reset();
}
