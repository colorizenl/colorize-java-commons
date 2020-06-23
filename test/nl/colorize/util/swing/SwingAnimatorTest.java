//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import nl.colorize.util.animation.Animatable;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SwingAnimatorTest {

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

        SwingAnimator animator = new ManualAnimator() {
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
    public void testRemoveAnimationDuringPlayback() {
        SwingAnimator animator = new ManualAnimator();

        Animatable anim1 = deltaTime -> {};
        Animatable anim2 = deltaTime -> animator.cancel(anim1);

        animator.play(anim1);
        animator.play(anim2);
        animator.performFrameUpdate(1f);

        assertEquals(1, animator.getCurrentlyPlaying().size());
        assertEquals(anim2, animator.getCurrentlyPlaying().get(0));
    }

    /**
     * Implementation of an {@code Animator} for testing purposes.
     */
    private static class ManualAnimator extends SwingAnimator {

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

        public boolean isCompleted() {
            return totalTime >= duration;
        }

        public void reset() {
        }
    }
}
