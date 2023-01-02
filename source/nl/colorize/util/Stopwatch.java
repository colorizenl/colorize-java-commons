//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

/**
 * A timer with millisecond precision. Although timer values are reported in
 * milliseconds, the timer resolution is dependent on the platform and may be
 * more than 1 millisecond.
 * <p>
 * This class is not thread safe, {@code Stopwatch} instances cannot be shared
 * between multiple threads.
 */
public class Stopwatch {
    
    private long lastTick;
    private long lastInterval;
    
    public Stopwatch() {
        tick();
    }
    
    /**
     * Returns the current value of the underlying timer. Note that this value
     * should only be compared against other invocations of this method, the
     * value returned is not absolute.
     */
    protected long value() {
        return System.currentTimeMillis();
    }
    
    /**
     * Marks this point in time. Calling {@link #tock()} will return the time
     * relative to this point. Returns the interval between the new tick and
     * the previous tick, in milliseconds. This value can later also be obtained
     * using {@link #getLastInterval()}.
     */
    public long tick() {
        long thisTick = value();
        long interval = thisTick - lastTick;
        lastTick = thisTick;
        lastInterval = interval;
        return interval;
    }
    
    /**
     * Returns the time since the last {@link #tick()} in milliseconds, but
     * does not set a new tick.
     */
    public long tock() {
        return value() - lastTick;
    }

    /**
     * Returns the distance between the most recent tick and the tick before
     * that. Returns zero if there has only been one tick so far.
     */
    public long getLastInterval() {
        return lastInterval;
    }
}
