//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@code TextUtils} class.
 */
public class TestTextUtils {
	
	@Test
	public void testRemoveLeadingAndTrailing() {
		assertEquals("1, [2]]", TextUtils.removeLeading("[1, [2]]", "["));
		assertEquals("", TextUtils.removeLeading("", "["));
		
		assertEquals("[1, [2", TextUtils.removeTrailing("[1, [2]]", "]"));
		assertEquals("", TextUtils.removeTrailing("", "]"));
		
		assertEquals("1, [2", TextUtils.removeLeadingAndTrailing("[1, [2]]", "[", "]"));
	}
	
	@Test
	public void testMatchAll() {
		Pattern regex = Pattern.compile("\\w(\\w)");
		assertEquals(Arrays.asList("aa", "cc"), TextUtils.matchAll("aa b cc", regex));
		assertEquals(Arrays.asList("a", "c"), TextUtils.matchAll("aa b cc", regex, 1));
		assertEquals(Arrays.asList(), TextUtils.matchAll("a b c", regex, 1));
	}
	
	@Test
	public void testMatchFirst() {
		Pattern regex = Pattern.compile("\\w(\\w)");
		assertEquals("aa", TextUtils.matchFirst("aa b cc", regex).orNull());
		assertEquals("a", TextUtils.matchFirst("aa b cc", regex, 1).orNull());
		assertNull("aa", TextUtils.matchFirst("a b", regex).orNull());
	}

	@Test
	public void testMatchLines() {
		assertEquals(Arrays.asList("first", "second"), TextUtils.matchLines("first\nsecond\nthird", 
				Pattern.compile(".*?s.*")));
	}
	
	@Test
	public void testRemoveBetween() {
		assertEquals("text  text", TextUtils.removeBetween("text from text to text", "from", "to"));
		assertEquals(" text", TextUtils.removeBetween("from text to text", "from", "to"));
		assertEquals("text from text", TextUtils.removeBetween("text from text", "from", "to"));
		assertEquals("text to text", TextUtils.removeBetween("text to text", "from", "to"));
		assertEquals("text text text", TextUtils.removeBetween("text text text", "from", "to"));
	}
	
	@Test
	public void testCalculateLongestCommonPrefix() {
		assertEquals("", TextUtils.calculateLongestCommonPrefix("first", "second"));
		assertEquals("test", TextUtils.calculateLongestCommonPrefix("test", "test"));
		assertEquals("test ", TextUtils.calculateLongestCommonPrefix("test first", "test second"));
		assertEquals(Arrays.asList("a", "b"), TextUtils.calculateLongestCommonPrefix(
				Arrays.asList("a", "b", "c"), Arrays.asList("a", "b", "z")));
	}
	
	@Test
	public void testCalculateLevenshteinDistance() {
		assertEquals(0, TextUtils.calculateLevenshteinDistance("", ""));
		assertEquals(0, TextUtils.calculateLevenshteinDistance("test", "test"));
		assertEquals(3, TextUtils.calculateLevenshteinDistance("kitten", "sitting"));
		
		assertEquals(0.0f, TextUtils.calculateRelativeLevenshteinDistance("test", "test"), 0.01f);
		assertEquals(0.43f, TextUtils.calculateRelativeLevenshteinDistance("kitten", "sitting"), 0.01f);
		assertEquals(0.0f, TextUtils.calculateRelativeLevenshteinDistance("te-st", "test"), 0.01f);
		assertEquals(1.0f, TextUtils.calculateRelativeLevenshteinDistance("", "test"), 0.01f);
		assertEquals(1.0f, TextUtils.calculateRelativeLevenshteinDistance("test", ""), 0.01f);
	}
	
	@Test
	public void testFuzzyMatch() {
		assertEquals(Arrays.asList("john smith", "john smit"), TextUtils.fuzzyMatch("john smith", 
				Arrays.asList("john smith", "john smit", "john", "pete"), 0.2f));
		assertEquals(Arrays.asList(), TextUtils.fuzzyMatch("john smith", Arrays.<String>asList(), 0.1f));
		assertEquals(Arrays.asList("aaa", "aab"), TextUtils.fuzzyMatch("aaa", 
				Arrays.asList("aaa", "aab", "abb"), 0.35f));
		assertEquals(Arrays.asList("aaa"), TextUtils.fuzzyMatch("aaa", 
				Arrays.asList("aaa", "aab", "abb"), 0.1f));
		assertEquals(Arrays.asList("aaa"), TextUtils.fuzzyMatch("aaa", Arrays.asList("aaa"), 0f));
	}
}
