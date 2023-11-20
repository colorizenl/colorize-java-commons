//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class URLResponseTest {

    @Test
    void headersKeepInsertionOrder() {
        Headers headers = Headers.of("a", "2", "b", "3");
        URLResponse response = new URLResponse(HttpStatus.OK, headers, new byte[0]);

        assertEquals(List.of("a", "b"), response.getHeaders().getHeaderNames());
    }

    @Test
    void headerNameIsNotCaseSensitive() {
        Headers headers = Headers.of("a", "2");
        URLResponse response = new URLResponse(HttpStatus.OK, headers, new byte[0]);

        assertEquals("2", response.getHeader("a").orElse(""));
        assertEquals("2", response.getHeader("A").orElse(""));
        assertEquals("", response.getHeader("aa").orElse(""));
    }

    @Test
    void sameHeaderCanBeUsedMultipleTimes() {
        Headers headers = Headers.of("a", "2", "b", "3", "a", "4");
        URLResponse response = new URLResponse(HttpStatus.OK, headers, new byte[0]);

        assertEquals(List.of("2", "4"), response.getHeaderValues("a"));
        assertEquals(List.of("3"), response.getHeaderValues("b"));
        assertEquals(Collections.emptyList(), response.getHeaderValues("c"));
    }

    @Test
    void stringForm() {
        Headers headers = Headers.fromMap(Map.of("Accept", "text/plain"));
        byte[] body = "test\nanother line\n".getBytes(Charsets.UTF_8);
        URLResponse response = new URLResponse(HttpStatus.OK, headers, body);

        String expected = """
            Accept: text/plain
            
            
            test
            another line
            """;

        assertEquals(expected, response.toString());
    }
}
