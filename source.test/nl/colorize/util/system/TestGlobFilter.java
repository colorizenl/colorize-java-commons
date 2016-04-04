//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.system;

import nl.colorize.util.system.GlobFilter;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for the {@code GlobFilter} class.
 */
public class TestGlobFilter {
	
	@Test
	public void testLiteralMatch() {
		GlobFilter filter = new GlobFilter("first.txt");
		assertTrue(filter.accept(null, "first.txt"));
		assertFalse(filter.accept(null, "second.txt"));
	}
	
	@Test
	public void testQuestionMark() {
		GlobFilter filter = new GlobFilter("t?st");
		assertTrue(filter.accept(null, "test"));
		assertTrue(filter.accept(null, "tost"));
		assertFalse(filter.accept(null, "teste"));
	}
	
	@Test
	public void testAsterisk() {
		GlobFilter filter = new GlobFilter("*.xml");
		assertTrue(filter.accept(null, "first.xml"));
		assertTrue(filter.accept(null, "second.xml"));
		assertFalse(filter.accept(null, "second.png"));
		
		GlobFilter start = new GlobFilter("*ab");
		assertTrue(start.accept(null, "ab"));
		assertTrue(start.accept(null, "zab"));
		assertFalse(start.accept(null, "abc"));
		assertFalse(start.accept(null, "bcd"));
		
		GlobFilter end = new GlobFilter("*svn*");
		assertTrue(end.accept(null, ".svn"));
		assertTrue(end.accept(null, "svn"));
		assertTrue(end.accept(null, ".svnignore"));
		assertFalse(end.accept(null, "abc"));
		assertFalse(end.accept(null, ""));
	}
	
	@Test
	public void testAlternativesInBrackets() {
		GlobFilter filter = new GlobFilter("t[ea]st");
		assertTrue(filter.accept(null, "test"));
		assertTrue(filter.accept(null, "tast"));
		assertFalse(filter.accept(null, "tost"));
	}
	
	@Test
	public void testEscapeCharacters() {
		GlobFilter filter = new GlobFilter("t\\*st");
		assertTrue(filter.accept(null, "t*st"));
		assertFalse(filter.accept(null, "test"));
	}
	
	@Test
	public void testCaseSensitivity() {
		GlobFilter filter = new GlobFilter("*.xml");
		assertTrue(filter.accept(null, "a.xml"));
		assertTrue(filter.accept(null, "a.XML"));
		
		GlobFilter caseSensitiveFilter = new GlobFilter("*.xml", true);
		assertTrue(caseSensitiveFilter.accept(null, "a.xml"));
		assertFalse(caseSensitiveFilter.accept(null, "a.XML"));
	}
}
