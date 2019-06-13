//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.Timer;

import com.google.common.base.Preconditions;

import nl.colorize.util.animation.Animator;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;

/**
 * Implementation of an {@code Animator} for Swing applications. All animations
 * are performed on the Swing thread, and use the Swing timer to schedule frame
 * updates.
 * <p>
 * In addition to the animation mechanism itself, this class also contains
 * several convenience methods for performing commonly used animations in
 * Swing applications.
 */
public class SwingAnimator extends Animator {

    private Timer swingTimer;
    private int framerate;

    public static final int DEFAULT_FRAMERATE = 30;

    /**
     * Creates a {@code SwingAnimator} that will operate at the specified framerate.
     */
    public SwingAnimator(int framerate) {
        Preconditions.checkArgument(framerate >= 1, "Invalid framerate: " + framerate);
        this.framerate = framerate;
    }
    
    /**
     * Creates a {@code SwingAnimator} that will operate at the default framerate
     * of 30 frames per second.
     */
    public SwingAnimator() {
        this(DEFAULT_FRAMERATE);
    }

    @Override
    public void start() {
        final float frameTimeInSeconds = 1f / framerate;
        final int frameTimeInMilliseconds = Math.round(frameTimeInSeconds * 1000);

        swingTimer = new Timer(frameTimeInMilliseconds, e -> performFrameUpdate(frameTimeInSeconds));
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
    
    public void animateBackgroundColor(JComponent component, Color target, float duration) {
        AnimatedColor targetColor = new AnimatedColor(component.getBackground(), target, duration);
        component.setBackground(targetColor);
        
        Timeline timeline = new Timeline(Interpolation.LINEAR);
        timeline.addKeyFrame(0f, 0f);
        timeline.addKeyFrame(duration, duration);

        play(timeline).onFrame(anim -> {
            targetColor.setPlayhead(timeline.getValue());
            component.repaint();
        });
    }
    
    public void animateForegroundColor(JComponent component, Color target, float duration) {
        AnimatedColor targetColor = new AnimatedColor(component.getForeground(), target, duration);
        component.setForeground(targetColor);
        
        Timeline timeline = new Timeline(Interpolation.LINEAR);
        timeline.addKeyFrame(0f, 0f);
        timeline.addKeyFrame(duration, duration);

        play(timeline).onFrame(anim -> {
            targetColor.setPlayhead(timeline.getValue());
            component.repaint();
        });
    }
    
    public void animateWidth(JComponent component, int target, float duration) {
        Timeline timeline = new Timeline(Interpolation.EASE);
        timeline.addKeyFrame(0f, component.getPreferredSize().width);
        timeline.addKeyFrame(duration, target);

        play(timeline).onFrame(anim -> {
            SwingUtils.setPreferredWidth(component, Math.round(timeline.getValue()));
            component.revalidate();
        });
    }
    
    public void animateHeight(JComponent component, int target, float duration) {
        Timeline timeline = new Timeline(Interpolation.EASE);
        timeline.addKeyFrame(0f, component.getPreferredSize().height);
        timeline.addKeyFrame(duration, target);

        play(timeline).onFrame(anim -> {
            SwingUtils.setPreferredHeight(component, Math.round(timeline.getValue()));
            component.revalidate();
        });
    }
}
