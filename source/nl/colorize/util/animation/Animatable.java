//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Animated object that expects to receive frame updates while the animation is
 * running.
 */
public interface Animatable {
	
	/**
	 * Called every frame update while the animation is active.
	 * @param deltaTime Time since the last frame upate, in seconds.
	 */
	public void onFrame(float deltaTime);
}
