//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import java.util.function.Consumer;

/**
 * Notified of events that occur while an animation is playing.
 */
@FunctionalInterface
public interface AnimationObserver {

    public AnimationObserver onFrame(Consumer<Animatable> callback);
}
