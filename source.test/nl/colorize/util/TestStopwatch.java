//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code Stopwatch} class.
 */
public class TestStopwatch {

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
	
	@Test
	public void testMark() throws Exception {
		Stopwatch timer = new Stopwatch();
		timer.mark("first");
		Thread.sleep(10);
		timer.mark("second");
		
		assertTrue(timer.getMark("second") > timer.getMark("first"));
	}
}
