//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import nl.colorize.util.stats.Tuple;
import nl.colorize.util.stats.TupleList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeadersTest {

    @Test
    void keepOrder() {
        Headers headers = Headers.of(
            "Content-Type", "application/json",
            "Accept", "application/json"
        );

        String expected = """
            Content-Type: application/json
            Accept: application/json
            """;

        assertEquals(expected, headers.toString());
    }

    @Test
    void allowSameHeaderMultipleTimes() {
        Headers headers = Headers.of(
            "Content-Type", "application/json",
            "Test", "2",
            "Test", "3"
        );

        String expected = """
            Content-Type: application/json
            Test: 2
            Test: 3
            """;

        assertEquals(expected, headers.toString());
    }

    @Test
    void headerNamesAreCaseInsensitive() {
        Headers headers = Headers.of(
            "Test", "2",
            "test", "3"
        );

        assertEquals("2", headers.get("Test").orElse(null));
        assertEquals("2", headers.get("test").orElse(null));
        assertEquals(List.of("2", "3"), headers.getAll("Test"));
        assertEquals(List.of("2", "3"), headers.getAll("test"));
    }

    @Test
    void cannotUseInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            Tuple<String, String> header = Tuple.of("Some:Name", "Value");
            TupleList<String, String> headers = TupleList.of(header);
            new Headers(headers);
        });
    }

    @Test
    void cannotHaveNewLineInValue() {
        assertThrows(IllegalArgumentException.class, () -> Headers.of("A", "2\n3"));
    }

    @Test
    void retainHeaderNames() {
        Headers headers = Headers.of(
            "A", "2",
            "B", "3",
            "A", "4",
            "a", "5"
        );

        assertEquals(List.of("A", "B", "A", "a"), headers.getHeaderNames());
        assertEquals(List.of("2", "4", "5"), headers.getAll("A"));
        assertEquals(List.of("2", "4", "5"), headers.getAll("a"));
    }

    @Test
    void createFromEntries() {
        Headers headers = Headers.of("a", "b", "c", "d");

        assertEquals(List.of("a", "c"), headers.getHeaderNames());
        assertEquals("b", headers.get("a").orElse("?"));
        assertEquals("d", headers.get("c").orElse("?"));
    }

    @Test
    void createFromOddNumberOfEntriesCausesException() {
        assertThrows(IllegalArgumentException.class, () -> Headers.of("a", "b", "c"));
    }
}
