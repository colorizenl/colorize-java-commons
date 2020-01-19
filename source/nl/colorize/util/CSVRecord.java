//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializes data to and parses it from CSV. Cells are separated by a delimiter,
 * and record are separated by newlines. Cells can only be referenced by index,
 * not by column name.
 */
public class CSVRecord {

    private List<String> cells;
    private String delimiter;

    private static final Splitter RECORD_SPLITTER = Splitter.on("\n").omitEmptyStrings();
    private static final Joiner RECORD_JOINER = Joiner.on("\n");

    private CSVRecord(List<String> cells, String delimiter) {
        Preconditions.checkArgument(!cells.isEmpty(), "CSV record is empty");
        Preconditions.checkArgument(delimiter.length() == 1,
            "Invalid CSV delimiter: " + delimiter);

        this.cells = ImmutableList.copyOf(cells);
        this.delimiter = delimiter;
    }

    public int getCellCount() {
        return cells.size();
    }

    public String get(int index) {
        Preconditions.checkArgument(index < cells.size(),
            "Invalid index " + index + ", record has " + cells.size() + " cells");
        return cells.get(index);
    }

    public int getInt(int index) {
        return Integer.parseInt(get(index));
    }

    public float getFloat(int index) {
        return Float.parseFloat(get(index));
    }

    public boolean getBoolean(int index) {
        return get(index).equals("true");
    }

    public String toCSV() {
        CharMatcher escaper = CharMatcher.anyOf("\r\n" + delimiter);

        List<String> normalizedCells = cells.stream()
            .map(cell -> escaper.removeFrom(cell))
            .collect(Collectors.toList());

        Joiner cellJoiner = Joiner.on(delimiter).useForNull("");
        return cellJoiner.join(normalizedCells);
    }

    public static CSVRecord create(List<String> cells, String delimiter) {
        return new CSVRecord(cells, delimiter);
    }

    public static CSVRecord parseRecord(String csv, String delimiter) {
        Splitter cellSplitter = Splitter.on(delimiter);
        List<String> cells = cellSplitter.splitToList(csv);

        return new CSVRecord(cells, delimiter);
    }

    public static List<CSVRecord> parseRecords(String csv, String delimiter, boolean hasHeaders) {
        List<CSVRecord> records = RECORD_SPLITTER.splitToList(csv).stream()
            .map(row -> parseRecord(row, delimiter))
            .collect(Collectors.toList());

        if (hasHeaders && records.size() > 0) {
            records = records.subList(1, records.size());
        }

        return records;
    }

    public static String toCSV(List<CSVRecord> records) {
        Preconditions.checkArgument(records.size() > 0, "No records");

        List<String> rows = records.stream()
            .map(record -> record.toCSV())
            .collect(Collectors.toList());

        return RECORD_JOINER.join(rows);
    }

    public static String toCSV(List<CSVRecord> records, List<String> headers) {
        Preconditions.checkArgument(records.size() > 0, "No records");

        String delimiter = records.get(0).delimiter;
        CSVRecord headersRecord = create(headers, delimiter);

        return headersRecord.toCSV() + "\n" + toCSV(records);
    }
}
