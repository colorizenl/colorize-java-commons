//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Animated object that expects to receive frame updates from the moment the
 * animation starts to the moment the animation is completed.
 */
public interface Animatable {
    
    /**
     * Called every frame update while the animation is active.
     * @param deltaTime Time since the last frame update, in seconds.
     */
    public void onFrame(float deltaTime);
    
    /**
     * Indicates the animation has completed, after which it will no longer
     * receive frame updates. By default, animations will play indefinitely.
     */
    default boolean isCompleted() {
        return false;
    }
}
