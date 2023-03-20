//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import nl.colorize.util.stats.Tuple;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeadersTest {

    @Test
    void keepOrder() {
        Headers headers = new Headers()
            .append("Content-Type", "application/json")
            .append("Accept", "application/json");

        String expected = """
            Content-Type: application/json
            Accept: application/json
            """;

        assertEquals(expected, headers.toString());
    }

    @Test
    void allowSameHeaderMultipleTimes() {
        Headers headers = new Headers()
            .append("Content-Type", "application/json")
            .append("Test", "2")
            .append("Test", "3");

        String expected = """
            Content-Type: application/json
            Test: 2
            Test: 3
            """;

        assertEquals(expected, headers.toString());
    }

    @Test
    void headerNamesAreCaseInsensitive() {
        Headers headers = new Headers()
            .append("Test", "2")
            .append("test", "3");

        assertEquals("2", headers.get("Test").orElse(null));
        assertEquals("2", headers.get("test").orElse(null));
        assertEquals(List.of("2", "3"), headers.getAll("Test"));
        assertEquals(List.of("2", "3"), headers.getAll("test"));
    }

    @Test
    void appendHeaders() {
        Headers headers = new Headers(Tuple.of("A", "2"), Tuple.of("B", "3"))
            .append("A", "4")
            .append("C", "5");

        String expected = """
            A: 2
            B: 3
            A: 4
            C: 5
            """;

        assertEquals(expected, headers.toString());
    }

    @Test
    void replaceHeaders() {
        Headers headers = new Headers(Tuple.of("A", "2"), Tuple.of("B", "3"))
            .replace("A", "4")
            .replace("C", "5");

        String expected = """
            B: 3
            A: 4
            C: 5
            """;

        assertEquals(expected, headers.toString());
    }

    @Test
    void cannotUseInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            Tuple<String, String> header = Tuple.of("Some:Name", "Value");
            new Headers(header);
        });
    }

    @Test
    void cannotHaveNewLineInValue() {
        Headers headers = new Headers()
            .append("A", "2:3");

        assertThrows(IllegalArgumentException.class, () -> headers.append("A", "2\n3"));
    }

    @Test
    void retainHeaderNames() {
        Headers headers = new Headers()
            .append("A", "2")
            .append("B", "3")
            .append("A", "4")
            .append("a", "5");

        assertEquals(List.of("A", "B", "A", "a"), headers.getHeaderNames());
        assertEquals(List.of("2", "4", "5"), headers.getAll("A"));
        assertEquals(List.of("2", "4", "5"), headers.getAll("a"));
    }
}
