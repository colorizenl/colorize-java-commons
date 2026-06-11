//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import com.google.common.base.Preconditions;

/**
 * Defines a property's value at a certain point in time, used in conjunction
 * with {@link Timeline}. The key frame's {@code time} indicates its position
 * on the timeline, in seconds. The timeline will then use the key frame data,
 * interpolating between key frames when necessary.
 *
 * @see Timeline
 */
public record KeyFrame(double time, double value) implements Comparable<KeyFrame> {

    public KeyFrame {
        Preconditions.checkArgument(time >= 0.0, "Invalid time position: " + time);
    }

    @Override
    public int compareTo(KeyFrame other) {
        return Double.compare(time, other.time);
    }
}
