//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CSVFormatTest {

    @Test
    void parseCSV() {
        String csv = """
            name;age
            john;38
            jim;40
            """;

        List<CSVRecord> records = CSVFormat.SEMICOLON.parseCSV(csv);

        assertEquals(2, records.size());
        assertEquals("john", records.get(0).get(0));
        assertEquals("38", records.get(0).get(1));
        assertEquals("jim", records.get(1).get(0));
        assertEquals("40", records.get(1).get(1));
    }

    @Test
    void parseWithoutHeaders() {
        String csv = """
            john;38
            jim;40
            """;

        List<CSVRecord> records = CSVFormat.withoutHeaders(';').parseCSV(csv);

        assertEquals(2, records.size());
        assertEquals("john", records.get(0).get(0));
        assertEquals("38", records.get(0).get(1));
        assertEquals("jim", records.get(1).get(0));
        assertEquals("40", records.get(1).get(1));
    }

    @Test
    void parseEmptyCSV() {
        List<CSVRecord> records = CSVFormat.withHeaders(';').parseCSV("");

        assertEquals(0, records.size());
    }

    @Test
    void parseHeadersWithoutRows() {
        List<CSVRecord> records = CSVFormat.withHeaders(';').parseCSV("name;age");

        assertEquals(0, records.size());
    }

    @Test
    void parseQuotes() {
        String csv = """
            name;age
            "jo;hn";38
            jim;"old"
            "escaped ""quotes"" in cell";1
            """;

        List<CSVRecord> records = CSVFormat.SEMICOLON.withQuotes().parseCSV(csv);

        assertEquals(3, records.size());
        assertEquals("jo;hn", records.get(0).get(0));
        assertEquals("38", records.get(0).get(1));
        assertEquals("jim", records.get(1).get(0));
        assertEquals("old", records.get(1).get(1));
        assertEquals("escaped \"quotes\" in cell", records.get(2).get(0));
        assertEquals("1", records.get(2).get(1));
    }

    @Test
    void serializeRecords() {
        assertEquals("john;38\n", CSVFormat.SEMICOLON.toCSV("john", "38"));
    }

    @Test
    void escapeDelimiters() {
        assertEquals("john;38\n", CSVFormat.SEMICOLON.toCSV("jo;hn", "38"));
    }

    @Test
    void quoteFields() {
        CSVFormat format = CSVFormat.SEMICOLON.withQuotes();

        assertEquals("\"john\"\"s name\";\"38\"\n", format.toCSV("john\"s name", "38"));
    }
}
