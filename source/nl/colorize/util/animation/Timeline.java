//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Animates the value of a property over time by interpolating between key
 * frames. The duration of the animation is equal to the position of the last
 * key fram. Timelines can end once its duration has been reached, or loop to
 * restart playback from the beginning.
 */
public class Timeline implements Animatable {
    
    private List<KeyFrame> keyFrames;
    private Interpolation interpolationMethod;
    private boolean loop;

    private double playhead;
    private int nextKeyFrameIndex;

    private static final double EPSILON = 0.001;

    /**
     * Creates a timeline that will interpolate between key frames using the
     * specified interpolation method.
     */
    public Timeline(Interpolation interpolationMethod, boolean loop) {
        this.keyFrames = new ArrayList<>();
        this.interpolationMethod = interpolationMethod;
        this.loop = loop;

        this.playhead = 0.0;
        this.nextKeyFrameIndex = 0;
    }
    
    /**
     * Creates a timeline that uses the specified interpolation method and
     * will not loop.
     */
    public Timeline(Interpolation interpolationMethod) {
        this(interpolationMethod, false);
    }
    
    /**
     * Creates a timeline that uses linear interpolation and will not loop.
     */
    public Timeline() {
        this(Interpolation.LINEAR, false);
    }
    
    /**
     * Moves the position of the playhead to the specified value, in seconds.
     * The playhead is restricted to the range between zero and the timeline's
     * duration as returned by {@link #getDuration()}.
     */
    public void setPlayhead(double position) {
        double duration = getDuration();
        
        if (loop && position > duration) {
            playhead = position % duration;
            scanNextKeyFrame(true);
        } else if (position >= playhead) {
            playhead = Math.min(position, duration);
            scanNextKeyFrame(false);
        } else {
            playhead = Math.max(position, 0f);
            scanNextKeyFrame(true);
        }
    }

    /**
     * Scans the timeline for the position of the next key frame, relative
     * to the current position of the playhead. If we know the playhead is
     * moving forward, we don't need to scan every single key frame, which
     * helps performance for huge timelines.
     */
    private void scanNextKeyFrame(boolean full) {
        if (keyFrames.isEmpty()) {
            nextKeyFrameIndex = 0;
            return;
        }

        int start = full ? 0 : nextKeyFrameIndex;

        for (int i = start; i < keyFrames.size(); i++) {
            nextKeyFrameIndex = i;
            if (keyFrames.get(i).time() > playhead) {
                break;
            }
        }
    }

    /**
     * Moves the playhead by the specified amount, in seconds. Passing a
     * negative value will move the playhead backwards. The playhead is
     * restricted to the range between zero and the timeline's duration as
     * returned by {@link #getDuration()}.
     *
     * @return The new position of this timeline's playhead.
     */
    public double movePlayhead(double deltaTime) {
        setPlayhead(playhead + deltaTime);
        return playhead;
    }

    /**
     * Moves the playhead by the specified amount, in seconds. Passing a
     * negative value will move the playhead backwards. The playhead is
     * restricted to the range between zero and the timeline's duration as
     * returned by {@link #getDuration()}.
     */
    @Override
    public void onFrame(double deltaTime) {
        movePlayhead(deltaTime);
    }
    
    /**
     * Moves the playhead back to the start of the timeline.
     */
    public void reset() {
        setPlayhead(0.0);
    }
    
    /**
     * Moves the playhead to the end of the timeline.
     */
    public void end() {
        setPlayhead(getDuration());
    }

    /**
     * Returns the current position of the playhead, in seconds. The value of
     * the playhead will exist somewhere in the range between zero and the
     * value of {@link #getDuration()}.
     */
    public double getPlayhead() {
        return playhead;
    }

    /**
     * Returns this timeline's duration, which is based on the position of the
     * last key frame. If the timeline does not contain any key frames, this
     * method will return zero.
     */
    public double getDuration() {
        if (keyFrames.isEmpty()) {
            return 0.0;
        }

        KeyFrame last = keyFrames.getLast();
        return last.time();
    }
    
    /**
     * Returns the position of the playhead as a number between 0 and 1,
     * where 0 is the beginning of the timeline and 1 is the timeline's
     * duration.
     */
    public double getDelta() {
        double duration = getDuration();

        if (playhead <= 0.0) {
            return 0.0;
        } else if (playhead >= duration) {
            return 1.0;
        } else {
            return playhead / duration;
        }
    }
    
    /**
     * Returns true if the playhead is positioned at the start of the timeline.
     */
    public boolean isAtStart() {
        return playhead <= EPSILON;
    }
    
    /**
     * Returns true if the playhead has reached the end of the timeline. 
     */
    public boolean isCompleted() {
        if (keyFrames.isEmpty()) {
            return false;
        }

        return playhead >= (getDuration() - EPSILON);
    }

    public boolean isLoop() {
        return loop;
    }
    
    /**
     * Adds a key frame to this timeline. If the key frame's position is
     * located <em>before</em> the last key frame in this timeline, the
     * new key frame will not be inserted at the end but instead at its
     * appropriate location.
     *
     * @return This timeline, for method chaining.
     * @throws IllegalStateException if this timeline already has a key
     *         frame at the same position.
     */
    public Timeline addKeyFrame(KeyFrame keyFrame) {
        if (keyFrames.isEmpty() || keyFrame.time() > getDuration()) {
            keyFrames.add(keyFrame);
            return this;
        }

        // Checking all key frames is relatively expensive if the
        // timeline is huge. However, the overwhelming majority
        // of timelines are *not* huge.
        Preconditions.checkState(!checkKeyFrame(keyFrame.time()),
            "Timeline already contains a key frame at position " + keyFrame.time());

        keyFrames.add(keyFrame);
        Collections.sort(keyFrames);
        return this;
    }

    private boolean checkKeyFrame(double position) {
        return keyFrames.stream()
            .map(KeyFrame::time)
            .anyMatch(t -> t >= position - EPSILON && t <= position + EPSILON);
    }
    
    /**
     * Adds a key frame with the specified position (in seconds) and value.
     * If the key frame's position is located <em>before</em> the last key
     * frame in this timeline, the new key frame will not be inserted at
     * the end but instead at its appropriate location.
     *
     * @return This timeline, for method chaining.
     * @throws IllegalStateException if this timeline already has a key
     *         frame at the same position.
     */
    public Timeline addKeyFrame(double position, double value) {
        KeyFrame keyFrame = new KeyFrame(position, value);
        return addKeyFrame(keyFrame);
    }

    public void removeKeyFrame(KeyFrame keyFrame) {
        keyFrames.remove(keyFrame);
        setPlayhead(Math.min(playhead, getDuration()));
    }

    /**
     * Returns the value of the property that is animated by this timeline,
     * based on the current position of the playhead. If the playhead is
     * positioned in between key frames, interpolation will be used to
     * determine the current value.
     */
    public double getValue() {
        if (keyFrames.isEmpty()) {
            return 0.0;
        }

        if (nextKeyFrameIndex == 0) {
            return keyFrames.getFirst().value();
        }

        KeyFrame prev = keyFrames.get(nextKeyFrameIndex - 1);
        KeyFrame next = keyFrames.get(nextKeyFrameIndex);
        return interpolateValue(prev, next);
    }

    /**
     * Interpolates between the specified two key frames based on the current
     * position of the playhead.
     */
    private double interpolateValue(KeyFrame prev, KeyFrame next) {
        if (playhead <= prev.time()) {
            return prev.value();
        }

        if (playhead >= next.time()) {
            return next.value();
        }
        
        // Although getDelta() already returns a value between 0 and 1 for,
        // the entire timeline, we need a value between 0 and 1 for the
        // relative position between these two key frames.
        double relativeDelta = (playhead - prev.time()) / (next.time() - prev.time());
        return interpolationMethod.strictInterpolate(prev.value(), next.value(), relativeDelta);
    }

    /**
     * Returns a new timeline that contains the same key frames as this
     * timeline, but in the reverse order. This means the first key frame in
     * the result will have the value of the last key frame in this timeline,
     * and vice versa.
     */
    public Timeline reverse() {
        Timeline reversed = new Timeline(interpolationMethod, loop);
        for (int i = 0; i < keyFrames.size(); i++) {
            double value = keyFrames.get(keyFrames.size() - i - 1).value();
            reversed.addKeyFrame(keyFrames.get(i).time(), value);
        }
        return reversed;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Timeline\n");
        for (KeyFrame keyFrame : keyFrames) {
            buffer.append("    ");
            buffer.append(keyFrame);
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
