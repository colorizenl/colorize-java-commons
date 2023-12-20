//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import nl.colorize.util.Stopwatch;
import nl.colorize.util.swing.AnimatedColor;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimelineTest {

    private static final float EPSILON = 0.001f;

    @Test
    public void testDuration() {
        Timeline timeline = new Timeline();
        assertEquals(0f, timeline.getDuration(), EPSILON);
        timeline.addKeyFrame(0f, 10f);
        assertEquals(0f, timeline.getDuration(), EPSILON);
        timeline.addKeyFrame(0.3f, 20f);
        assertEquals(0.3f, timeline.getDuration(), EPSILON);
        timeline.addKeyFrame(0.7f, 30f);
        assertEquals(0.7f, timeline.getDuration(), EPSILON);
    }

    @Test
    public void testPlayhead() {
        Timeline timeline = new Timeline(Interpolation.LINEAR, false);
        timeline.addKeyFrame(5f, 0);
        assertEquals(0f, timeline.getPlayhead(), EPSILON);
        assertEquals(0f, timeline.getDelta(), EPSILON);
        assertTrue(timeline.isAtStart());

        timeline.setPlayhead(2f);
        assertEquals(2f, timeline.getPlayhead(), EPSILON);
        assertEquals(0.4f, timeline.getDelta(), EPSILON);
        assertFalse(timeline.isAtStart());

        timeline.reset();
        assertEquals(0f, timeline.getPlayhead(), EPSILON);
        assertEquals(0f, timeline.getDelta(), EPSILON);

        timeline.end();
        assertEquals(5f, timeline.getPlayhead(), EPSILON);
        assertEquals(1f, timeline.getDelta(), EPSILON);
    }

    @Test
    public void testPlayheadOutOfBounds() {
        Timeline timeline = new Timeline(Interpolation.LINEAR, false);
        timeline.addKeyFrame(5f, 5000);
        timeline.movePlayhead(10f);
        assertEquals(5f, timeline.getPlayhead(), EPSILON);
        timeline.movePlayhead(-10f);
        assertEquals(0, timeline.getPlayhead(), EPSILON);
        timeline.setPlayhead(-2f);
        assertEquals(0, timeline.getPlayhead(), EPSILON);
        timeline.setPlayhead(300f);
        assertEquals(5f, timeline.getPlayhead(), EPSILON);
    }

    @Test
    public void testLoopingTimeline() {
        Timeline timeline = new Timeline(Interpolation.LINEAR, true);
        timeline.addKeyFrame(5f, 5000);
        assertTrue(timeline.isLoop());
        timeline.setPlayhead(4.5f);
        assertEquals(4.5f, timeline.getPlayhead(), EPSILON);
        timeline.movePlayhead(0.5f);
        assertEquals(5f, timeline.getPlayhead(), EPSILON);
        timeline.movePlayhead(0.1f);
        assertEquals(0.1f, timeline.getPlayhead(), EPSILON);
    }

    @Test
    public void testNoZeroTimeline() {
        Timeline timeline = new Timeline(Interpolation.LINEAR, false);
        timeline.setPlayhead(1f);

        assertEquals(0f, timeline.getPlayhead(), EPSILON);
    }

    @Test
    public void testKeyFrames() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(new KeyFrame(0f, 7f));
        timeline.addKeyFrame(3f, 10f);
        timeline.addKeyFrame(new KeyFrame(1f, 7.1f));
        timeline.addKeyFrame(new KeyFrame(0.5f, 2f));

        timeline.setPlayhead(0f);
        assertEquals(7f, timeline.getValue(), EPSILON);

        timeline.setPlayhead(0.5f);
        assertEquals(2f, timeline.getValue(), EPSILON);

        timeline.setPlayhead(1f);
        assertEquals(7.1f, timeline.getValue(), 0.01f);

        timeline.setPlayhead(3f);
        assertEquals(10f, timeline.getValue(), 0.01f);
    }

    @Test
    public void testKeyFrameWithNegativePosition() {
        assertThrows(IllegalArgumentException.class, () -> new KeyFrame(-1f, 0f));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testKeyFrameInterpolation() {
        Timeline timeline = new Timeline(Interpolation.LINEAR, false);
        timeline.addKeyFrame(new KeyFrame(0f, 2f));
        timeline.addKeyFrame(new KeyFrame(1f, 3f));

        KeyFrame last = new KeyFrame(5f, 5f);
        timeline.addKeyFrame(last);
        assertEquals(2f, timeline.getValue(), 0.01f);

        timeline.setPlayhead(0.5f);
        assertEquals(2.5f, timeline.getValue(), 0.01f);

        timeline.setPlayhead(1f);
        assertEquals(3f, timeline.getValue(), 0.01f);

        timeline.setPlayhead(2f);
        assertEquals(3.5f, timeline.getValue(), 0.01f);

        timeline.setPlayhead(3f);
        assertEquals(4f, timeline.getValue(), 0.01f);

        timeline.setPlayhead(5f);
        assertEquals(5f, timeline.getValue(), 0.01f);

        timeline.removeKeyFrame(last);
        assertEquals(3f, timeline.getValue(), 0.01f);
    }

    @Test
    public void testValueBeforeFirstKeyFrame() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(new KeyFrame(2f, 10f));
        assertEquals(10f, timeline.getValue(), 0.01f);

        timeline.addKeyFrame(new KeyFrame(0f, 7f));
        assertEquals(7f, timeline.getValue(), 0.01f);
    }

    @Test
    public void testBeforeTheFirstKeyFrame() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(new KeyFrame(1f, 5f));
        timeline.addKeyFrame(new KeyFrame(2f, 10f));

        timeline.setPlayhead(0f);
        assertEquals(5f, timeline.getValue(), 0.01f);
        timeline.setPlayhead(0.5f);
        assertEquals(5f, timeline.getValue(), 0.01f);
    }

    @Test
    public void testNoKeyFrames() {
        Timeline timeline = new Timeline();

        assertEquals(0f, timeline.getValue(), EPSILON);
    }

    @Test
    public void testAddKeyFrameTwice() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(new KeyFrame(1f, 2f));

        assertThrows(IllegalStateException.class, () -> timeline.addKeyFrame(new KeyFrame(1f, 3f)));
    }

    @Test
    public void testAddKeyFrameWhileAnimating() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(0f, 10f);
        timeline.addKeyFrame(1f, 20f);
        timeline.movePlayhead(1f);

        assertTrue(timeline.isCompleted());
        assertEquals(20f, timeline.getValue(), EPSILON);

        timeline.addKeyFrame(1.5f, 20f);
        assertFalse(timeline.isCompleted());
        assertEquals(20f, timeline.getValue(), EPSILON);

        timeline.movePlayhead(0.5f);
        assertTrue(timeline.isCompleted());
    }

    @Test
    public void testKeyFrameComparator() {
        KeyFrame kf = new KeyFrame(1f, 10f);
        assertTrue(kf.compareTo(new KeyFrame(2f, 20f)) < 0);
        assertTrue(kf.compareTo(new KeyFrame(1f, 20f)) == 0);
        assertTrue(kf.compareTo(new KeyFrame(0.5f, 20f)) > 0);
    }

    @Test
    public void testAnimatedColor() {
        AnimatedColor nonChangingColor = new AnimatedColor(Color.ORANGE);
        assertRGBA(nonChangingColor, 255, 200, 0, 255);
        nonChangingColor.onFrame(1);
        assertRGBA(nonChangingColor, 255, 200, 0, 255);

        AnimatedColor linearColor = new AnimatedColor(Color.RED, Color.BLUE, 2);
        assertRGBA(linearColor, 255, 0, 0, 255);
        linearColor.onFrame(1);
        assertRGBA(linearColor, 128, 0, 128, 255);
        linearColor.onFrame(1);
        assertRGBA(linearColor, 0, 0, 255, 255);
        linearColor.onFrame(1);
        assertRGBA(linearColor, 0, 0, 255, 255);
    }

    @Test
    public void testAnimatedColorWithKeyFrames() {
        AnimatedColor colorWithKeyFrames = new AnimatedColor(Color.RED);
        colorWithKeyFrames.addKeyFrame(3f, new Color(255, 0, 0, 105));
        colorWithKeyFrames.addKeyFrame(4f, new Color(255, 0, 0, 5));
        assertRGBA(colorWithKeyFrames, 255, 0, 0, 255);
        colorWithKeyFrames.onFrame(1);
        assertRGBA(colorWithKeyFrames, 255, 0, 0, 205);
        colorWithKeyFrames.onFrame(1);
        colorWithKeyFrames.onFrame(1);
        assertRGBA(colorWithKeyFrames, 255, 0, 0, 105);
        colorWithKeyFrames.onFrame(1);
        assertRGBA(colorWithKeyFrames, 255, 0, 0, 5);
        colorWithKeyFrames.onFrame(1);
        assertRGBA(colorWithKeyFrames, 255, 0, 0, 5);
    }

    @Test
    public void testReplaceKeyFrame() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(1f, 10f);
        KeyFrame middle = new KeyFrame(2f, 20f);
        timeline.addKeyFrame(middle);
        timeline.setPlayhead(20f);
        timeline.removeKeyFrame(middle);
        timeline.addKeyFrame(3f, 30f);
        timeline.end();

        String expected = """
            Timeline
                1.0: 10.0
                3.0: 30.0
            """;

        assertEquals(expected, timeline.toString());
    }

    @Test
    void testTimelineWithoutKeyFrameAtZero() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(new KeyFrame(1f, 10f));

        assertEquals(10f, timeline.getValue(), EPSILON);

        timeline.setPlayhead(0.5f);
        assertEquals(10f, timeline.getValue(), EPSILON);

        timeline.setPlayhead(1f);
        assertEquals(10f, timeline.getValue(), EPSILON);
    }

    @Test
    void insertKeyFramesInOrder() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(1.5f, 1f);
        timeline.addKeyFrame(2f, 2f);
        timeline.addKeyFrame(2.5f, 3f);

        String expected = """
            Timeline
                1.5: 1.0
                2.0: 2.0
                2.5: 3.0
            """;

        assertEquals(expected, timeline.toString());
    }

    @Test
    void insertKeyFramesOutOfOrder() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(4f, 4f);
        timeline.addKeyFrame(1.5f, 1f);
        timeline.addKeyFrame(2.5f, 3f);
        timeline.addKeyFrame(2f, 2f);

        String expected = """
            Timeline
                1.5: 1.0
                2.0: 2.0
                2.5: 3.0
                4.0: 4.0
            """;

        assertEquals(expected, timeline.toString());
    }

    @Test
    void cannotInsertSameKeyFrameTwice() {
        Timeline timeline = new Timeline();
        timeline.addKeyFrame(1.2f, 1f);

        assertThrows(IllegalStateException.class, () -> timeline.addKeyFrame(1.2f, 2f));
    }

    @Test
    void emptyTimelineIsNotConsideredCompleted() {
        Timeline empty = new Timeline();

        assertEquals(0f, empty.getPlayhead());
        assertEquals(0f, empty.getDuration());
        assertFalse(empty.isCompleted());
    }

    @Test
    void performanceForHugeTimeline() {
        Timeline timeline = new Timeline();
        Stopwatch timer = new Stopwatch();

        for (int i = 0; i < 1_000_000; i++) {
            timeline.addKeyFrame(i, i);
        }

        for (int i = 0; i < 1_000_000; i++) {
            timeline.setPlayhead(i);
        }

        assertTrue(timer.tock() <= 2000L,
            "Insufficient timeline performance: " + timer.tock());
    }

    @Test
    void reverseTimeline() {
        Timeline timeline = new Timeline()
            .addKeyFrame(0f, 1f)
            .addKeyFrame(1f, 2f)
            .addKeyFrame(2f, 4f)
            .addKeyFrame(3f, 8f);

        String expected = """
            Timeline
                0.0: 8.0
                1.0: 4.0
                2.0: 2.0
                3.0: 1.0
            """;

        assertEquals(expected.trim(), timeline.reverse().toString().trim());
    }

    private void assertRGBA(Color color, int r, int g, int b, int a) {
        assertEquals(r, color.getRed());
        assertEquals(g, color.getGreen());
        assertEquals(b, color.getBlue());
        assertEquals(a, color.getAlpha());
    }
}
