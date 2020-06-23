//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeadersTest {

    @Test
    public void testKeepInsertionOrder() {
        Headers headers = new Headers();
        headers.add("a", "2");
        headers.add("b", "3");

        assertEquals(ImmutableSet.of("a", "b"), headers.getNames());
        assertEquals("a", headers.getNames().iterator().next());
    }

    @Test
    public void testAddMultipleHeadersWithSameName() {
        Headers headers = new Headers();
        headers.add("a", "2");
        headers.add("a", "3");

        assertEquals(ImmutableList.of("2", "3"), headers.getValues("a"));
        assertEquals("2", headers.getValue("a"));
    }

    @Test
    public void testReplaceHeader() {
        Headers headers = new Headers();
        headers.add("a", "2");
        headers.replace("a", "3");

        assertEquals(ImmutableList.of("3"), headers.getValues("a"));
        assertEquals("3", headers.getValue("a"));
    }

    @Test
    public void testNamesAreNotCaseSensitive() {
        Headers headers = new Headers();
        headers.add("a", "2");
        headers.add("A", "3");

        assertEquals(ImmutableSet.of("a"), headers.getNames());
        assertEquals(ImmutableList.of("2", "3"), headers.getValues("a"));
    }

    @Test
    public void testCannotNullHeader() {
        Headers headers = new Headers();

        assertThrows(IllegalArgumentException.class, () -> headers.add(null, "test"));
    }

    @Test
    public void testCannotHaveEmptyHeaderName() {
        Headers headers = new Headers();

        assertThrows(IllegalArgumentException.class, () -> headers.add("", "test"));
    }

    @Test
    public void testCanHaveEmptyHeaderValue() {
        Headers headers = new Headers();
        headers.add("test", "");

        assertTrue(headers.has("test"));
    }

    @Test
    public void testMerge() {
        Headers first = new Headers();
        first.add("a", "2");
        first.add("b", "3");

        Headers second = new Headers();
        second.add("b", "4");
        second.add("c", "5");

        first.merge(second);

        assertEquals(ImmutableSet.of("a", "b", "c"), first.getNames());
        assertEquals(ImmutableList.of("3", "4"), first.getValues("b"));
    }

    @Test
    public void testReplace() {
        Headers first = new Headers();
        first.add("a", "2");
        first.add("b", "3");

        Headers second = new Headers();
        second.add("b", "4");
        second.add("c", "5");

        first.replace(second);

        assertEquals(ImmutableSet.of("b", "c"), first.getNames());
        assertEquals(ImmutableList.of("4"), first.getValues("b"));
    }

    @Test
    public void testGetEntries() {
        Headers headers = new Headers();
        headers.add("a", "2");
        headers.add("b", "3");
        headers.add("b", "4");

        assertEquals("[(a, 2), (b, 3), (b, 4)]", headers.getEntries().toString());
    }
}
