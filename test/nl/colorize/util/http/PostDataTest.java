//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PostDataTest {

    @Test
    public void testParse() {
        assertEquals(ImmutableMap.of(), PostData.parse("", Charsets.UTF_8).getData());
        assertEquals(ImmutableMap.of("a", "2"), PostData.parse("a=2", Charsets.UTF_8).getData());
        assertEquals(ImmutableMap.of("a", "", "b", "4"), PostData.parse("a=&b=4", Charsets.UTF_8).getData());
        assertEquals(ImmutableMap.of("a", "3>4"), PostData.parse("a=3%3E4", Charsets.UTF_8).getData());
        assertEquals(ImmutableMap.of(), PostData.parse("?", Charsets.UTF_8).getData());
        assertEquals(ImmutableMap.of("a", "7"), PostData.parse("?a=7", Charsets.UTF_8).getData());
        assertEquals(ImmutableMap.of("a", ""), PostData.parse("a=", Charsets.UTF_8).getData());
        assertEquals(ImmutableMap.of(), PostData.parse("a", Charsets.UTF_8).getData());
    }

    @Test
    public void testEncode() {
        assertEquals("", PostData.create(ImmutableMap.of()).encode(Charsets.UTF_8));
        assertEquals("a=2", PostData.create(ImmutableMap.of("a", "2")).encode(Charsets.UTF_8));
        assertEquals("a=2&b=3", PostData.create(ImmutableMap.of("a", "2", "b", "3")).encode(Charsets.UTF_8));
        assertEquals("a=2&b=", PostData.create(ImmutableMap.of("a", "2", "b", "")).encode(Charsets.UTF_8));
        assertEquals("a=3%3E4", PostData.create(ImmutableMap.of("a", "3>4")).encode(Charsets.UTF_8));
    }

    @Test
    public void testGetRequired() {
        PostData postData = PostData.create(ImmutableMap.of("a", "2", "b", "3"));

        assertEquals("2", postData.getRequiredParameter("a"));
        assertEquals("3", postData.getRequiredParameter("b"));
    }

    @Test
    public void testGetRequiredMissing() {
        PostData postData = PostData.create(ImmutableMap.of("a", "2", "b", "3"));

        assertThrows(IllegalStateException.class, () -> postData.getRequiredParameter("c"));
    }

    @Test
    public void testGetOptional() {
        PostData postData = PostData.create(ImmutableMap.of("a", "2", "b", "3", "c", ""));

        assertEquals("2", postData.getOptionalParameter("a", "0"));
        assertEquals("3", postData.getOptionalParameter("b", "0"));
        assertEquals("0", postData.getOptionalParameter("c", "0"));
        assertEquals("0", postData.getOptionalParameter("d", "0"));
    }

    @Test
    public void testCreateFromNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("a", null);

        assertEquals(ImmutableMap.of("a", ""), PostData.create(map).getData());
    }

    @Test
    public void testEmptyName() {
        PostData emptyName = PostData.create("", "2");
        PostData emptyValue = PostData.create("b", "");

        assertEquals("2", emptyName.toString());
        assertEquals("b=", emptyValue.toString());
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

        assertEquals(ImmutableMap.of("a", "2", "b", "3"), merged.getData());
    }

    @Test
    void cannotMergeIfSameParameter() {
        PostData a = PostData.create("a", "2");
        PostData b = PostData.create("a", "3");

        assertThrows(IllegalArgumentException.class, () -> a.merge(b));
    }
}
