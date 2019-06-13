//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Schedules animations and drives them by performing frame updates at regular
 * intervals. This class only provides a basic mechanism for playing
 * animations, subclasses need to link this mechanism to the underlying platform.
 * <p>
 * Animations receive frame updates through the {@link Animatable} interface.
 * All animations are played using the same framerate. In practice, the actual
 * framerate may deviate from the targeted framerate, depending on the amount of
 * work that is performed during frame updates and the performance of the
 * underlying platform. For this reason, all animations receive the exact delta
 * time since the last frame updates, so that animations can look consistent
 * even when there are small deviations in the time between frame updates.
 */
public abstract class Animator {

    private List<Animatable> currentlyPlaying;
    private Multimap<Animatable, MulticastAnimationObserver> observers;

    /**
     * Creates a new animator, but will not start playing animations until
     * {@link #start()} is called.
     */
    public Animator() {
        this.currentlyPlaying = new ArrayList<>();
        this.observers = ArrayListMultimap.create();
    }
    
    /**
     * Starts this {@code Animator}. Scheduled animations will not receive frame
     * updates until this method is called.
     */
    public abstract void start();
    
    /**
     * Stops this {@code Animator}. Animations that are currently playing will
     * be cancelled.
     */
    public abstract void stop();

    /**
     * Performs a frame update for all animations that are currently playing.
     * Subclasses should call this method once per frame while the animator
     * is running.
     * @param deltaTime Time since the last frame upate, in seconds.
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

            if (anim instanceof TimedAnimatable) {
                checkAnimationCompleted((TimedAnimatable) anim);
            }
        }
    }

    private void checkAnimationCompleted(TimedAnimatable anim) {
        if (anim.isCompleted()) {
            for (MulticastAnimationObserver observer : observers.get(anim)) {
                observer.onComplete(anim);
            }

            currentlyPlaying.remove(anim);
            observers.removeAll(anim);
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

    public List<Animatable> getCurrentlyPlaying() {
        return ImmutableList.copyOf(currentlyPlaying);
    }

    public List<AnimationObserver> getObservers(Animatable anim) {
        return ImmutableList.copyOf(observers.get(anim));
    }

    /**
     * Standard implementation of the {@link AnimationObserver} interface that
     * forwards events to all registered callbacks.
     */
    private static class MulticastAnimationObserver implements AnimationObserver {

        private List<Consumer<Animatable>> frameCallbacks;
        private List<Consumer<Animatable>> completeCallbacks;

        public MulticastAnimationObserver() {
            frameCallbacks = new ArrayList<>();
            completeCallbacks = new ArrayList<>();
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

        @Override
        public AnimationObserver onComplete(Consumer<Animatable> callback) {
            completeCallbacks.add(callback);
            return this;
        }

        public void onComplete(Animatable anim) {
            for (Consumer<Animatable> callback : completeCallbacks) {
                callback.accept(anim);
            }
        }
    }
}
