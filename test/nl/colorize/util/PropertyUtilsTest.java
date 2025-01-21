//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertyUtilsTest {

    /**
     * Tests support for our custom implementation for loading .properties
     * files, using the file format example from Wikipedia.
     */
    @Test
    void loadProperties() throws IOException {
        String example = """
            # You are reading a comment in ".properties" file.
            ! The exclamation mark ('!') can also be used for comments.
            # Comments are ignored.
            # Blank lines are also ignored.
            
            # Lines with "properties" contain a key and a value separated by a delimiting character.
            # There are 3 delimiting characters: equal ('='), colon (':') and whitespace (' ', '\\t' and '\\f').
            website = https://en.wikipedia.org/
            language : English
            topic .properties files
            # A word on a line will just create a key with no value.
            empty
            # Whitespace that appears between the key, the delimiter and the value is ignored.
            # This means that the following are equivalent (other than for readability).
            hello=hello
            hello = hello
            # To start the value with whitespace, escape it with a backspace ('\\').
            whitespaceStart = \\ <-This space is not ignored.
            # Keys with the same name will be overwritten by the key that is the furthest in a file.
            # For example the final value for "duplicateKey" will be "second".
            duplicateKey = first
            duplicateKey = second
            # To use the delimiter characters inside a key, you need to escape them with a ('\\').
            # However, there is no need to do this in the value.
            delimiterCharacters\\:\\=\\ = This is the value for the key "delimiterCharacters\\:\\=\\ "
            # Adding a backslash ('\\') at the end of a line means that the value continues on the next line.
            multiline = This line \\
            continues
            # If you want your value to include a backslash ('\\'), it should be escaped by another backslash ('\\').
            path = c:\\\\wiki\\\\templates
            # This means that if the number of backslashes ('\\') at the end of the line is even, the next line is not included in the value.\s
            # In the following example, the value for "evenKey" is "This is on one line\\".
            # Whitespace characters at the beginning of a line is removed.
            # Make sure to add the spaces you need before the backslash ('\\') on the first line.\s
            # If you add them at the beginning of the next line, they will be removed.
            # In the following example, the value for "welcome" is "Welcome to Wikipedia!".
            welcome = Welcome to \\
                      Wikipedia!
            # If you need to add newlines and carriage returns, they need to be escaped using ('\\n') and ('\\r') respectively.
            # You can also optionally escape tabs with ('\\t') for readability purposes.
            valueWithEscapes = This is a newline\\n and a carriage return\\r and a tab\\t.
            # You can also use Unicode escape characters (maximum of four hexadecimal digits).
            # In the following example, the value for "encodedHelloInJapanese" is "こんにちは".
            encodedHelloInJapanese = \\u3053\\u3093\\u306b\\u3061\\u306f
            # But with more modern file encodings like UTF-8, you can directly use supported characters.
            helloInJapanese = こんにちは
            """;

        Properties properties = PropertyUtils.emulateLoadProperties(new StringReader(example));

        assertEquals("https://en.wikipedia.org/", properties.getProperty("website"));
        assertEquals("English", properties.getProperty("language"));
        assertEquals("hello", properties.getProperty("hello"));
        assertEquals("second", properties.getProperty("duplicateKey"));
        assertEquals("This line continues", properties.getProperty("multiline"));
        assertEquals("Welcome to Wikipedia!", properties.getProperty("welcome"));
        assertEquals("This is a newline\n and a carriage return\r and a tab\t.",
            properties.getProperty("valueWithEscapes"));
        assertEquals("こんにちは", properties.getProperty("helloInJapanese"));
    }

    @Test
    public void testLoadProperties() {
        String str = "a=value\nb=valu\u00C9 2";
        Properties p1 = PropertyUtils.loadProperties(new StringReader(str));

        assertEquals("value", p1.getProperty("a"));
        assertEquals("valu\u00C9 2", p1.getProperty("b"));
    }

    @Test
    public void testSaveProperties(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "a.properties");
        Properties props = PropertyUtils.loadProperties(
            new StringReader("a=something\nb=test\na.x=2"));
        PropertyUtils.saveProperties(props, tempFile, UTF_8);
        
        String expected = "";
        expected += "a=something\n";
        expected += "a.x=2\n";
        expected += "b=test\n";
        
        assertTrue(tempFile.exists());
        assertEquals(expected, Files.readString(tempFile.toPath(), UTF_8));
    }

    @Test
    void loadPropertiesFromString() {
        Properties properties = PropertyUtils.loadProperties(new StringReader("a=1\nb=2"));

        assertEquals("1", properties.getProperty("a"));
        assertEquals("2", properties.getProperty("b"));
    }

    @Test
    void convertMapToProperties() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("a", "1");
        data.put("c", null);

        Properties properties = PropertyUtils.toProperties(data);

        assertEquals("1", properties.getProperty("a"));
        assertEquals("3", properties.getProperty("c", "3"));
    }

    @Test
    void serializeProperties() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");

        assertEquals("a=2\n", PropertyUtils.serializeProperties(properties));
    }

    @Test
    void propertiesToMap() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");

        Map<String, String> map = PropertyUtils.toMap(properties);

        assertEquals("{a=2}", map.toString());
    }

    @Test
    void emulateUnicodeProperties() throws IOException {
        List<String> source = new ArrayList<>();
        source.add("a=some text");
        source.add("b=text that spans \\");
        source.add("  multiple lines");
        source.add("c=some other text");

        Properties properties = PropertyUtils.emulateLoadProperties(
            new StringReader(String.join("\n", source)));

        assertEquals("some text", properties.getProperty("a"));
        assertEquals("text that spans multiple lines", properties.getProperty("b"));
        assertEquals("some other text", properties.getProperty("c"));
    }

    @Test
    void filterPrefix() {
        Properties original = PropertyUtils.loadProperties(new StringReader("a.x=2\na.y=3\nb.x=4\nb.y=5"));
        Properties filtered = PropertyUtils.filterPrefix(original, "b.");

        assertEquals(Set.of("x", "y"), filtered.stringPropertyNames());
        assertEquals("4", filtered.getProperty("x"));
        assertEquals("5", filtered.getProperty("y"));
    }

    @Test
    void savePropertiesFileUTF8(@TempDir File tempDir) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("a", "b\u25B6c");

        File tempFile = new File(tempDir, "test.properties");
        PropertyUtils.saveProperties(properties, tempFile);

        assertEquals("a=b\u25B6c\n", Files.readString(tempFile.toPath(), UTF_8));
    }

    @Test
    void loadFromResourceFile() {
        ResourceFile file = new ResourceFile("custom-swing-components.properties");
        Properties properties = PropertyUtils.loadProperties(file);

        assertEquals("OK", properties.getProperty("Popups.ok"));
    }

    @Test
    void loadFromFile(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "test.properties");
        Files.writeString(file.toPath(), "a=This is a test", UTF_8);
        Properties properties = PropertyUtils.loadProperties(file);

        assertEquals("This is a test", properties.getProperty("a"));
    }
}
