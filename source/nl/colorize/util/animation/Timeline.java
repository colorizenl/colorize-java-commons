//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import com.google.common.base.Preconditions;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Animates the value of a property over time, by interpolating between key
 * frames. Timelines implement the {@link Animatable} interface and can be
 * played as animations, notifying a number of observers on every frame. The
 * duration of the animation is equal to the position of the last key frame,
 * although timelines also have the option to loop infinitely or until
 * stopped manually.
 */
public class Timeline implements Animatable {
    
    private float playhead;
    private SortedSet<KeyFrame> keyframes;
    private Interpolation interpolationMethod;
    private boolean loop;

    private static final float EPSILON = 0.001f;
    
    public Timeline(Interpolation interpolationMethod, boolean loop) {
        this.playhead = 0f;
        this.keyframes = new TreeSet<>();
        this.interpolationMethod = interpolationMethod;
        this.loop = loop;    
    }
    
    /**
     * Creates a new timeline that uses the specified interpolation method and 
     * will not loop.
     */
    public Timeline(Interpolation interpolationMethod) {
        this(interpolationMethod, false);
    }
    
    /**
     * Creates a new timeline that uses the linear interpolation and will not 
     * loop.
     */
    public Timeline() {
        this(Interpolation.LINEAR, false);
    }
    
    /**
     * Moves the playhead to the specified position. The playhead cannot go
     * out-of-bounds, it will be restricted to the range (0 - duration).
     * @param position New position of the playhead, in seconds.
     * @throws IllegalStateException if this timeline has no key frames.
     */
    public void setPlayhead(float position) {
        Preconditions.checkState(!keyframes.isEmpty(), "Timeline has no key frames");
        
        float duration = getDuration();
        
        if (loop && position > duration) {
            playhead = position % duration;
        } else {
            playhead = position;
        }
        
        playhead = Math.max(playhead, 0f);
        playhead = Math.min(playhead, duration);
    }

    /**
     * Moves the playhead by the specified amount. The same restrictions apply
     * as with {@link #setPlayhead(float)}. Passing a negative value will 
     * move the playhead backwards.
     * @param amount Amount to move the playhead, in seconds.
     * @throws IllegalStateException if this timeline has no key frames.
     */
    public void movePlayhead(float amount) {        
        setPlayhead(playhead + amount);
    }
    
    /**
     * Moves the playhead forward by {@code deltaTime}.
     * @param deltaTime Amount to move the playhead, in seconds.
     * @throws IllegalStateException if this timeline has no key frames.
     */
    @Override
    public void onFrame(float deltaTime) {
        movePlayhead(deltaTime);
    }
    
    /**
     * Moves the playhead back to the start of the timeline.
     */
    public void reset() {
        if (!keyframes.isEmpty()) {
            setPlayhead(0f);
        }
    }
    
    /**
     * Moves the playhead to the end of the timeline.
     */
    public void end() {
        if (keyframes.isEmpty()) {
            setPlayhead(0f);
        } else {
            setPlayhead(getDuration());
        }
    }

    /**
     * Returns the position of the playhead, in seconds.
     */
    public float getPlayhead() {
        return playhead;
    }

    /**
     * Returns the position of the last key frame, in seconds. If the timeline
     * contains no key frames the duration will be 0.
     */
    public float getDuration() {
        if (keyframes.isEmpty()) {
            return 0;
        }
        return keyframes.last().time();
    }
    
    /**
     * Returns the position of the playhead as a number between 0 and 1, where
     * 0 is the beginning of the timeline and 1 is its duration.
     */
    public float getDelta() {
        if (isAtStart()) {
            return 0f;
        } else if (isCompleted()) {
            return 1f;
        } else {
            return playhead / getDuration();
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
        if (keyframes.isEmpty()) {
            return false;
        }
        return playhead >= (getDuration() - EPSILON);
    }
    
    /**
     * Returns true if this timeline is "playing" (e.g. if the playhead is 
     * positioned somewhere between the beginning and the end of the timeline).
     */
    public boolean isPlaying() {
        return !isAtStart() && !isCompleted(); 
    }

    public Interpolation getInterpolationMethod() {
        return interpolationMethod;
    }
    
    public boolean isLoop() {
        return loop;
    }
    
    /**
     * Adds a key frame to this timeline.
     * @return This timeline, for method chaining;
     * @throws IllegalArgumentException if a key frame already exists at that position.
     */
    public Timeline addKeyFrame(KeyFrame keyframe) {
        float position = keyframe.time();
        Preconditions.checkArgument(getKeyFrameAtPosition(position) == null,
            "Key frame already exists at position: " + position);
        keyframes.add(keyframe);
        return this;
    }
    
    /**
     * Adds a key frame with the specified position and value to this timeline.
     * @param position Position of the key frame on the timeline, in seconds.
     * @return This timeline, for method chaining;
     * @throws IllegalArgumentException if a key frame already exists at that position.
     */
    public Timeline addKeyFrame(float position, float value) {
        return addKeyFrame(new KeyFrame(position, value));
    }

    public void removeKeyFrame(KeyFrame keyframe) {
        keyframes.remove(keyframe);
    }
    
    /**
     * Removes the key frame at the specified position.
     * @deprecated Use {@link #removeKeyFrame(KeyFrame)} instead.
     */
    @Deprecated
    public void removeKeyFrame(float position) {
        KeyFrame atPosition = getKeyFrameAtPosition(position);
        if (atPosition != null) {
            keyframes.remove(atPosition);
        }
    }
    
    public void removeAllKeyFrames() {
        keyframes.clear();
    }
    
    /**
     * Returns all key frames that have been added. The key frames are sorted by 
     * their position on the timeline.
     */
    public SortedSet<KeyFrame> getKeyFrames() {
        return keyframes;
    }
    
    public int getNumKeyFrames() {
        return keyframes.size();
    }
    
    /**
     * Returns the key frame with the lowest position.
     * @throws IllegalStateException if this timeline does not contain any key frames.
     */
    public KeyFrame getFirstKeyFrame() {
        if (keyframes.isEmpty()) {
            throw new IllegalStateException("No key frames");
        }
        return keyframes.first();
    }
    
    /**
     * Returns the key frame with the highest position.
     * @throws IllegalStateException if this timeline does not contain any key frames.
     */
    public KeyFrame getLastKeyFrame() {
        if (keyframes.isEmpty()) {
            throw new IllegalStateException("No key frames");
        }
        return keyframes.last();
    }
    
    /**
     * Returns the key frame on this timeline that is closest to and less than
     * or equal to {@code position}. If there is only one key frame that will be
     * returned, even if it's position is after {@code position}.
     * @throws IllegalStateException if this timeline does not contain any key frames.
     */
    protected KeyFrame getClosestKeyFrameBefore(float position) {
        if (keyframes.isEmpty()) {
            throw new IllegalStateException("No key frames");
        }
        
        KeyFrame closest = keyframes.first();
        for (KeyFrame keyframe : keyframes) {
            if (keyframe.time() <= position) {
                closest = keyframe;
            } else {
                break;
            }
        }
        return closest;
    }
    
    private KeyFrame getKeyFrameAtPosition(float position) {
        for (KeyFrame keyframe : keyframes) {
            if (hasKeyFrameAtPosition(keyframe, position)) {
                return keyframe;
            }
        }
        return null;
    }
    
    public boolean hasKeyFrameAtPosition(float position) {
        return getKeyFrameAtPosition(position) != null;
    }
    
    private boolean hasKeyFrameAtPosition(KeyFrame keyframe, float position) {
        return keyframe.time() >= position - EPSILON && keyframe.time() <= position + EPSILON;
    }

    /**
     * Returns the value of the property that is animated by this timeline. The
     * value depends on the key frames that have been added, the current position
     * of the playhead, and the used interpolation method.
     * @throws IllegalStateException if this timeline does not contain any key frames.
     */
    public float getValue() {
        Preconditions.checkState(!keyframes.isEmpty(), "Timeline has no key frames");
        
        if (keyframes.size() == 1) {
            return keyframes.first().value();
        } else {
            return getInterpolatedValue();
        }
    }
    
    /**
     * Returns the property's current value, based on the current position of 
     * the playhead.
     */
    private float getInterpolatedValue() {
        KeyFrame currentKeyFrame = null;
        KeyFrame nextKeyFrame = null;
        
        for (KeyFrame keyframe : keyframes) {
            if (keyframe.time() <= playhead) {
                currentKeyFrame = keyframe;
            } else {
                if (currentKeyFrame == null) {
                    currentKeyFrame = keyframe;
                }
                nextKeyFrame = keyframe;
                break;
            }
        }
        
        return interpolateValue(currentKeyFrame, nextKeyFrame);
    }

    /**
     * Interpolates between two key frames, based on the position of the playhead
     * and the used interpolation method.
     */
    private float interpolateValue(KeyFrame prev, KeyFrame next) {
        if (next == null || prev == next) {
            return prev.value();
        }
        
        if (playhead <= prev.time()) {
            return prev.value();
        } else if (playhead >= next.time()) {
            return next.value();
        }
        
        // Although getDelta() already returns a value between 0 and 1 for,
        // the entire timeline, we need a value between 0 and 1 for the
        // relative position between these two key frames.
        float relativePlayhead = playhead - prev.time();
        float relativeDuration = next.time() - prev.time();
        float relativeDelta = relativePlayhead / relativeDuration;
        
        return interpolationMethod.interpolate(prev.value(), next.value(), relativeDelta);
    }
}
