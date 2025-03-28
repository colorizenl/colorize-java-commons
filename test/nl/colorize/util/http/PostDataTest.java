//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostDataTest {

    @Test
    public void testParse() {
        assertEquals(Map.of(), PostData.parse("", UTF_8).toMap());
        assertEquals(Map.of("a", "2"), PostData.parse("a=2", UTF_8).toMap());
        assertEquals(Map.of("a", "", "b", "4"), PostData.parse("a=&b=4", UTF_8).toMap());
        assertEquals(Map.of("a", "3>4"), PostData.parse("a=3%3E4", UTF_8).toMap());
        assertEquals(Map.of(), PostData.parse("?", UTF_8).toMap());
        assertEquals(Map.of("a", "7"), PostData.parse("?a=7", UTF_8).toMap());
        assertEquals(Map.of("a", ""), PostData.parse("a=", UTF_8).toMap());
        assertEquals(Map.of("a", ""), PostData.parse("a", UTF_8).toMap());
    }

    @Test
    public void testEncode() {
        assertEquals("", PostData.create(Map.of()).encode(UTF_8));
        assertEquals("a=2", PostData.create(Map.of("a", "2")).encode(UTF_8));
        assertEquals("a=2&b=3", PostData.create("a", "2", "b", "3").encode(UTF_8));
        assertEquals("a=2&b", PostData.create("a", "2", "b", "").encode(UTF_8));
        assertEquals("a=3%3E4", PostData.create("a", "3>4").encode(UTF_8));
    }

    @Test
    public void testGetRequired() {
        PostData postData = PostData.create("a", "2", "b", "3");

        assertEquals("2", postData.getRequiredParameter("a"));
        assertEquals("3", postData.getRequiredParameter("b"));
    }

    @Test
    public void testGetRequiredMissing() {
        PostData postData = PostData.create("a", "2", "b", "3");

        assertThrows(IllegalStateException.class, () -> postData.getRequiredParameter("c"));
    }

    @Test
    public void testGetOptional() {
        PostData postData = PostData.create("a", "2", "b", "3", "c", "");

        assertEquals("2", postData.getOptionalParameter("a", "0"));
        assertEquals("3", postData.getOptionalParameter("b", "0"));
        assertEquals("0", postData.getOptionalParameter("c", "0"));
        assertEquals("0", postData.getOptionalParameter("d", "0"));
    }

    @Test
    public void testCreateFromNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("a", null);

        assertEquals(Map.of("a", ""), PostData.create(map).toMap());
    }

    @Test
    public void testEmptyValue() {
        assertEquals("b", PostData.create("b", "").toString());
    }

    @Test
    public void testCreateVarArgs() {
        PostData postData = PostData.create("1", "2", "3", "4", "5", "6");

        assertEquals("1=2&3=4&5=6", postData.toString());
    }

    @Test
    public void testWrongNumberOfVarArgs() {
        assertThrows(IllegalArgumentException.class, () -> PostData.create("1", "2", "3"));
    }

    @Test
    void merge() {
        PostData a = PostData.create("a", "2");
        PostData b = PostData.create("b", "3");
        PostData merged = a.merge(b);

        assertEquals(Map.of("a", "2", "b", "3"), merged.toMap());
    }

    @Test
    void canMergeIfSameParameter() {
        PostData a = PostData.create("a", "2");
        PostData b = PostData.create("a", "3");

        assertEquals("a=2&a=3", a.merge(b).toString());
    }

    @Test
    void keyWithoutValue() {
        PostData postData = PostData.parse("a=2&b=&c", UTF_8);

        assertEquals("2", postData.getOptionalParameter("a", "?"));
        assertTrue(postData.contains("b"));
        assertEquals("?", postData.getOptionalParameter("b", "?"));
        assertTrue(postData.contains("c"));
        assertEquals("?", postData.getOptionalParameter("c", "?"));
    }

    @Test
    void sameParameterMultipleTimes() {
        PostData postData = PostData.parse("a=2&b=3&a=4&a");

        assertEquals(List.of("2", "4", ""), postData.getParameterValues("a"));
        assertEquals("2", postData.getRequiredParameter("a"));
        assertEquals("a=2&b=3&a=4&a", postData.toString());
    }

    @Test
    void parseSingleEntry() {
        PostData postData = PostData.parse("?test");

        assertTrue(postData.contains("test"));
        assertEquals(List.of(""), postData.getParameterValues("test"));
        assertEquals("test", postData.toString());
    }

    @Test
    void parseSingleEntryWithEquals() {
        PostData postData = PostData.parse("?test=");

        assertTrue(postData.contains("test"));
        assertEquals(List.of(""), postData.getParameterValues("test"));
    }
}
