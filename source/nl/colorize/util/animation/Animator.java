//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

/**
 * Schedules animations and drives them by performing frame updates at regular
 * time intervals. Apart from driving animations, {@code Animator}s can register
 * a number of listeners that will be notified of animations being completed and 
 * animations being performed.
 */
public abstract class Animator {

	private List<AnimationInfo> currentlyPlaying;
	
	public Animator() {
		currentlyPlaying = new LockedIterationList<AnimationInfo>();
	}
	
	/**
	 * Starts this {@code Animator}. Scheduled animations will not receive frame
	 * updates until this method is called.
	 */
	public abstract void start();
	
	/**
	 * Stops this {@code Animator}. Animations that are currently playing will
	 * be cancelled. No frame updates will be performed until {@link #start()}
	 * is called again.
	 */
	public abstract void stop();
	
	/**
	 * Performs a frame update for all animations that are currently playing.
	 * Subclasses should call this method once per frame while this
	 * {@code Animator} is running.
	 * @param deltaTime Time since the last frame upate, in seconds.
	 */
	protected final void doFrameUpdate(float deltaTime) {
		for (AnimationInfo animInfo  : currentlyPlaying) {
			doFrameUpdate(animInfo, deltaTime);
		}
	}
	
	private void doFrameUpdate(AnimationInfo animInfo, float deltaTime) {
		if (isCurrentlyActive(animInfo.anim)) {
			animInfo.playhead += deltaTime;
			animInfo.anim.onFrame(deltaTime);
			notifyListeners(animInfo.anim, animInfo.frameUpdateListeners);
			
			if (isCompleted(animInfo)) {
				currentlyPlaying.remove(animInfo);
				notifyListeners(animInfo.anim, animInfo.completionListeners);
			}
		}
	}

	private boolean isCompleted(AnimationInfo animInfo) {
		return animInfo.duration > 0f && animInfo.playhead >= animInfo.duration;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void notifyListeners(Animatable anim, List<AnimationListener<?>> listeners) {
		for (AnimationListener listener : listeners) {
			listener.onAnimationEvent(anim);
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
	 * Schedules an animation to be played for as long as this {@code Animator}
	 * is running.
	 */
	public void play(Animatable anim) {
		currentlyPlaying.add(new AnimationInfo(anim, 0f));
	}
	
	/**
	 * Schedules an animation to be played for the specified duration.
	 * @param duration Time the animation should be played, in seconds.
	 */
	public void play(Animatable anim, float duration) {
		currentlyPlaying.add(new AnimationInfo(anim, duration));
	}
	
	/**
	 * Schedules a timeline to be played until it is completed.
	 */
	public void play(Timeline timeline) {
		currentlyPlaying.add(new AnimationInfo(timeline, timeline.getDuration()));
	}
	
	/**
	 * Cancels an animation that is currently playing.
	 */
	public void cancel(Animatable anim) {
		AnimationInfo animInfo = lookupAnimationInfo(anim);
		if (animInfo != null) {
			currentlyPlaying.remove(animInfo);
		}
	}
	
	/**
	 * Cancels all animations that are currently playing.
	 */
	public void cancelAll() {
		currentlyPlaying.clear();
	}
	
	private AnimationInfo lookupAnimationInfo(Animatable anim) {
		for (AnimationInfo animInfo : currentlyPlaying) {
			if (animInfo.anim == anim) {
				return animInfo;
			}
		}
		return null;
	}
	
	/**
	 * Notifies the listener after the specified animation receives a frame update.
	 * @throws IllegalArgumentException if the animation is not currenly playing.
	 */
	public <T extends Animatable> void registerFrameUpdateListener(T forAnim, AnimationListener<T> listener) {
		AnimationInfo animInfo = lookupAnimationInfo(forAnim);
		if (animInfo == null) {
			throw new IllegalArgumentException("Animation is not currenly playing");
		}
		animInfo.frameUpdateListeners.add(listener);
	}
	
	/**
	 * Notifies the listener after the specified animation has been completed.
	 * Note that listeners are not notified when the animation is cancelled.
	 * @throws IllegalArgumentException if the animation is not currenly playing.
	 */
	public <T extends Animatable> void registerCompletionListener(T forAnim, AnimationListener<T> listener) {
		AnimationInfo animInfo = lookupAnimationInfo(forAnim);
		if (animInfo == null) {
			throw new IllegalArgumentException("Animation is not currenly playing");
		}
		animInfo.completionListeners.add(listener);
	}

	/**
	 * Stores information about a scheduled animation, such as the duration it
	 * is going to be played.
	 */
	private static class AnimationInfo {
		
		private Animatable anim;
		private float playhead;
		private float duration;
		private List<AnimationListener<?>> frameUpdateListeners;
		private List<AnimationListener<?>> completionListeners;
		
		public AnimationInfo(Animatable anim, float duration) {
			this.anim = anim;
			this.playhead = 0f;
			this.duration = duration;
			frameUpdateListeners = new LockedIterationList<AnimationListener<?>>();
			completionListeners = new LockedIterationList<AnimationListener<?>>();
		}
	}
	
	/**
	 * List that iterates over an immutable snapshot instead of the "live" list,
	 * meaning the list can be modified during iteration without causing a
	 * {@link java.util.ConcurrentModificationException}.
	 */
	private static class LockedIterationList<E> extends ForwardingList<E> {
		
		private List<E> elements;
		
		public LockedIterationList() {
			elements = new ArrayList<E>();
		}
		
		@Override
		protected List<E> delegate() {
			return elements;
		}
		
		@Override
		public Iterator<E> iterator() {
			ImmutableList<E> immutableSnapshot = ImmutableList.copyOf(elements);
			return immutableSnapshot.iterator();
		}
	}
}
