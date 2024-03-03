//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertyUtilsTest {

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
        PropertyUtils.saveProperties(props, tempFile, Charsets.UTF_8);
        
        String expected = "";
        expected += "a=something\n";
        expected += "a.x=2\n";
        expected += "b=test\n";
        
        assertTrue(tempFile.exists());
        assertEquals(expected, Files.readString(tempFile.toPath(), Charsets.UTF_8));
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
    void emulateUnicodeProperties() {
        List<String> source = new ArrayList<>();
        source.add("a=some text");
        source.add("b=text that spans \\");
        source.add("  multiple lines");
        source.add("c=some other text");

        Properties properties = new Properties();
        PropertyUtils.emulateUnicodeProperties(String.join("\n", source), properties);

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

        assertEquals("a=b\u25B6c\n", Files.readString(tempFile.toPath(), Charsets.UTF_8));
    }
}
