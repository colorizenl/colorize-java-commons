//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("john;38\n", CSVFormat.SEMICOLON.of("john", "38").toCSV());
    }

    @Test
    void escapeDelimiters() {
        assertEquals("john;38\n", CSVFormat.SEMICOLON.of("jo;hn", "38").toCSV());
    }

    @Test
    void quoteFields() {
        CSVFormat format = CSVFormat.SEMICOLON.withQuotes();

        assertEquals("\"john\"\"s name\";\"38\"\n", format.of("john\"s name", "38").toCSV());
    }

    @Test
    void noRecordsAreSerializedToEmptyString() {
        CSVFormat format = CSVFormat.withHeaders(';');

        assertEquals("", format.toCSV(List.of()));
    }

    @Test
    void serializeRecordsWithColumnInformation() {
        CSVFormat format = CSVFormat.withHeaders(';');
        CSVRecord a = format.of(List.of("name", "age"), "john", "38");
        CSVRecord b = format.of(List.of("name", "age"), "jim", "41");

        assertEquals("name;age\njohn;38\njim;41\n", format.toCSV(List.of(a, b)));
    }

    @Test
    void serializeRecordsWithoutColumnInformation() {
        CSVFormat format = CSVFormat.withoutHeaders(';');
        CSVRecord a = format.of("john", "38");
        CSVRecord b = format.of("jim", "41");

        assertEquals("john;38\njim;41\n", format.toCSV(List.of(a, b)));
    }

    @Test
    void doNotAllowMissingColumnInformation() {
        CSVFormat format = CSVFormat.withHeaders(';');
        CSVRecord a = format.of(List.of("name", "age"), "john", "38");
        CSVRecord b = format.of("jim", "41");

        assertThrows(IllegalStateException.class, () -> format.toCSV(List.of(a, b)));
    }

    @Test
    void doNotAllowInconsistentColumnInformation() {
        CSVFormat format = CSVFormat.withHeaders(';');
        CSVRecord a = format.of(List.of("name", "age"), "john", "38");
        CSVRecord b = format.of(List.of("name", "other"), "jim", "41");

        assertThrows(IllegalStateException.class, () -> format.toCSV(List.of(a, b)));
    }

    @Test
    void serializeObjects() {
        CSVFormat format = CSVFormat.withHeaders(';');
        List<String> columns = List.of("a", "b");
        List<Integer> rows = List.of(1, 2);
        String csv = format.serialize(columns, rows, e -> List.of("" + e, e + "/" + e));

        assertEquals("a;b\n1;1/1\n2;2/2\n", csv);
    }

    @Test
    void deserializeObjects() {
        CSVFormat format = CSVFormat.withHeaders(';');
        String csv = "a;b\n1;2\n3;4\n";
        List<Integer> result = format.deserialize(csv, record -> Integer.parseInt(record.get("b")));

        assertEquals(List.of(2, 4), result);
    }

    @Test
    void createRecord() {
        CSVFormat format = CSVFormat.withHeaders(';');

        assertTrue(format.of(List.of("a", "b"), List.of("1", "2")).hasColumnInformation());
        assertTrue(format.of(List.of("a", "b"), "1", "2").hasColumnInformation());
        assertFalse(format.of("1", "2").hasColumnInformation());
        assertThrows(IllegalArgumentException.class, () -> format.of(List.of("a", "b"), "1"));
    }
}
