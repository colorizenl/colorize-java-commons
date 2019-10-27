//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class HttpMessageTest {

    @Test
    public void testMultipleHeadersWithSameName() {
        MockHttpMessage message = new MockHttpMessage();
        message.addHeader("a", "1");
        message.addHeader("b", "2");
        message.addHeader("b", "3");
        message.addHeader("b", "3");

        assertTrue(message.hasHeader("a"));
        assertTrue(message.hasHeader("b"));
        assertFalse(message.hasHeader("c"));

        assertEquals("1", message.getHeader("a"));
        assertEquals("2", message.getHeader("b"));

        assertEquals(ImmutableList.of("1"), message.getHeaderValues("a"));
        assertEquals(ImmutableList.of("2", "3", "3"), message.getHeaderValues("b"));

        assertEquals(ImmutableSet.of("a", "b"), message.getHeaderNames());
    }

    @Test
    public void testHeaderNamesAreCaseInsensitive() {
        MockHttpMessage message = new MockHttpMessage();
        message.addHeader("Content-Type", "1");
        message.addHeader("content-type", "2");

        assertTrue(message.hasHeader("Content-Type"));
        assertTrue(message.hasHeader("content-type"));

        assertEquals(ImmutableList.of("1", "2"), message.getHeaderValues("Content-Type"));
        assertEquals(ImmutableList.of("1", "2"), message.getHeaderValues("content-type"));
        assertEquals(ImmutableSet.of("Content-Type"), message.getHeaderNames());
    }

    @Test
    public void testReplaceHeader() {
        MockHttpMessage message = new MockHttpMessage();
        message.addHeader("a", "1");
        message.replaceHeader("a", "2");

        assertTrue(message.hasHeader("a"));
        assertEquals(ImmutableList.of("2"), message.getHeaderValues("a"));
    }

    @Test
    public void testBody() throws IOException {
        MockHttpMessage message = new MockHttpMessage();
        message.setBody(MediaType.PLAIN_TEXT_UTF_8, "Test");
        assertEquals("Test", message.getBody());
        assertEquals(4, ByteStreams.toByteArray(message.openBodyStream()).length);
        assertTrue(message.hasHeader("Content-Type"));
        assertEquals("text/plain; charset=utf-8", message.getHeader("Content-Type"));

        MockHttpMessage empty = new MockHttpMessage();
        assertEquals("", empty.getBody());
        assertArrayEquals(new byte[0], ByteStreams.toByteArray(empty.openBodyStream()));
    }

    private static class MockHttpMessage extends HttpMessage {

    }
}
