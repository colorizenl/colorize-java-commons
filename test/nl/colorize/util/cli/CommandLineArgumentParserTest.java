//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
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
        argParser.disableColor();

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--b", "4"), Example.class);
        });

        String expected = """
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
                        
            Missing required argument 'a'
            """;

        assertEquals(expected.strip(), buffer.toString().strip());
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
        argParser.disableColor();

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--a", "123", "unexpected"), Example.class);
        });

        String expected = """
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
                        
            Unknown argument 'unexpected'
            """;

        assertEquals(expected.strip(), buffer.toString().strip());
    }

    @Test
    void throwExceptionForMissingValue() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        argParser.disableColor();

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--a"), Example.class);
        });

        String expected = """
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
                        
            Missing value for argument 'a'
            """;

        assertEquals(expected.strip(), buffer.toString().strip());
    }

    @Test
    void throwExceptionWhenFlagArgumentHasValue() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        argParser.disableColor();

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--a", "test", "--c", "true"), Example.class);
        });

        String expected = """
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
                        
            Unknown argument 'true'
            """;

        assertEquals(expected.strip(), buffer.toString().strip());
    }

    @Test
    void printUsage() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        argParser.disableColor();
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
        argParser.disableColor();
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

    @Test
    void argumentProvidedMultipleTimes() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        argParser.disableColor();

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.parse(toArgs("--a", "1", "--a", "2"), Example.class);
        });

        String expected = """
            Usage: test
                --a   \s
                [--b]  This field has usage information
                [--c] \s
                [--e] \s
                        
            Duplicate argument 'a'
            """;

        assertEquals(expected.strip(), buffer.toString().strip());
    }

    @Test
    void handleEqualsSignInValue() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        Example values = argParser.parse(toArgs("--a", "b=c"), Example.class);

        assertEquals("b=c", values.a);
    }

    @Test
    void handleEqualsSignInBothEntryAndValue() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        Example values = argParser.parse(toArgs("--a=b=c"), Example.class);

        assertEquals("b=c", values.a);
    }

    @Test
    void argumentNameWithAlias() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out, false);
        Example values = argParser.parse(toArgs("--aaa", "something"), Example.class);

        assertEquals("something", values.a);
    }

    private String[] toArgs(String... argv) {
        return argv;
    }

    private static class Example {
        private @Arg(aliases = {"aaa"}) String a;
        private @Arg(defaultValue = "2", usage = "This field has usage information") int b;
        private @Arg boolean c;
        private @Arg(name = "e", required = false) String d;
    }
}
