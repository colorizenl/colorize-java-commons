//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadUtilsTest {

    @Test
    public void testReadFirstLines() throws IOException {
        assertEquals("", LoadUtils.readFirstLines(new StringReader(""), 2));
        assertEquals("a\nb", LoadUtils.readFirstLines(new StringReader("a\nb\n"), 2));
        assertEquals("a\nb", LoadUtils.readFirstLines(new StringReader("a\nb"), 2));
        assertEquals("a\nb", LoadUtils.readFirstLines(new StringReader("a\nb\nc\n"), 2));
        assertEquals("a", LoadUtils.readFirstLines(new StringReader("a\n"), 2));
    }
    
    @Test
    public void testLoadProperties() throws IOException {
        String str = "a=value\nb=valu\u00C9 2";
        Properties p1 = LoadUtils.loadProperties(new StringReader(str));

        assertEquals("value", p1.getProperty("a"));
        assertEquals("valu\u00C9 2", p1.getProperty("b"));
    }

    @Test
    public void testSaveProperties(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "a.properties");
        Properties props = LoadUtils.loadProperties(
            new StringReader("a=something\nb=test\na.x=2"));
        LoadUtils.saveProperties(props, tempFile, Charsets.UTF_8);
        
        String expected = "";
        expected += "a=something\n";
        expected += "a.x=2\n";
        expected += "b=test\n";
        
        assertTrue(tempFile.exists());
        assertEquals(expected, Files.readString(tempFile.toPath(), Charsets.UTF_8));
    }

    @Test
    void loadPropertiesFromString() {
        Properties properties = LoadUtils.loadProperties(new StringReader("a=1\nb=2"));

        assertEquals("1", properties.getProperty("a"));
        assertEquals("2", properties.getProperty("b"));
    }

    @Test
    void convertMapToProperties() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("a", "1");
        data.put("c", null);

        Properties properties = LoadUtils.toProperties(data);

        assertEquals("1", properties.getProperty("a"));
        assertEquals("3", properties.getProperty("c", "3"));
    }

    @Test
    void serializeProperties() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");

        assertEquals("a=2\n", LoadUtils.serializeProperties(properties));
    }

    @Test
    void propertiesToMap() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");

        Map<String, String> map = LoadUtils.toMap(properties);

        assertEquals("{a=2}", map.toString());
    }
}
