//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationTest {

    @Test
    void convertDataTypes() {
        Configuration data = Configuration.fromProperties();
        data.setInt("a", 2);

        assertEquals(2, data.getInt("a", 0));
        assertEquals(0, data.getInt("b", 0));
    }

    @Test
    void convertList() {
        Configuration data = Configuration.fromProperties();
        data.setList("test", ImmutableList.of("1", "2", "3"));

        assertEquals(ImmutableList.of("1", "2", "3"), data.getList("test"));
        assertEquals(Collections.emptyList(), data.getList("test2"));
    }

    @Test
    void deserializeProperties() {
        Configuration data = Configuration.fromProperties(ImmutableMap.of("a", "b", "c", "3"));

        assertEquals("b", data.get("a", ""));
        assertEquals(3, data.getInt("c", 0));
    }

    @Test
    void serialize() {
        Configuration data = Configuration.fromProperties();
        data.setInt("a", 2);
        data.setFloat("b", 3.5f);
        data.setBoolean("c", true);
        data.setList("d", ImmutableList.of("1", "2", "3"));

        assertEquals("a=2\nb=3.5\nc=true\nd=1,2,3\n", data.serialize());
    }

    @Test
    void emptyString() {
        Configuration data = Configuration.fromProperties();

        assertEquals("", data.get("a", ""));
    }

    @Test
    void filterPrefix() {
        Configuration data = Configuration.fromProperties();
        data.setInt("a.x", 2);
        data.setInt("a.y", 3);
        data.setInt("b.x", 4);
        data.setInt("b.y", 5);

        Map<String, String> result = new LinkedHashMap<>();
        data.filterPrefix("b.", (key, value) -> result.put(key, value));

        assertEquals(ImmutableMap.of("x", "4", "y", "5"), result);
    }
}
