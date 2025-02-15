//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnsiColorTest {

    @Test
    void formatEscapeCodes() {
        assertEquals("\u001B[31mtest\u001B[0m", AnsiColor.RED.format("test"));
        assertEquals("\u001B[1m\u001B[33mtest\u001B[0m", AnsiColor.YELLOW_BOLD.format("test"));
    }

    @Test
    void clean() {
        assertEquals("test", AnsiColor.clean("\u001B[31mtest\u001B[0m"));
        assertEquals("test", AnsiColor.clean("\u001B[1m\u001B[33mtest\u001B[0m"));
        assertEquals("This is a test", AnsiColor.clean("This is a \u001B[31;1mtest\u001B[0m"));
    }
}
