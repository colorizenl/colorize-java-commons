//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

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
        assertEquals(Arrays.asList("first", "second"),
            TextUtils.matchLines("first\nsecond\nthird", Pattern.compile(".*?s.*")));
    }

    @Test
    void startsWith() {
        assertTrue(TextUtils.startsWith("abc", List.of("a", "b")));
        assertTrue(TextUtils.startsWith("bbb", List.of("a", "b")));
        assertFalse(TextUtils.startsWith("zabc", List.of("a", "b")));
        assertFalse(TextUtils.startsWith("zzz", List.of("a", "b")));
    }

    @Test
    void endsWith() {
        assertTrue(TextUtils.endsWith("abc", List.of("c", "a")));
        assertTrue(TextUtils.endsWith("bba", List.of("c", "a")));
        assertFalse(TextUtils.endsWith("abc", List.of("a", "b")));
        assertFalse(TextUtils.endsWith("zzz", List.of("a", "b")));
    }

    @Test
    void contains() {
        assertTrue(TextUtils.contains("abc", List.of("a", "b")));
        assertTrue(TextUtils.contains("bc", List.of("a", "b")));
        assertFalse(TextUtils.contains("zz", List.of("a", "b")));
    }

    @Test
    void matchWithCallback() {
        Pattern pattern = Pattern.compile("(\\S+) \\S+ (\\S+).*");
        String input = "one two three four\nfive\n\nsix seven eight nine\nten eleven";
        List<String> result = new ArrayList<>();

        TextUtils.matchLines(input, pattern,
            groups -> result.add(groups.get(1) + "/" + groups.get(2)));

        assertEquals(List.of("one/three", "six/eight"), result);
    }

    @Test
    void numberFormat() {
        assertEquals("1.0", TextUtils.numberFormat(1f, 1));
        assertEquals("1.4", TextUtils.numberFormat(1.4f, 1));
        assertEquals("1.5", TextUtils.numberFormat(1.5f, 1));
        assertEquals("999.0", TextUtils.numberFormat(999f, 1));
        assertEquals("1,001.0", TextUtils.numberFormat(1001f, 1));
    }

    @Test
    void timeFormatWithSecondPrecision() {
        assertEquals("----", TextUtils.timeFormat(0L, false));
        assertEquals("0:00", TextUtils.timeFormat(1L, false));
        assertEquals("0:00", TextUtils.timeFormat(500L, false));
        assertEquals("0:00", TextUtils.timeFormat(999L, false));
        assertEquals("0:01", TextUtils.timeFormat(1000L, false));
        assertEquals("0:30", TextUtils.timeFormat(30_000L, false));
        assertEquals("0:59", TextUtils.timeFormat(59_000L, false));
        assertEquals("1:00", TextUtils.timeFormat(60_000L, false));
        assertEquals("1:02", TextUtils.timeFormat(62_000L, false));
        assertEquals("1:15", TextUtils.timeFormat(75_000L, false));
        assertEquals("1:00:00", TextUtils.timeFormat(3600_000L, false));
        assertEquals("1:00:05", TextUtils.timeFormat(3605_000L, false));
        assertEquals("1:01:00", TextUtils.timeFormat(3660_000L, false));
    }

    @Test
    void timeFormatWithMillisecondPrecision() {
        assertEquals("0.001", TextUtils.timeFormat(1L, true));
        assertEquals("0.500", TextUtils.timeFormat(500L, true));
        assertEquals("0.999", TextUtils.timeFormat(999L, true));
        assertEquals("1.000", TextUtils.timeFormat(1000L, true));
        assertEquals("30.000", TextUtils.timeFormat(30_000L, true));
        assertEquals("59.000", TextUtils.timeFormat(59_000L, true));
        assertEquals("1:00.000", TextUtils.timeFormat(60_000L, true));
        assertEquals("1:02.000", TextUtils.timeFormat(62_000L, true));
        assertEquals("1:00:00.000", TextUtils.timeFormat(3600_000L, true));
    }
}
