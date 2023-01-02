//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppPropertiesTest {

    @Test
    void convertDataTypes() {
        AppProperties data = AppProperties.fromPropertiesFile(new StringReader("a=2"));

        assertEquals(2, data.getInt("a", 0));
        assertEquals(0, data.getInt("b", 0));
    }

    @Test
    void deserializeProperties() {
        AppProperties data = AppProperties.from(LoadUtils.toProperties(Map.of("a", "b", "c", "3")));

        assertEquals("b", data.get("a", ""));
        assertEquals(3, data.getInt("c", 0));
    }

    @Test
    void emptyString() {
        AppProperties data = AppProperties.from(new Properties());

        assertEquals("", data.get("a", ""));
    }

    @Test
    void toMap() {
        Map<String, String> original = new HashMap<>();
        original.put("a", "2");
        original.put("b", null);
        original.put("c", "");

        AppProperties properties = AppProperties.fromMap(original);
        Map<String, String> map = properties.toPropertyMap();

        assertEquals("{a=2}", map.toString());
    }

    @Test
    void filterPrefix() {
        AppProperties data = AppProperties.fromPropertiesFile(
            new StringReader("a.x=2\na.y=3\nb.x=4\nb.y=5"));

        AppProperties filtered = data.filterPrefix("b.");

        assertEquals(Set.of("x", "y"), filtered.getPropertyNames());
        assertEquals("4", filtered.get("x"));
        assertEquals(5, filtered.getInt("y"));
    }

    @Test
    void getDate() {
        Date original = DateParser.parse("2022-01-09");
        Date alternative = DateParser.parse("2022-02-01");

        AppProperties data = AppProperties.fromPropertiesFile(new StringReader("a=09-01-2022"));

        assertEquals(original, data.getDate("a", alternative, "dd-MM-yyyy"));
        assertEquals(alternative, data.getDate("b", alternative, "dd-MM-yyyy"));
    }

    @Test
    void forEach() {
        AppProperties all = AppProperties.fromMap(Map.of("a1", "2", "b", "3", "a2", "4"));
        AppProperties filtered = all.filterPrefix("a");

        Set<String> allKeys = new HashSet<>();
        all.forEach((p, name) -> allKeys.add(name));

        assertEquals(Set.of("a1", "b", "a2"), allKeys);

        Set<String> filteredKeys = new HashSet<>();
        filtered.forEach((p, name) -> filteredKeys.add(name));

        assertEquals(Set.of("1", "2"), filteredKeys);
    }
}
