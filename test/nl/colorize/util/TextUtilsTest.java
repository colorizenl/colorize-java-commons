//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextUtilsTest {

    @Test
    public void testToTitleCase() {
        assertEquals("S", TextUtils.toTitleCase("s"));
        assertEquals("John Doe", TextUtils.toTitleCase("john doe"));
        assertEquals("John Doe", TextUtils.toTitleCase("JOHN_DOE"));
    }
    
    @Test
    public void testCountOccurrences() {
        assertEquals(5, TextUtils.countOccurrences("aa ab aab c", "a"));
        assertEquals(2, TextUtils.countOccurrences("aa ab aab c", "aa"));
        assertEquals(1, TextUtils.countOccurrences("aa ab aab c", "aab"));
        assertEquals(0, TextUtils.countOccurrences("aa ab aab c", "aaab"));
    }

    @Test
    public void testCountOccurrencesWithOverlappingPattern() {
        assertEquals(2, TextUtils.countOccurrences("abababab", "abab"));
    }
    
    @Test
    public void testAddLeadingAndTrailing() {
        assertEquals("atest", TextUtils.addLeading("test", "a"));
        assertEquals("test", TextUtils.addLeading("test", "t"));
        assertEquals("testa", TextUtils.addTrailing("test", "a"));
        assertEquals("test", TextUtils.addTrailing("test", "t"));
    }
    
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
        assertEquals("aa", TextUtils.matchFirst("aa b cc", regex).orElse(null));
        assertEquals("a", TextUtils.matchFirst("aa b cc", regex, 1).orElse(null));
        assertNull(TextUtils.matchFirst("a b", regex).orElse(null));
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
    void startsWith() {
        assertTrue(TextUtils.startsWith("abc", ImmutableList.of("a", "b")));
        assertTrue(TextUtils.startsWith("bbb", ImmutableList.of("a", "b")));
        assertFalse(TextUtils.startsWith("zabc", ImmutableList.of("a", "b")));
        assertFalse(TextUtils.startsWith("zzz", ImmutableList.of("a", "b")));
    }

    @Test
    void endsWith() {
        assertTrue(TextUtils.endsWith("abc", ImmutableList.of("c", "a")));
        assertTrue(TextUtils.endsWith("bba", ImmutableList.of("c", "a")));
        assertFalse(TextUtils.endsWith("abc", ImmutableList.of("a", "b")));
        assertFalse(TextUtils.endsWith("zzz", ImmutableList.of("a", "b")));
    }

    @Test
    void contains() {
        assertTrue(TextUtils.contains("abc", ImmutableList.of("a", "b")));
        assertTrue(TextUtils.contains("bc", ImmutableList.of("a", "b")));
        assertFalse(TextUtils.contains("zz", ImmutableList.of("a", "b")));
    }

    @Test
    void matchWithCallback() {
        Pattern pattern = Pattern.compile("(\\S+) \\S+ (\\S+).*");
        String input = "one two three four\nfive\n\nsix seven eight nine\nten eleven";
        List<String> result = new ArrayList<>();

        TextUtils.matchLines(input, pattern,
            groups -> result.add(groups.get(1) + "/" + groups.get(2)));

        assertEquals(ImmutableList.of("one/three", "six/eight"), result);
    }

    @Test
    void countIndent() {
        assertEquals(0, TextUtils.countIndent("a"));
        assertEquals(1, TextUtils.countIndent(" a"));
        assertEquals(2, TextUtils.countIndent("  a"));
        assertEquals(4, TextUtils.countIndent("\ta"));
    }
}
