//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A timer with millisecond precision. Although timer values are reported in
 * milliseconds, the timer resolution is dependent on the platform and may be
 * more than 1 millisecond.
 * <p>
 * This class is not thread safe, {@code Stopwatch} instances cannot be shared
 * between multiple threads.
 */
public final class Stopwatch {
    
    private long lastTick;
    private Map<String, Long> marks;
    
    public Stopwatch() {
        tick();
        marks = new HashMap<String, Long>();
    }
    
    /**
     * Returns the current value of the underlying timer. Note that this value
     * should only be compared against other invocations of this method, the
     * value returned is not absolute.
     */
    private long value() {
        return System.currentTimeMillis();
    }
    
    /**
     * Marks this point in time. Calling {@link #tock()} will return the time
     * relative to this point.
     * @return Time passed since the last tick, in milliseconds.
     */
    public long tick() {
        long thisTick = value();
        long since = thisTick - lastTick;
        lastTick = thisTick;
        return since;
    }
    
    /**
     * Returns the time since the last {@link #tick()}, but does not set a new
     * tick.
     * @return Time passed since the last tick, in milliseconds.
     */
    public long tock() {
        return value() - lastTick;
    }
    
    /**
     * Marks this point in time for future reference. This method is equivalent
     * to {@link #tick()}, but stores the time passed since the last tick so 
     * that it can be retrieved later using {@link #getMark(String)}.
     * @param label Text description for the mark, used to retrieve it later. 
     */
    public void mark(String label) {
        marks.put(label, tick());
    }
    
    /**
     * Retrieves a marked point in time that has been created using 
     * {@link #mark(String)}. If multiple marks use the same label, the most
     * recent one is returned.
     * @throws IllegalArgumentException if no mark with that label exists.  
     */
    public long getMark(String label) {
        Long mark = marks.get(label);
        if (mark == null) {
            throw new IllegalArgumentException("No mark with label " + label);
        }
        return mark.longValue();
    }
}
