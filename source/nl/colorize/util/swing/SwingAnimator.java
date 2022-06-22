//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import nl.colorize.util.animation.Animatable;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedules animations and drives them by performing frame updates at regular
 * intervals. All animations are performed on the Swing thread, and use the
 * Swing timer to schedule frame updates.
 * <p>
 * Animations receive frame updates through the {@link Animatable} interface.
 * All animations are played using the same framerate. In practice, the actual
 * framerate may deviate from the targeted framerate, depending on the amount of
 * work that is performed during frame updates and the performance of the
 * underlying platform. For this reason, all animations receive the exact delta
 * time since the last frame updates, so that animations can look consistent
 * even when there are small deviations in the time between frame updates.
 * <p>
 * In addition to the animation mechanism itself, this class also contains
 * several convenience methods for performing commonly used animations in
 * Swing applications.
 */
public class SwingAnimator {

    private Timer swingTimer;
    private int framerate;
    private List<Animatable> currentlyPlaying;

    public static final int DEFAULT_FRAMERATE = 30;

    /**
     * Creates a {@code SwingAnimator} that will operate at the specified
     * framerate. Note that playback of animations is not actually started
     * until {@link #start()} is called.
     */
    public SwingAnimator(int framerate) {
        Preconditions.checkArgument(framerate >= 1, "Invalid framerate: " + framerate);

        this.framerate = framerate;
        this.currentlyPlaying = new ArrayList<>();
    }
    
    /**
     * Creates a {@code SwingAnimator} that will operate at the default
     * framerate of 30 frames per second. Note that playback of animations
     * is not actually started until {@link #start()} is called.
     */
    public SwingAnimator() {
        this(DEFAULT_FRAMERATE);
    }

    /**
     * Starts this {@code Animator}. Scheduled animations will not receive frame
     * updates until this method is called.
     */
    public void start() {
        final float frameTimeInSeconds = 1f / framerate;
        final int frameTimeInMilliseconds = Math.round(frameTimeInSeconds * 1000);

        swingTimer = new Timer(frameTimeInMilliseconds, e -> performFrameUpdate(frameTimeInSeconds));
        swingTimer.setInitialDelay(frameTimeInMilliseconds);
        swingTimer.setCoalesce(false);
        swingTimer.start();
    }

    /**
     * Stops this {@code Animator}. Animations that are currently playing will
     * be cancelled.
     */
    public void stop() {
        cancelAll();
        swingTimer.stop();
        swingTimer = null;
    }

    /**
     * Performs a frame update for all animations that are currently playing.
     */
    protected void performFrameUpdate(float deltaTime) {
        List<Animatable> snapshot = ImmutableList.copyOf(currentlyPlaying);

        for (Animatable anim : snapshot) {
            doFrameUpdate(anim, deltaTime);
        }
    }

    private void doFrameUpdate(Animatable anim, float deltaTime) {
        if (isCurrentlyActive(anim)) {
            anim.onFrame(deltaTime);
        }
    }

    /**
     * Returns true if the animation is currently active and should receive frame
     * updates. By default all registered animations are considered active, but
     * this can be overridden by subclasses.
     */
    protected boolean isCurrentlyActive(Animatable anim) {
        return true;
    }

    /**
     * Starts playing an animation, that will continue playing until it is
     * cancelled.
     */
    public void play(Animatable anim) {
        currentlyPlaying.add(anim);
    }

    /**
     * Starts playing an animation based to the specified timeline. Every frame,
     * the animator will first update the timeline, and then call the specified
     * callback function. The animation will continue to play until it is
     * cancelled.
     */
    public void play(Timeline timeline, Animatable callback) {
        play(deltaTime -> {
            timeline.onFrame(deltaTime);
            callback.onFrame(deltaTime);
        });
    }

    /**
     * Cancels an animation that is currently playing.
     */
    public void cancel(Animatable anim) {
        currentlyPlaying.remove(anim);
    }

    /**
     * Cancels all animations that are currently playing.
     */
    public void cancelAll() {
        currentlyPlaying.clear();
    }
    
    public void animateBackgroundColor(JComponent component, Color target, float duration) {
        AnimatedColor targetColor = new AnimatedColor(component.getBackground(), target, duration);
        component.setBackground(targetColor);
        
        Timeline timeline = new Timeline(Interpolation.LINEAR);
        timeline.addKeyFrame(0f, 0f);
        timeline.addKeyFrame(duration, duration);

        play(timeline, dt -> {
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

        play(timeline, dt -> {
            targetColor.setPlayhead(timeline.getValue());
            component.repaint();
        });
    }
    
    public void animateWidth(JComponent component, int target, float duration) {
        Timeline timeline = new Timeline(Interpolation.EASE);
        timeline.addKeyFrame(0f, component.getPreferredSize().width);
        timeline.addKeyFrame(duration, target);

        play(timeline, dt -> {
            SwingUtils.setPreferredWidth(component, Math.round(timeline.getValue()));
            component.revalidate();
        });
    }
    
    public void animateHeight(JComponent component, int target, float duration) {
        Timeline timeline = new Timeline(Interpolation.EASE);
        timeline.addKeyFrame(0f, component.getPreferredSize().height);
        timeline.addKeyFrame(duration, target);

        play(timeline, dt -> {
            SwingUtils.setPreferredHeight(component, Math.round(timeline.getValue()));
            component.revalidate();
        });
    }

    protected List<Animatable> getCurrentlyPlaying() {
        return ImmutableList.copyOf(currentlyPlaying);
    }
}
