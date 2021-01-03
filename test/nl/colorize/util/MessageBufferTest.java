//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageBufferTest {

    @Test
    void flushBuffer() {
        MessageBuffer<String> buffer = new MessageBuffer<>();
        buffer.add("123");
        buffer.add("456");

        assertEquals(ImmutableList.of("123", "456"), buffer.flush());

        buffer.add("789");

        assertEquals(ImmutableList.of("789"), buffer.flush());
    }

    @Test
    void resetBuffer() {
        MessageBuffer<String> buffer = new MessageBuffer<>();
        buffer.add("123");
        buffer.add("456");
        buffer.reset();
        buffer.add("789");

        assertEquals(ImmutableList.of("789"), buffer.flush());
    }
}
