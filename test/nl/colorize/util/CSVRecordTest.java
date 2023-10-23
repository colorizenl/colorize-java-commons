//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CSVRecordTest {

    @Test
    public void testParse() {
        CSVRecord record = CSVRecord.parseRecord("a;2;3.4;false", ";");

        assertEquals("a", record.get(0));
        assertEquals("2", record.get(1));
        assertEquals("3.4", record.get(2));
        assertEquals("false", record.get(3));
    }

    @Test
    public void testSerialize() {
        CSVRecord record = CSVRecord.parseRecord("a;2", ";");

        assertEquals("a;2", record.toString());
    }

    @Test
    public void testSerializeMultiple() {
        CSVRecord first = CSVRecord.create(Arrays.asList("a", "2"));
        CSVRecord second = CSVRecord.create(Arrays.asList("b", "3"));

        assertEquals("a;2\nb;3", CSVRecord.toCSV(List.of(first, second), ";", false));
    }

    @Test
    public void testEscape() {
        CSVRecord record = CSVRecord.create(Arrays.asList("a", "b;c", "d\ne"));

        assertEquals("a;bc;de", record.toString());
    }

    @Test
    public void testParseMultipleWithHeader() {
        List<CSVRecord> records = CSVRecord.parseCSV("h1;h2\na;2\nb;3", ";", true);

        assertEquals(2, records.size());
        assertEquals("a", records.get(0).get(0));
        assertEquals("b", records.get(1).get(0));
    }

    @Test
    void returnEmptyStringIfNoRecords() {
        assertEquals("", CSVRecord.toCSV(Collections.emptyList(), ";", false));
    }

    @Test
    void getColumnByName() {
        String csv = """
            Name;Age
            John;38
            """;

        List<CSVRecord> records = CSVRecord.parseCSV(csv, ";", true);

        assertEquals("John", records.get(0).get("Name"));
        assertEquals("38", records.get(0).get("Age"));
    }

    @Test
    void parseWithoutHeaders() {
        String csv = """
            Name;Age
            John;38
            Jim;26
            """;

        List<CSVRecord> records = CSVRecord.parseCSV(csv, ";", false);

        assertEquals(3, records.size());
        assertEquals("Name", records.get(0).get(0));
        assertEquals("John", records.get(1).get(0));
        assertEquals("Jim", records.get(2).get(0));
    }

    @Test
    void serializeWithHeaders() {
        CSVRecord record = CSVRecord.create(List.of("Name", "Age"), List.of("John", "38"));
        String csv = CSVRecord.toCSV(List.of(record), ";", true);

        String expected = """
            Name;Age
            John;38""";

        assertEquals(expected, csv);
    }

    @Test
    void serializeWithoutHeaders() {
        CSVRecord record = CSVRecord.create(List.of("Name", "Age"), List.of("John", "38"));
        String csv = CSVRecord.toCSV(List.of(record), ";", false);

        assertEquals("John;38", csv);
    }

    @Test
    void exceptionIfHeadersAreRequestedButNotAvailable() {
        CSVRecord record = CSVRecord.create(List.of("John", "38"));

        assertThrows(IllegalStateException.class,
            () -> CSVRecord.toCSV(List.of(record), ";", true));
    }

    @Test
    void exceptionForInconsistentHeaders() {
        CSVRecord recordA = CSVRecord.create(List.of("Name", "Age"), List.of("John", "38"));
        CSVRecord recordB = CSVRecord.create(List.of("Name", "Other"), List.of("Jim", "26"));

        assertThrows(IllegalStateException.class,
            () -> CSVRecord.toCSV(List.of(recordA, recordB), ";", true));
    }

    @Test
    void getColumns() {
        String csv = """
            Name;Age
            John;38
            """;

        List<CSVRecord> records = CSVRecord.parseCSV(csv, ";", true);

        assertEquals(1, records.size());
        assertEquals(List.of("Name", "Age"), records.get(0).getColumns());
        assertEquals("[(Name, John), (Age, 38)]", records.get(0).getColumnValues().toString());
    }

    @Test
    void parseEmptyCSV() {
        assertEquals(0, CSVRecord.parseCSV("", ";", true).size());
    }
}
