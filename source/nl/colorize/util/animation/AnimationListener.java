//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Receives notifications while animations are being played. The type of animation
 * events the listener is interested in is specified when registering it with an
 * {@code Animator}.
 * @param <T> The type of {@link Animatable} to receive notifications for.
 */
public interface AnimationListener<T extends Animatable> {

	/**
	 * Called by an {@code Animator} to notify this listener of an event for an
	 * animation that is currenly playing.
	 * @param anim The animation for which this event is sent.
	 */
	public void onAnimationEvent(T anim);
}
