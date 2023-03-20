//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyTest {

    @Test
    void deserializeDataTypes() {
        Map<String, String> values = Map.of(
            "a", "test",
            "b", "3",
            "c", "4.5",
            "d", "true"
        );

        Map<String, Property> propertyMap = Property.from(values);

        assertEquals("test", propertyMap.get("a").getStringOr("other"));
        assertEquals(3, propertyMap.get("b").getInt());
        assertEquals(4.5f, propertyMap.get("c").getFloat());
        assertTrue(propertyMap.get("d").getBool());
    }

    @Test
    void fromPropertiesFile(@TempDir File tempDir) throws IOException {
        Path propertiesFile = new File(tempDir, "test.properties").toPath();
        Files.writeString(propertiesFile, "a=test\nb=something", Charsets.UTF_8);

        ResourceFile resource = new ResourceFile(propertiesFile.toFile());
        Map<String, Property> propertyMap = Property.from(resource);

        assertEquals("test", propertyMap.get("a").getStringOr("other"));
        assertEquals("something", propertyMap.get("b").getStringOr("other"));
        assertEquals("other", propertyMap.get("c").getStringOr("other"));
    }

    @Test
    void absentPropertyIsAlwaysDefault() {
        Map<String, String> values = new HashMap<>();
        values.put("a", "test");
        values.put("b", null);
        values.put("c", "");

        Map<String, Property> propertyMap = Property.from(values);

        assertEquals("test", propertyMap.get("a").getStringOr("other"));
        assertEquals("other", propertyMap.get("b").getStringOr("other"));
        assertEquals("other", propertyMap.get("c").getStringOr("other"));
        assertEquals("other", propertyMap.get("d").getStringOr("other"));
    }

    @Test
    void missingRequiredPropertyThrowsException() {
        Property missing = Property.of(null);

        assertThrows(NoSuchElementException.class, missing::getString);
    }
}
