//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@code Animator} class.
 */
public class TestAnimator {
	
	private static final float EPSILON = 0.001f;  
	
	@Test
	public void testPlayAnimation() {
		MockAnim first = new MockAnim();
		MockAnim second = new MockAnim();
		
		ManualAnimator animator = new ManualAnimator();
		animator.play(first);
		animator.play(second);
		animator.doFrameUpdate(1f);
		animator.doFrameUpdate(2f);
		
		assertEquals(3f, first.totalTime, EPSILON);
		assertEquals(3f, second.totalTime, EPSILON);
	}
	
	@Test
	public void testPlayAnimationForFixedDuration() {
		MockAnim anim = new MockAnim();
		
		ManualAnimator animator = new ManualAnimator();
		animator.play(anim, 1.5f);
		
		animator.doFrameUpdate(1f);
		assertEquals(1f, anim.totalTime, EPSILON);
		animator.doFrameUpdate(1f);
		assertEquals(2f, anim.totalTime, EPSILON);
		animator.doFrameUpdate(1f);
		assertEquals(2f, anim.totalTime, EPSILON);
	}
	
	@Test
	public void testPlayTimeline() {
		Timeline timeline = new Timeline();
		timeline.addKeyFrame(0f, 10f);
		timeline.addKeyFrame(1.5f, 20f);
		
		ManualAnimator animator = new ManualAnimator();
		animator.play(timeline);
		
		animator.doFrameUpdate(1f);
		assertEquals(1f, timeline.getPlayhead(), EPSILON);
		animator.doFrameUpdate(1f);
		assertEquals(1.5f, timeline.getPlayhead(), EPSILON);
		assertTrue(timeline.isCompleted());
	}
	
	@Test
	public void testCancelAnimation() {
		MockAnim anim = new MockAnim();
		
		ManualAnimator animator = new ManualAnimator();
		animator.play(anim, 10f);
		
		animator.doFrameUpdate(1f);
		assertEquals(1f, anim.totalTime, EPSILON);
		animator.cancel(anim);
		animator.doFrameUpdate(1f);
		assertEquals(1f, anim.totalTime, EPSILON);
	}
	
	@Test
	public void testRegisterListeners() {
		Timeline timeline = new Timeline();
		timeline.addKeyFrame(0f, 0f);
		timeline.addKeyFrame(2f, 10f);
		
		final AtomicInteger frameCount = new AtomicInteger();
		final AtomicInteger completionCount = new AtomicInteger();
		
		ManualAnimator animator = new ManualAnimator();
		animator.play(timeline);
		animator.registerFrameUpdateListener(timeline, new AnimationListener<Timeline>() {
			public void onAnimationEvent(Timeline t) {
				frameCount.set(frameCount.get() + 1);
			}
		});
		animator.registerCompletionListener(timeline, new AnimationListener<Timeline>() {
			public void onAnimationEvent(Timeline t) {
				completionCount.set(completionCount.get() + 1);
			}
		});
		animator.doFrameUpdate(1f);
		animator.doFrameUpdate(1f);
		animator.doFrameUpdate(1f);
		
		assertEquals(2, frameCount.get());
		assertEquals(1, completionCount.get());
		assertTrue(timeline.isCompleted());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCanOnlyRegisterListenerForPlayingAnimation() {
		ManualAnimator animator = new ManualAnimator();
		animator.registerCompletionListener(new MockAnim(), new AnimationListener<MockAnim>() {
			public void onAnimationEvent(MockAnim anim) {
				// No-op
			}
		});
	}
	
	@Test
	public void testOverrideAnimationCurrentlyActive() {
		final MockAnim first = new MockAnim();
		final MockAnim second = new MockAnim();
		
		Animator animator = new ManualAnimator() {
			@Override
			protected boolean isCurrentlyActive(Animatable anim) {
				return anim == second;
			}
		};
		animator.start();
		animator.play(first);
		animator.play(second);
		animator.doFrameUpdate(1f);
		
		assertEquals(0f, first.totalTime, EPSILON);
		assertEquals(1f, second.totalTime, EPSILON);
	}

	/**
	 * Implementation of an {@code Animator} for testing purposes.
	 */
	private static class ManualAnimator extends Animator {
		
		@Override
		public void start() {
		}
		
		@Override
		public void stop() {
			cancelAll();
		}
	}
	
	/**
	 * Keeps track of the number of frame updates received.
	 */
	private static class MockAnim implements Animatable {
		
		private float totalTime = 0f;

		public void onFrame(float deltaTime) {
			totalTime += deltaTime;
		}
	}
}
