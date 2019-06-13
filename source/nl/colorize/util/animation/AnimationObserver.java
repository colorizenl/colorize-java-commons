//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.animation;

import java.util.function.Consumer;

/**
 * Notified of events that occur while an animation is playing. Additional
 * callbacks can be registered to listen for specific events.
 */
public interface AnimationObserver {

    /**
     * Registers a callback that will be notified whenever the observed
     * animation received frame updates.
     */
    public AnimationObserver onFrame(Consumer<Animatable> callback);

    /**
     * Registers a callback that will be notified when the observed animation
     * is completed.
     */
    public AnimationObserver onComplete(Consumer<Animatable> callback);
}
