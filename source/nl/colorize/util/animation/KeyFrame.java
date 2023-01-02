//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import com.google.common.base.Preconditions;

/**
 * Defines a property's value at a certain point in time. Key frames are placed 
 * on a timeline, which can then animate the property by interpolating between
 * key frames.
 */
public record KeyFrame(float time, float value) implements Comparable<KeyFrame> {

    public KeyFrame {
        Preconditions.checkArgument(time >= 0f, "Invalid time position: " + time);
    }

    @Override
    public int compareTo(KeyFrame other) {
        return Float.compare(time, other.time);
    }
}
