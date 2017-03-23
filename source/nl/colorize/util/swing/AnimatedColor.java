//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.Color;

import nl.colorize.util.animation.Animatable;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;

/**
 * Color with red, green, blue, and alpha components that change over time. 
 */
public class AnimatedColor extends Color implements Animatable {
	
	private Timeline r;
	private Timeline g;
	private Timeline b;
	private Timeline a;
	
	private static final long serialVersionUID = 11;
	
	/**
	 * Creates a new {@code AnimatedColor} that will start out with the RGBA values
	 * of {@code startColor}. Animation can be added to the color by adding key
	 * frames.
	 */
	public AnimatedColor(Color startColor) {
		this(startColor, Interpolation.LINEAR);
	}
	
	public AnimatedColor(Color startColor, Interpolation interpolationMethod) {
		super(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha());
		
		r = initTimeline(startColor.getRed(), interpolationMethod);
		g = initTimeline(startColor.getGreen(), interpolationMethod);
		b = initTimeline(startColor.getBlue(), interpolationMethod);
		a = initTimeline(startColor.getAlpha(), interpolationMethod);
	}

	/**
	 * Creates a new color with the specified RGBA values. The actual appearance
	 * of the color will be determined by the state of the animation.
	 */
	public AnimatedColor(Color startColor, Color endColor, int duration) {
		this(startColor);
		addKeyFrame(duration, endColor);
	}
	
	private Timeline initTimeline(int initialValue, Interpolation interpolationMethod) {
		Timeline timeline = new Timeline(interpolationMethod);
		timeline.addKeyFrame(0, initialValue);
		return timeline;
	}
	
	/**
	 * Adds a key frame that describes the RGBA values of this color at the
	 * specified point in time.
	 */
	public void addKeyFrame(float time, Color rgba) {
		r.addKeyFrame(time, rgba.getRed());
		g.addKeyFrame(time, rgba.getGreen());
		b.addKeyFrame(time, rgba.getBlue());
		a.addKeyFrame(time, rgba.getAlpha());
	}
	
	public void onFrame(float deltaTime) {
		r.movePlayhead(deltaTime);
		g.movePlayhead(deltaTime);
		b.movePlayhead(deltaTime);
		a.movePlayhead(deltaTime);
	}
	
	public void reset() {
		r.reset();
		g.reset();
		b.reset();
		a.reset();
	}
	
	public void end() {
		r.end();
		g.end();
		b.end();
		a.end();
	}
	
	@Override
	public int getRed() {
		return Math.round(r.getValue());
	}
	
	@Override
	public int getGreen() {
		return Math.round(g.getValue());
	}
	
	@Override
	public int getBlue() {
		return Math.round(b.getValue());
	}

	@Override
	public int getAlpha() {
		return Math.round(a.getValue());
	}

	@Override
	public int getRGB() {
		return ((getAlpha() & 0xFF) << 24) 
				| ((getRed() & 0xFF) << 16) 
				| ((getGreen() & 0xFF) << 8) 
				| ((getBlue() & 0xFF) << 0);
	}
	
	@Override
	public int hashCode() {
		return getRGB();
	}
}
