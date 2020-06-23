//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpMessageTest {

    @Test
    public void testMultipleHeadersWithSameName() {
        MockHttpMessage message = new MockHttpMessage();
        message.addHeader("a", "1");
        message.addHeader("b", "2");
        message.addHeader("b", "3");
        message.addHeader("b", "3");

        assertTrue(message.getHeaders().has("a"));
        assertTrue(message.getHeaders().has("b"));
        assertFalse(message.getHeaders().has("c"));

        assertEquals("1", message.getHeaders().getValue("a"));
        assertEquals("2", message.getHeaders().getValue("b"));

        assertEquals(ImmutableList.of("1"), message.getHeaders().getValues("a"));
        assertEquals(ImmutableList.of("2", "3", "3"), message.getHeaders().getValues("b"));

        assertEquals(ImmutableSet.of("a", "b"), message.getHeaders().getNames());
    }

    @Test
    public void testHeaderNamesAreCaseInsensitive() {
        MockHttpMessage message = new MockHttpMessage();
        message.addHeader("Content-Type", "1");
        message.addHeader("content-type", "2");

        assertTrue(message.getHeaders().has("Content-Type"));
        assertTrue(message.getHeaders().has("content-type"));

        assertEquals(ImmutableList.of("1", "2"), message.getHeaders().getValues("Content-Type"));
        assertEquals(ImmutableList.of("1", "2"), message.getHeaders().getValues("content-type"));
        assertEquals(ImmutableSet.of("Content-Type"), message.getHeaders().getNames());
    }

    @Test
    public void testReplaceHeader() {
        MockHttpMessage message = new MockHttpMessage();
        message.addHeader("a", "1");
        message.getHeaders().replace("a", "2");

        assertTrue(message.getHeaders().has("a"));
        assertEquals(ImmutableList.of("2"), message.getHeaders().getValues("a"));
    }

    @Test
    public void testBody() throws IOException {
        MockHttpMessage message = new MockHttpMessage();
        message.setBody(MediaType.PLAIN_TEXT_UTF_8, "Test");
        assertEquals("Test", message.getBody());
        assertEquals(4, ByteStreams.toByteArray(message.openBodyStream()).length);
        assertTrue(message.getHeaders().has("Content-Type"));
        assertEquals("text/plain; charset=utf-8", message.getHeaders().getValue("Content-Type"));

        MockHttpMessage empty = new MockHttpMessage();
        assertEquals("", empty.getBody());
        assertArrayEquals(new byte[0], ByteStreams.toByteArray(empty.openBodyStream()));
    }

    private static class MockHttpMessage extends HttpMessage {

    }
}
