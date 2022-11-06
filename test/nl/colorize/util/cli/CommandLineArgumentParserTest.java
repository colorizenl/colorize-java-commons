//-----------------------------------------------------------------------------
// Colorize MultimediaLib
// Copyright 2009-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import nl.colorize.util.AppProperties;
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
        AppProperties args = new CommandLineArgumentParser("test", out)
            .addRequired("--input", "Input directory")
            .tryParseArgs("--input", "/a/b/c");

        assertEquals("/a/b/c", args.get("input"));
    }

    @Test
    void normalizeArgumentName() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("test", out)
            .addRequired("--input", "Input directory")
            .tryParseArgs("--input", "/a/b/c");

        assertEquals("/a/b/c", args.get("input"));
        assertEquals("/a/b/c", args.get("--input"));
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
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addRequired("--input", "Input directory")
            .addFlag("--overwrite", "Overwrites existing values")
            .tryParseArgs("--input", "/tmp", "--overwrite");

        assertEquals("/tmp", args.get("input"));
        assertTrue(args.getBoolean("overwrite"));
    }

    @Test
    void showUsageIfMandatoryArgumentIsMissing() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out)
            .addRequired("--input", "Input directory");

        assertThrows(CommandLineInterfaceException.class, () -> argParser.tryParseArgs());
    }

    @Test
    void doNotShowUsageIfNonMandatoryArgumentIsMissing() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.addRequired("--input", "Input directory");
        argParser.addOptional("--overwrite", "Overwrites existing values");
    }

    @Test
    void optionalArgumentWithDefaultValue() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--input", "Input directory")
            .tryParseArgs();

        assertNull(args.get("input"));
        assertEquals("/tmp", args.get("input", "/tmp"));
    }

    @Test
    void relativeFilePath() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addRequired("--input", "Input directory")
            .tryParseArgs("--input", "~/Documents");

        assertEquals("Documents", args.getFile("input").getName());
        assertEquals(Platform.getUserHomeDir().getAbsolutePath(),
            args.getFile("input").getParentFile().getAbsolutePath());
    }

    @Test
    void multipleFlags() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addFlag("--a", "A")
            .addFlag("--b", "B")
            .addFlag("--c", "C")
            .tryParseArgs("--a", "--c");

        assertTrue(args.getBoolean("a"));
        assertFalse(args.getBoolean("b"));
        assertTrue(args.getBoolean("c"));
    }

    @Test
    void allowNullAsDefaultValue() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .addOptional("--b", "test")
            .tryParseArgs();

        assertNull(args.get("a"));
        assertEquals("B", args.get("b", "B"));
    }

    @Test
    void allowEmptyAsDefault() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--exclude", "test")
            .tryParseArgs();

        assertEquals("", args.get("exclude", ""));
    }

    @Test
    void allowBothSingleAndDoubleLeadingHyphen() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .tryParseArgs("-a", "2");

        assertEquals("2", args.get("a"));
    }

    @Test
    void allowRetrievalTimeDefaultValue() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .addOptional("--b", "test")
            .addOptional("--c", "test")
            .tryParseArgs();

        assertEquals("3", args.get("a", "3"));
        assertNull(args.get("a"));
        assertEquals("3", args.get("c", "3"));
    }

    @Test
    void optionalArgumentCanProduceNullValue() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .tryParseArgs();

        assertNull(args.get("a"));
    }

    @Test
    void allowNameEqualsValueNotationForArguments() throws CommandLineInterfaceException {
        AppProperties args = new CommandLineArgumentParser("MyApp", out)
            .addOptional("--a", "test")
            .tryParseArgs("--a=b");

        assertEquals("b", args.get("a"));
    }
}
