//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVRecordTest {

    @Test
    public void testParse() {
        CSVRecord record = CSVRecord.parseRecord("a;2;3.4;false", ";");

        assertEquals(4, record.getCellCount());
        assertEquals("a", record.get(0));
        assertEquals(2, record.getInt(1));
        assertEquals(3.4, record.getFloat(2), 0.01f);
        assertEquals(false, record.getBoolean(3));
    }

    @Test
    public void testSerialize() {
        CSVRecord record = CSVRecord.parseRecord("a;2", ";");

        assertEquals("a;2", record.toCSV());
    }

    @Test
    public void testSerializeMultiple() {
        CSVRecord first = CSVRecord.create(Arrays.asList("a", "2"), ";");
        CSVRecord second = CSVRecord.create(Arrays.asList("b", "3"), ";");

        assertEquals("a;2\nb;3", CSVRecord.toCSV(ImmutableList.of(first, second)));
    }

    @Test
    public void testSerializeMultipleWithHeaders() {
        CSVRecord first = CSVRecord.create(Arrays.asList("a", "2"), ";");
        CSVRecord second = CSVRecord.create(Arrays.asList("b", "3"), ";");

        assertEquals("h1;h2\na;2\nb;3",
            CSVRecord.toCSV(ImmutableList.of(first, second), ImmutableList.of("h1", "h2")));
    }

    @Test
    public void testEscape() {
        CSVRecord record = CSVRecord.create(Arrays.asList("a", "b;c", "d\ne"), ";");

        assertEquals("a;bc;de", record.toCSV());
    }

    @Test
    public void testParseMultipleWithHeader() {
        List<CSVRecord> records = CSVRecord.parseRecords("h1;h2\na;2\nb;3", ";", true);

        assertEquals(2, records.size());
        assertEquals("a", records.get(0).get(0));
        assertEquals("b", records.get(1).get(0));
    }
}
