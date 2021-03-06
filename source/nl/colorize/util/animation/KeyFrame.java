//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

/**
 * Defines a property's value at a certain point in time. Key frames are placed 
 * on a timeline, which can then animate the property by interpolating between
 * key frames.
 */
public class KeyFrame implements Comparable<KeyFrame> {
    
    private float time;
    private float value;
    
    /**
     * Creates a new key frame with the specified value.
     * @param time Position of the key frame on the timeline, in seconds.
     * @throws IllegalArgumentException if {@code time} is negative.
     */
    public KeyFrame(float time, float value) {
        if (time < 0) {
            throw new IllegalArgumentException("Invalid time position: " + time);
        }
        this.time = time;
        this.value = value;
    }
    
    /**
     * Returns the position of this key frame on the timeline, in seconds.
     */
    public float getTime() {
        return time;
    }
    
    public float getValue() {
        return value;
    }
    
    /**
     * Compares key frames based on their position on the timeline
     */
    public int compareTo(KeyFrame other) {
        if (time < other.time) {
            return -1;
        } else if (time > other.time) {
            return 1;
        } else {
            return 0;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof KeyFrame) {
            return compareTo((KeyFrame) o) == 0;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Math.round(time * 1000);
    }
    
    @Override
    public String toString() {
        return String.format("KeyFrame(time=%.3f, value=%.3f)", time, value);
    }
}
