//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import nl.colorize.util.Platform;
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
    void simpleArgument() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("test", out)
            .addRequired("--input", "Input directory")
            .tryParseArgs("--input", "/a/b/c");

        assertEquals("/a/b/c", args.get("input").getString());
    }

    @Test
    void normalizeArgumentName() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("test", out)
            .addRequired("--input", "Input directory")
            .tryParseArgs("--input", "/a/b/c");

        assertEquals("/a/b/c", args.get("input").getString());
        assertEquals("/a/b/c", args.get("--input").getString());
    }

    @Test
    void showUsageInformation() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.addRequired("--input", "Input directory");
        argParser.addOptional("--overwrite", "Overwrites existing values");
        argParser.printUsage();

        String usage = "";
        usage += "Usage: MyApp\n";
        usage += "       <--input>      Input directory\n";
        usage += "       [--overwrite]  Overwrites existing values\n";

        assertEquals(usage, buffer.toString());
    }

    @Test
    void simpleFlag() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addRequired("--input", "Input directory")
            .addFlag("--overwrite", "Overwrites existing values")
            .tryParseArgs("--input", "/tmp", "--overwrite");

        assertEquals("/tmp", args.get("input").getString());
        assertTrue(args.get("overwrite").getBool());
    }

    @Test
    void showUsageIfMandatoryArgumentIsMissing() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out)
            .addRequired("--input", "Input directory");

        assertThrows(CommandLineInterfaceException.class, () -> argParser.tryParseArgs());
    }

    @Test
    void optionalArgumentWithDefaultValue() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--input", "Input directory")
            .tryParseArgs();

        assertNull(args.get("input").getString());
        assertEquals("/tmp", args.get("input").getStringOr("/tmp"));
    }

    @Test
    void relativeFilePath() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addRequired("--input", "Input directory")
            .tryParseArgs("--input", "~/Documents");

        assertEquals("Documents", args.get("input").getFile().getName());
        assertEquals(Platform.getUserHomeDir().getAbsolutePath(),
            args.get("input").getFile().getParentFile().getAbsolutePath());
    }

    @Test
    void multipleFlags() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addFlag("--a", "A")
            .addFlag("--b", "B")
            .addFlag("--c", "C")
            .tryParseArgs("--a", "--c");

        assertTrue(args.get("a").getBool());
        assertFalse(args.get("b").getBool());
        assertTrue(args.get("c").getBool());
    }

    @Test
    void allowNullAsDefaultValue() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .addOptional("--b", "test")
            .tryParseArgs();

        assertNull(args.get("a").getString());
        assertEquals("B", args.get("b").getStringOr("B"));
    }

    @Test
    void allowEmptyAsDefault() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--exclude", "test")
            .tryParseArgs();

        assertEquals("", args.get("exclude").getStringOr(""));
    }

    @Test
    void allowBothSingleAndDoubleLeadingHyphen() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .tryParseArgs("-a", "2");

        assertEquals("2", args.get("a").getString());
    }

    @Test
    void allowRetrievalTimeDefaultValue() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .addOptional("--b", "test")
            .addOptional("--c", "test")
            .tryParseArgs();

        assertEquals("3", args.get("a").getStringOr("3"));
        assertNull(args.get("a").getString());
        assertEquals("3", args.get("c").getStringOr("3"));
    }

    @Test
    void optionalArgumentCanProduceNullValue() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .tryParseArgs();

        assertNull(args.get("a").getString());
    }

    @Test
    void allowNameEqualsValueNotationForArguments() throws CommandLineInterfaceException {
        CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .tryParseArgs("--a=b");

        assertEquals("b", args.get("a").getString());
    }
}
