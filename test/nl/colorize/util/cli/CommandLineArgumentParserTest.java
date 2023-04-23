//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineArgumentParserTest {

    private StringWriter buffer;
    private PrintWriter out;

    @BeforeEach
    public void before() {
        buffer = new StringWriter();
        out = new PrintWriter(buffer, true);
    }

    @Test
    void simpleCommandLineInterface() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        Example values = argParser.parse(toArgs("--a", "123", "--b", "4", "-c", "--e", "567"),
            Example.class);

        assertEquals("123", values.a);
        assertEquals(4, values.b);
        assertTrue(values.c);
        assertEquals("567", values.d);
    }

    @Test
    void optionalArgumentsUseDefaultValues() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        Example values = argParser.parse(toArgs("--a", "123"), Example.class);

        assertEquals("123", values.a);
        assertEquals(2, values.b);
        assertFalse(values.c);
        assertNull(values.d);
    }

    @Test
    void missingRequiredArgumentCausesException() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--b", "4"), Example.class);
        });
    }

    @Test
    void invalidArgumentTypeCausesException() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--a", "123", "--b", "test"), Example.class);
        });
    }

    @Test
    void missingValueThrowsException() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--a", "123", "--b", "--c"), Example.class);
        });
    }

    @Test
    void throwExceptionForUnknownArgument() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--a", "123", "unexpected"), Example.class);
        });

        String expected = """
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
                        
            Unexpected argument 'unexpected'
            """;

        assertEquals(expected.strip(), buffer.toString().strip());
    }

    @Test
    void printUsage() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        argParser.printUsage(Example.class);

        String expected = """
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
            """;

        assertEquals(expected, buffer.toString());
    }

    @Test
    void printUsageWithDescription() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        argParser.addDescription("This is a description message.");
        argParser.printUsage(Example.class);

        String expected = """
            This is a description message.
            
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
            """;

        assertEquals(expected, buffer.toString());
    }

    private String[] toArgs(String... argv) {
        return argv;
    }

    private static class Example {
        private @Arg String a;
        private @Arg(defaultValue = "2", usage = "This field has usage information") int b;
        private @Arg boolean c;
        private @Arg(name = "e", required = false) String d;
    }
}
