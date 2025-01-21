//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVRecordTest {

    @Test
    public void testParseMultipleWithHeader() {
        List<CSVRecord> records = CSVFormat.SEMICOLON.parseCSV("h1;h2\na;2\nb;3");

        assertEquals(2, records.size());
        assertEquals("a", records.get(0).get(0));
        assertEquals("b", records.get(1).get(0));
    }

    @Test
    void getColumnByName() {
        String csv = """
            Name;Age
            John;38
            """;

        List<CSVRecord> records = CSVFormat.SEMICOLON.parseCSV(csv);

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

        List<CSVRecord> records = CSVFormat.withoutHeaders(';').parseCSV(csv);

        assertEquals(3, records.size());
        assertEquals("Name", records.get(0).get(0));
        assertEquals("John", records.get(1).get(0));
        assertEquals("Jim", records.get(2).get(0));
    }

    @Test
    void getColumns() {
        String csv = """
            Name;Age
            John;38
            """;

        List<CSVRecord> records = CSVFormat.SEMICOLON.parseCSV(csv);

        assertEquals(1, records.size());
        assertEquals(List.of("Name", "Age"), records.get(0).getColumns());
        assertEquals("[(Name, John), (Age, 38)]", records.get(0).getColumnValues().toString());
    }

    @Test
    void parseEmptyCSV() {
        assertEquals(0, CSVFormat.SEMICOLON.parseCSV("").size());
    }
}
