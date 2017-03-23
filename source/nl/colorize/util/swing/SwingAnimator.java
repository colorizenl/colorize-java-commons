//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

import nl.colorize.util.animation.Animator;

/**
 * Implementation of an {@code Animator} for Swing applications. All animations
 * are performed on the Swing thread, and use the Swing timer to schedule frame
 * updates.
 */
public class SwingAnimator extends Animator {

	private int framerate;
	private Timer swingTimer;

	/**
	 * Creates a {@code SwingAnimator} that will operate at the specified framerate.
	 * @throws IllegalArgumentException if the framerate is lower than 1 fps.
	 */
	public SwingAnimator(int framerate) {
		if (framerate < 1) {
			throw new IllegalArgumentException("Invalid framerate: " + framerate);
		}
		this.framerate = framerate;
	}
	
	/**
	 * Creates a {@code SwingAnimator} that will operate at a default framerate of 
	 * 25 frames per second.
	 * @deprecated Use {@link SwingAnimator()} instead. The target framerate should
	 *             be application-specific, instead of relying on the default value
	 *             set by this framework.  
	 */
	@Deprecated
	public SwingAnimator() {
		this(25);
	}

	@Override
	public void start() {
		final int frameTimeInMilliseconds = 1000 / framerate;
		final float frameTimeInSeconds = frameTimeInMilliseconds / 1000f;
		
		swingTimer = new Timer(frameTimeInMilliseconds, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doFrameUpdate(frameTimeInSeconds);
			}
		});
		swingTimer.setInitialDelay(frameTimeInMilliseconds);
		swingTimer.setCoalesce(false);
		swingTimer.start();
	}

	@Override
	public void stop() {
		cancelAll();
		swingTimer.stop();
		swingTimer = null;
	}
}
