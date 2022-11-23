//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import nl.colorize.util.Tuple;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class URLResponseTest {

    @Test
    void headersKeepInsertionOrder() {
        Headers headers = new Headers(Tuple.of("a", "2"), Tuple.of("b", "3"));
        URLResponse response = new URLResponse(HttpStatus.OK, headers, new byte[0],
            Charsets.UTF_8, Collections.emptyMap());

        assertEquals(List.of("a", "b"), response.headers().getHeaderNames());
    }

    @Test
    void headerNameIsNotCaseSensitive() {
        Headers headers = new Headers(Tuple.of("a", "2"));
        URLResponse response = new URLResponse(HttpStatus.OK, headers, new byte[0],
            Charsets.UTF_8, Collections.emptyMap());

        assertEquals("2", response.getHeader("a").orElse(""));
        assertEquals("2", response.getHeader("A").orElse(""));
        assertEquals("", response.getHeader("aa").orElse(""));
    }

    @Test
    void sameHeaderCanBeUsedMultipleTimes() {
        Headers headers = new Headers(Tuple.of("a", "2"), Tuple.of("b", "3"), Tuple.of("a", "4"));
        URLResponse response = new URLResponse(HttpStatus.OK, headers, new byte[0],
            Charsets.UTF_8, Collections.emptyMap());

        assertEquals(List.of("2", "4"), response.getHeaderValues("a"));
        assertEquals(List.of("3"), response.getHeaderValues("b"));
        assertEquals(Collections.emptyList(), response.getHeaderValues("c"));
    }

    @Test
    void readBody() {
        URLResponse response = new URLResponse(HttpStatus.OK, new Headers(),
            "test\ntest2\n".getBytes(Charsets.UTF_8),
            Charsets.UTF_8,
            Collections.emptyMap());

        assertEquals("test\ntest2\n", response.read(Charsets.UTF_8));
        assertEquals(List.of("test", "test2"), response.readLines(Charsets.UTF_8));
        assertEquals("[116, 101, 115, 116, 10, 116, 101, 115, 116, 50, 10]",
            Arrays.toString(response.readBytes()));
    }
}
