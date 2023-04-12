//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

/**
 * A timer with millisecond precision. Although the timer <em>granularity</em>
 * is in milliseconds, the timer <em>resolution</em> is dependent on the
 * platform and might be more than 1 millisecond.
 * <p>
 * This class is not thread safe, {@code Stopwatch} instances should not be
 * shared between multiple threads.
 */
public class Stopwatch {

    private boolean useNanoTime;
    private long lastTick;

    public Stopwatch() {
        this.useNanoTime = Platform.isWindows() || Platform.isMac();
        tick();
    }
    
    /**
     * Returns the current value of the underlying timer. Although the absolute
     * value is meaningless, multiple invocations of this method can be
     * compared against each other to determine the elapsed time.
     */
    public long value() {
        if (useNanoTime) {
            return System.nanoTime() / 1_000_000L;
        } else {
            return System.currentTimeMillis();
        }
    }
    
    /**
     * Marks this point in time. Calling {@link #tock()} will return the time
     * relative to this point. Returns the interval between now and the
     * previous tick, in milliseconds.
     */
    public long tick() {
        long thisTick = value();
        long interval = thisTick - lastTick;
        lastTick = thisTick;
        return interval;
    }
    
    /**
     * Returns the time since the last {@link #tick()}, but does not set a
     * new tick. Returns the interval between now and the previous tick, in
     * milliseconds.
     */
    public long tock() {
        return value() - lastTick;
    }
}
