//-----------------------------------------------------------------------------
// Colorize MultimediaLib
// Copyright 2009-2021 Colorize
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
    void simpleArgument() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("test", out);
        argParser.add("--input", "Input directory");
        argParser.tryParseArgs("--input", "/a/b/c");

        assertEquals("/a/b/c", argParser.get("input"));
        assertEquals("/a/b/c", argParser.get("Input"));
        assertEquals("/a/b/c", argParser.get("--input"));
        assertEquals("/a/b/c", argParser.getFile("input").getAbsolutePath());
    }

    @Test
    void showUsageInformation() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.add("--input", "Input directory");
        argParser.addOptional("--overwrite", "false", "Overwrites existing values");
        argParser.printUsage();

        String usage = "";
        usage += "Usage: MyApp\n";
        usage += "       <--input>      Input directory\n";
        usage += "       [--overwrite]  Overwrites existing values\n";

        assertEquals(usage, buffer.toString());
    }

    @Test
    void simpleFlag() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.add("--input", "Input directory");
        argParser.addOptional("--overwrite", "false", "Overwrites existing values");
        argParser.tryParseArgs("--input", "/tmp", "--overwrite");

        assertEquals("/tmp", argParser.get("input"));
        assertTrue(argParser.getBool("overwrite"));
    }

    @Test
    void showUsageIfMandatoryArgumentIsMissing() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.add("--input", "Input directory");

        assertThrows(CommandLineInterfaceException.class, () -> {
            argParser.tryParseArgs();
        });
    }

    @Test
    void doNotShowUsageIfNonMandatoryArgumentIsMissing() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.add("--input", "Input directory");
        argParser.addOptional("--overwrite", "false", "Overwrites existing values");
    }

    @Test
    void optionalArgumentWithDefaultValue() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.addOptional("--input", "/tmp", "Input directory");
        argParser.tryParseArgs();

        assertEquals("/tmp", argParser.get("input"));
    }

    @Test
    void relativeFilePath() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.add("--input", "Input directory");
        argParser.tryParseArgs("--input", "~/Documents");

        assertEquals("Documents", argParser.getFile("input").getName());
        assertEquals(Platform.getUserHomeDir().getAbsolutePath(),
            argParser.getFile("input").getParentFile().getAbsolutePath());
    }

    @Test
    void listOfFilePaths() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.add("--input", "Input directory");
        argParser.tryParseArgs("--input", "/Library,/tmp");

        assertEquals(2, argParser.getFileList("input").size());
        assertEquals("/Library", argParser.getFileList("input").get(0).getAbsolutePath());
        assertEquals("/tmp", argParser.getFileList("input").get(1).getAbsolutePath());
    }

    @Test
    void multipleFlags() {
        CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp", out);
        argParser.addOptional("--a", "false", "A");
        argParser.addOptional("--b", "false", "B");
        argParser.addOptional("--c", "", "C");
        argParser.tryParseArgs("--a", "--c", "test");

        assertTrue(argParser.getBool("a"));
        assertFalse(argParser.getBool("b"));
        assertEquals("test", argParser.get("c"));
    }
}
