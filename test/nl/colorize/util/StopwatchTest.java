//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StopwatchTest {

    @Test
    public void testTick() throws Exception {
        Stopwatch timer = new Stopwatch();
        long firstTick = timer.tick();
        Thread.sleep(10);
        long secondTick = timer.tick();
        
        assertTrue(secondTick > firstTick);
    }
    
    @Test
    public void testTockDoesNotSetNewTick() throws Exception {
        Stopwatch timer = new Stopwatch();
        timer.tick();
        Thread.sleep(10);
        
        assertTrue(timer.tock() > 0);
        
        long secondTick = timer.tick();
        
        assertTrue(timer.tock() < secondTick);
    }
}
