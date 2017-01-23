//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import java.awt.Color;
import java.util.Iterator;
import java.util.SortedSet;

import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.KeyFrame;
import nl.colorize.util.animation.Timeline;
import nl.colorize.util.swing.AnimatedColor;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for all classes in the animation framework.
 */
public class TestAnimation {
	
	private static final float EPSILON = 0.0001f;
	
	@Test
	public void testDiscreteInterpolation() {
		assertEquals(2f, Interpolation.DISCRETE.interpolate(2f, 9f, 0f), EPSILON);
		assertEquals(2f, Interpolation.DISCRETE.interpolate(2f, 9f, 0.5f), 0.01f);
		assertEquals(2f, Interpolation.DISCRETE.interpolate(2f, 9f, 0.9f), 0.01f);
		assertEquals(9f, Interpolation.DISCRETE.interpolate(2f, 9f, 1f), 0.01f);
	}

	@Test
	public void testLinearInterpolation() {
		assertEquals(3f, Interpolation.LINEAR.interpolate(3f, 5f, 0f), EPSILON);
		assertEquals(3.5f, Interpolation.LINEAR.interpolate(3f, 5f, 0.25f), EPSILON);
		assertEquals(4f, Interpolation.LINEAR.interpolate(3f, 5f, 0.5f), EPSILON);
		assertEquals(4.5f, Interpolation.LINEAR.interpolate(3f, 5f, 0.75f), EPSILON);
		assertEquals(5f, Interpolation.LINEAR.interpolate(3f, 5f, 1f), EPSILON);
	}
	
	@Test
	public void testEaseInterpolation() {
		assertEquals(3f, Interpolation.EASE.interpolate(3f, 5f, 0f), EPSILON);
		assertEquals(3.3125f, Interpolation.EASE.interpolate(3f, 5f, 0.25f), EPSILON);
		assertEquals(4f, Interpolation.EASE.interpolate(3f, 5f, 0.5f), EPSILON);
		assertEquals(4.6875f, Interpolation.EASE.interpolate(3f, 5f, 0.75f), EPSILON);
		assertEquals(5f, Interpolation.EASE.interpolate(3f, 5f, 1f), EPSILON);
	}
	
	@Test
	public void testCubicInterpolation() {
		assertEquals(0f, Interpolation.CUBIC.interpolate(0f, 1f, 0f), EPSILON);
		assertEquals(0.0625f, Interpolation.CUBIC.interpolate(0f, 1f, 0.25f), EPSILON);
		assertEquals(0.5f, Interpolation.CUBIC.interpolate(0f, 1f, 0.5f), EPSILON);
		assertEquals(0.9375f, Interpolation.CUBIC.interpolate(0f, 1f, 0.75f), EPSILON);
		assertEquals(1f, Interpolation.CUBIC.interpolate(0f, 1f, 1f), EPSILON);
	}
	
	@Test
	public void testQuadraticInterpolation() {
		assertEquals(0f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0f), EPSILON);
		assertEquals(0.125f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0.25f), EPSILON);
		assertEquals(0.5f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0.5f), EPSILON);
		assertEquals(0.875f, Interpolation.QUADRATIC.interpolate(0f, 1f, 0.75f), EPSILON);
		assertEquals(1f, Interpolation.QUADRATIC.interpolate(0f, 1f, 1f), EPSILON);
	}
	
	@Test
	public void testQuinticInterpolation() {
		assertEquals(0f, Interpolation.QUINTIC.interpolate(0f, 1f, 0f), EPSILON);
		assertEquals(0.0156f, Interpolation.QUINTIC.interpolate(0f, 1f, 0.25f), EPSILON);
		assertEquals(0.5f, Interpolation.QUINTIC.interpolate(0f, 1f, 0.5f), EPSILON);
		assertEquals(0.9844f, Interpolation.QUINTIC.interpolate(0f, 1f, 0.75f), EPSILON);
		assertEquals(1f, Interpolation.QUINTIC.interpolate(0f, 1f, 1f), EPSILON);
	}
	
	@Test
	public void testDuration() throws Exception {
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
		assertEquals(0f, timeline.getDelta(), 0.01f);
		timeline.setPlayhead(2f);
		assertEquals(2f, timeline.getPlayhead(), EPSILON);
		assertEquals(0.4f, timeline.getDelta(), 0.01f);
		timeline.reset();
		assertEquals(0f, timeline.getPlayhead(), EPSILON);
		timeline.end();
		assertEquals(5f, timeline.getPlayhead(), EPSILON);
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
		Timeline timeline = new Timeline();
		timeline.addKeyFrame(5f, 5000);
		timeline.setLoop(true);
		assertTrue(timeline.isLoop());
		timeline.setPlayhead(4.5f);
		assertEquals(4.5f, timeline.getPlayhead(), EPSILON);
		timeline.movePlayhead(0.5f);
		assertEquals(0f, timeline.getPlayhead(), EPSILON);
		timeline.setPlayhead(5f);
		assertEquals(0f, timeline.getPlayhead(), EPSILON);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testNoZeroTimeline() {
		Timeline timeline = new Timeline(Interpolation.LINEAR, false);
		timeline.setPlayhead(1f);
	}
	
	@Test
	public void testKeyFrames() {
		Timeline timeline = new Timeline();
		timeline.addKeyFrame(new KeyFrame(0f, 7f));
		timeline.addKeyFrame(3f, 10f);
		timeline.addKeyFrame(new KeyFrame(1f, 7.1f));
		timeline.addKeyFrame(new KeyFrame(0.5f, 2f));
		
		SortedSet<KeyFrame> keyframes = timeline.getKeyFrames();
		assertEquals(4, keyframes.size());
		Iterator<KeyFrame> iterator = keyframes.iterator();
		KeyFrame keyframe = iterator.next();
		assertEquals(0f, keyframe.getTime(), EPSILON);
		assertEquals(7f, keyframe.getValue(), EPSILON);
		keyframe = iterator.next();
		assertEquals(0.5f, keyframe.getTime(), EPSILON);
		assertEquals(2f, keyframe.getValue(), EPSILON);
		keyframe = iterator.next();
		assertEquals(1f, keyframe.getTime(), EPSILON);
		assertEquals(7.1f, keyframe.getValue(), 0.01f);
		keyframe = iterator.next();
		assertEquals(3f, keyframe.getTime(), EPSILON);
		assertEquals(10f, keyframe.getValue(), 0.01f);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testKeyFrameWithNegativePosition() {
		new KeyFrame(-1f, 0f);
	}
	
	@Test
	public void testKeyFrameInterpolation() {
		Timeline timeline = new Timeline(Interpolation.LINEAR, false);
		timeline.addKeyFrame(new KeyFrame(0f, 2f));
		timeline.addKeyFrame(new KeyFrame(1f, 3f));
		timeline.addKeyFrame(new KeyFrame(5f, 5f));
		
		timeline.setInterpolationMethod(Interpolation.LINEAR);
		assertEquals(Interpolation.LINEAR, timeline.getInterpolationMethod());
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
		timeline.removeKeyFrame(5f);
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
	
	@Test(expected = IllegalStateException.class)
	public void testNoKeyFrames() {
		Timeline timeline = new Timeline();
		timeline.getValue();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testAddKeyFrameTwice() {
		Timeline timeline = new Timeline();
		timeline.addKeyFrame(new KeyFrame(1f, 2f));
		timeline.addKeyFrame(new KeyFrame(1f, 3f));
	}
	
	@Test
	public void testAddKeyFrameWhileAnimating() {
		Timeline timeline = new Timeline();
		timeline.addKeyFrame(0f, 10f);
		timeline.addKeyFrame(1f, 20f);
		timeline.movePlayhead(1f);
		assertTrue(timeline.isCompleted());
		assertEquals(1f, timeline.getLastKeyFrame().getTime(), EPSILON);
		timeline.addKeyFrame(1.5f, 20f);
		assertEquals(1.5f, timeline.getLastKeyFrame().getTime(), EPSILON);
		assertFalse(timeline.isCompleted());
		timeline.movePlayhead(0.5f);
		assertTrue(timeline.isCompleted());
	}
	
	@Test
	public void testClosestKeyFrame() {
		Timeline timeline = new Timeline();
		timeline.addKeyFrame(0f, 0f);
		assertEquals(0f, timeline.getClosestKeyFrameBefore(10).getValue(), EPSILON);
		timeline.addKeyFrame(5f, 5f);
		assertEquals(5f, timeline.getClosestKeyFrameBefore(10).getValue(), EPSILON);
		timeline.addKeyFrame(11f, 11f);
		assertEquals(5f, timeline.getClosestKeyFrameBefore(10).getValue(), EPSILON);
		assertEquals(11f, timeline.getClosestKeyFrameBefore(11).getValue(), EPSILON);
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
		timeline.addKeyFrame(2f, 20f);
		timeline.setPlayhead(20f);
		
		assertTrue(timeline.hasKeyFrameAtPosition(2f));
		assertEquals(20f, timeline.getLastKeyFrame().getValue(), EPSILON);
		assertTrue(timeline.isCompleted());
		
		timeline.removeKeyFrame(2f);
		timeline.addKeyFrame(3f, 30f);
		
		assertFalse(timeline.hasKeyFrameAtPosition(2f));
		assertTrue(timeline.hasKeyFrameAtPosition(3f));
		assertEquals(30f, timeline.getLastKeyFrame().getValue(), EPSILON);
		assertFalse(timeline.isCompleted());
	}
	
	private void assertRGBA(Color color, int r, int g, int b, int a) {
		assertEquals(r, color.getRed());
		assertEquals(g, color.getGreen());
		assertEquals(b, color.getBlue());
		assertEquals(a, color.getAlpha());
	}
}
