//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@code Animator} class.
 */
public class AnimatorTest {
    
    private static final float EPSILON = 0.001f;  
    
    @Test
    public void testPlayAnimation() {
        MockAnim first = new MockAnim(1000f);
        MockAnim second = new MockAnim(1000f);
        
        ManualAnimator animator = new ManualAnimator();
        animator.play(first);
        animator.play(second);
        animator.performFrameUpdate(1f);
        animator.performFrameUpdate(2f);
        
        assertEquals(3f, first.totalTime, EPSILON);
        assertEquals(3f, second.totalTime, EPSILON);
    }
    
    @Test
    public void testPlayAnimationForFixedDuration() {
        MockAnim anim = new MockAnim(1.5f);
        
        ManualAnimator animator = new ManualAnimator();
        animator.play(anim);
        
        animator.performFrameUpdate(1f);
        assertEquals(1f, anim.totalTime, EPSILON);
        animator.performFrameUpdate(1f);
        assertEquals(2f, anim.totalTime, EPSILON);
        animator.performFrameUpdate(1f);
        assertEquals(2f, anim.totalTime, EPSILON);
    }
    
    @Test
    public void testPlayTimeline() {
        Timeline timeline = new Timeline(Interpolation.LINEAR);
        timeline.addKeyFrame(0f, 10f);
        timeline.addKeyFrame(1.5f, 20f);
        
        ManualAnimator animator = new ManualAnimator();
        animator.play(timeline);
        
        animator.performFrameUpdate(1f);
        assertEquals(1f, timeline.getPlayhead(), EPSILON);
        animator.performFrameUpdate(1f);
        assertEquals(1.5f, timeline.getPlayhead(), EPSILON);
        assertTrue(timeline.isCompleted());
    }
    
    @Test
    public void testCancelAnimation() {
        MockAnim anim = new MockAnim(1000f);
        
        ManualAnimator animator = new ManualAnimator();
        animator.play(anim);
        
        animator.performFrameUpdate(1f);
        assertEquals(1f, anim.totalTime, EPSILON);
        animator.cancel(anim);
        animator.performFrameUpdate(1f);
        assertEquals(1f, anim.totalTime, EPSILON);
    }
    
    @Test
    public void testOverrideAnimationCurrentlyActive() {
        final MockAnim first = new MockAnim(1000f);
        final MockAnim second = new MockAnim(100f);
        
        Animator animator = new ManualAnimator() {
            @Override
            protected boolean isCurrentlyActive(Animatable anim) {
                return anim == second;
            }
        };
        animator.start();
        animator.play(first);
        animator.play(second);
        animator.performFrameUpdate(1f);
        
        assertEquals(0f, first.totalTime, EPSILON);
        assertEquals(1f, second.totalTime, EPSILON);
    }

    @Test
    public void testObserveFrameUpdates() {
        List<Animatable> received = new ArrayList<>();

        Animator animator = new ManualAnimator();
        AnimationObserver observer = animator.play(new MockAnim(1f));
        observer.onFrame(anim -> received.add(anim));

        animator.performFrameUpdate(0.2f);
        animator.performFrameUpdate(0.5f);
        assertEquals(2, received.size());

        animator.performFrameUpdate(0.5f);
        assertEquals(3, received.size());

        animator.performFrameUpdate(0.5f);
        assertEquals(3, received.size());
    }

    @Test
    public void testObserveComplete() {
        List<Animatable> received = new ArrayList<>();

        Animator animator = new ManualAnimator();
        AnimationObserver observer = animator.play(new MockAnim(1f));
        observer.onComplete(anim -> received.add(anim));

        animator.performFrameUpdate(0.4f);
        assertEquals(0, received.size());

        animator.performFrameUpdate(0.7f);
        assertEquals(1, received.size());

        animator.performFrameUpdate(0.7f);
        assertEquals(1, received.size());
    }

    @Test
    public void testRemoveAnimationDuringPlayback() {
        Animator animator = new ManualAnimator();

        Animatable anim1 = deltaTime -> {};
        Animatable anim2 = deltaTime -> animator.cancel(anim1);

        animator.play(anim1);
        animator.play(anim2);
        animator.performFrameUpdate(1f);

        assertEquals(1, animator.getCurrentlyPlaying().size());
        assertEquals(anim2, animator.getCurrentlyPlaying().get(0));
    }

    @Test
    public void testTimedAnimation() {
        Animator animator = new ManualAnimator();
        animator.start();
        animator.play(TimedAnimation.from(deltaTime -> {}, 2f));
        animator.performFrameUpdate(1f);
        animator.performFrameUpdate(1f);
        animator.performFrameUpdate(1f);

        assertEquals(0, animator.getCurrentlyPlaying().size());
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
        
        private float totalTime;
        private float duration;

        public MockAnim(float duration) {
            this.totalTime = 0f;
            this.duration = duration;
        }

        @Override
        public void onFrame(float deltaTime) {
            totalTime += deltaTime;
        }

        @Override
        public boolean isCompleted() {
            return totalTime >= duration;
        }
    }
}
