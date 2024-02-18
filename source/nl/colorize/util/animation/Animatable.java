//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Animated object that expects to receive frame updates for as long as the
 * animation is active.
 */
@FunctionalInterface
public interface Animatable {
    
    /**
     * Called every frame update while the animation is active.
     * @param deltaTime Time since the last frame update, in seconds.
     */
    public void onFrame(float deltaTime);
}
