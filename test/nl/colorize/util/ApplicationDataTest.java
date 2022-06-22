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
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationDataTest {

    @Test
    void convertDataTypes() {
        ApplicationData data = new ApplicationData(new Properties());
        data.set("a", 2);

        assertEquals(2, data.get("a", 0));
        assertEquals(0, data.get("b", 0));
    }

    @Test
    void convertList() {
        ApplicationData data = new ApplicationData(new Properties());
        data.set("test", ImmutableList.of("1", "2", "3"));

        assertEquals(ImmutableList.of("1", "2", "3"), data.get("test", Collections.emptyList()));
        assertEquals(Collections.emptyList(), data.get("test2", Collections.emptyList()));
    }

    @Test
    void deserializeProperties() {
        ApplicationData data = new ApplicationData(ImmutableMap.of("a", "b", "c", "3"));

        assertEquals("b", data.get("a", ""));
        assertEquals(3, data.get("c", 0));
    }

    @Test
    void serialize() {
        ApplicationData data = new ApplicationData(new Properties());
        data.set("a", 2);
        data.set("b", 3.5f);
        data.set("c", true);
        data.set("d", ImmutableList.of("1", "2", "3"));

        assertEquals("a=2\nb=3.5\nc=true\nd=1,2,3\n", data.serialize());
    }

    @Test
    void emptyString() {
        ApplicationData data = new ApplicationData(new Properties());

        assertEquals("", data.get("a", ""));
    }
}
