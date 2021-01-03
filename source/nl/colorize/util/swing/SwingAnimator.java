//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import nl.colorize.util.animation.Animatable;
import nl.colorize.util.animation.AnimationObserver;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    private Multimap<Animatable, MulticastAnimationObserver> observers;

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
        this.observers = ArrayListMultimap.create();
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

            for (MulticastAnimationObserver observer : observers.get(anim)) {
                observer.onFrame(anim);
            }
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
     * Schedules an animation to be played until it is either completed or this
     * animator is stopped.
     * @return An observer object that can be used to track the playback of the
     *         animation.
     */
    public AnimationObserver play(Animatable anim) {
        currentlyPlaying.add(anim);

        MulticastAnimationObserver observer = new MulticastAnimationObserver();
        observers.put(anim, observer);
        return observer;
    }

    /**
     * Cancels an animation that is currently playing.
     */
    public void cancel(Animatable anim) {
        currentlyPlaying.remove(anim);
        observers.removeAll(anim);
    }

    /**
     * Cancels all animations that are currently playing.
     */
    public void cancelAll() {
        currentlyPlaying.clear();
        observers.clear();
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

    public List<Animatable> getCurrentlyPlaying() {
        return ImmutableList.copyOf(currentlyPlaying);
    }

    /**
     * Standard implementation of the {@link AnimationObserver} interface that
     * forwards events to all registered callbacks.
     */
    private static class MulticastAnimationObserver implements AnimationObserver {

        private List<Consumer<Animatable>> frameCallbacks;

        public MulticastAnimationObserver() {
            frameCallbacks = new ArrayList<>();
        }

        @Override
        public AnimationObserver onFrame(Consumer<Animatable> callback) {
            frameCallbacks.add(callback);
            return this;
        }

        public void onFrame(Animatable anim) {
            for (Consumer<Animatable> callback : frameCallbacks) {
                callback.accept(anim);
            }
        }
    }
}
