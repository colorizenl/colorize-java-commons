//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializes data to and parses it from CSV. Cells are separated by a delimiter,
 * and record are separated by newlines. Cells can only be referenced by index,
 * not by column name.
 */
public class CSVRecord {

    private List<String> cells;

    private static final Splitter RECORD_SPLITTER = Splitter.on("\n").omitEmptyStrings();
    private static final Joiner RECORD_JOINER = Joiner.on("\n");

    private CSVRecord(List<String> cells) {
        Preconditions.checkArgument(!cells.isEmpty(), "CSV record is empty");
        this.cells = ImmutableList.copyOf(cells);
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

    public double getDouble(int index) {
        return Double.parseDouble(get(index));
    }

    public boolean getBoolean(int index) {
        return get(index).equals("true");
    }

    public Date getDate(int index, String dateFormat) {
        try {
            return new SimpleDateFormat(dateFormat).parse(get(index));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Malformed date: " + get(index));
        }
    }

    /**
     * Serializes this record to CSV using the specified delimiter. For creating
     * CSV files consisting of multiple records, use {@link #toCSV(List, String)}
     * or {@link #toCSV(List, List, String)}.
     */
    protected String toCSV(String delimiter) {
        Preconditions.checkArgument(delimiter.length() == 1,
            "Invalid CSV delimiter: " + delimiter);

        CharMatcher escaper = CharMatcher.anyOf("\r\n" + delimiter);

        List<String> normalizedCells = cells.stream()
            .map(escaper::removeFrom)
            .collect(Collectors.toList());

        Joiner cellJoiner = Joiner.on(delimiter).useForNull("");
        return cellJoiner.join(normalizedCells);
    }

    @Override
    public String toString() {
        return toCSV(";");
    }

    /**
     * Creates a CSV record from the specified list of cells.
     *
     * @throws IllegalArgumentException when trying to create a record without
     *         any cells.
     */
    public static CSVRecord create(List<String> cells) {
        return new CSVRecord(cells);
    }

    /**
     * Creates a CSV record from the specified array of cells.
     *
     * @throws IllegalArgumentException when trying to create a record without
     *         any cells.
     */
    public static CSVRecord create(String... cells) {
        return new CSVRecord(ImmutableList.copyOf(cells));
    }

    /**
     * Parses an individual CSV record, using the specified delimiter.
     */
    public static CSVRecord parseRecord(String csv, String delimiter) {
        Splitter cellSplitter = Splitter.on(delimiter);
        List<String> cells = cellSplitter.splitToList(csv);
        return new CSVRecord(cells);
    }

    /**
     * Parses a number of CSV records, using the specified delimiter. If
     * {@code hasHeaders} is true, the first record is assumed to be the
     * headers and will not be included in the results.
     */
    public static List<CSVRecord> parseRecords(String csv, String delimiter, boolean hasHeaders) {
        if (csv.isEmpty()) {
            return Collections.emptyList();
        }

        List<CSVRecord> records = RECORD_SPLITTER.splitToList(csv).stream()
            .map(row -> parseRecord(row, delimiter))
            .collect(Collectors.toList());

        if (hasHeaders && records.size() > 0) {
            records = records.subList(1, records.size());
        }

        return records;
    }

    /**
     * Serializes a list of records to CSV, using the specified cell delimiter.
     * If the list of records is empty this will return an empty string.
     */
    public static String toCSV(List<CSVRecord> records, String delimiter) {
        List<String> rows = records.stream()
            .map(record -> record.toCSV(delimiter))
            .collect(Collectors.toList());

        return RECORD_JOINER.join(rows);
    }

    /**
     * Serializes a list of records to CSV, using the specified cell delimiter
     * and list of row headers.
     *
     * @throws IllegalArgumentException if the number of headers does not match
     *         the number of cells.
     */
    public static String toCSV(List<CSVRecord> records, List<String> headers, String delimiter) {
        List<CSVRecord> all = new ArrayList<>();
        all.add(new CSVRecord(headers));

        for (CSVRecord record : records) {
            Preconditions.checkArgument(record.getCellCount() == headers.size(),
                "Expected " + headers.size() + " cells but got " + record);
            all.add(record);
        }

        return toCSV(all, delimiter);
    }
}
