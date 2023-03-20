//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import nl.colorize.util.stats.TupleList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Immutable record within a CSV file. Cells are separated by a delimiter,
 * records are separated by newlines. Cells can be referenced by column index,
 * or by column name if the CSV record includes column name information.
 */
public class CSVRecord {

    private List<String> columns;
    private List<String> cells;

    private static final Splitter RECORD_SPLITTER = Splitter.on("\n").omitEmptyStrings();
    private static final Joiner RECORD_JOINER = Joiner.on("\n");

    private CSVRecord(List<String> columns, List<String> cells) {
        Preconditions.checkArgument(!cells.isEmpty(),
            "CSV record is empty");

        Preconditions.checkArgument(columns.isEmpty() || columns.size() == cells.size(),
            "Number of columns does not match number of cells: " + columns + " <-> " + cells);

        this.columns = ImmutableList.copyOf(columns);
        this.cells = ImmutableList.copyOf(cells);
    }

    /**
     * Returns the value of the cell with the specified index. This method is
     * always available, while accessing cells by column name is only available
     * if the CSV containing column information.
     *
     * @throws IllegalArgumentException if the column index is invalid.
     */
    public String get(int index) {
        Preconditions.checkArgument(index >= 0 && index < cells.size(),
            "Invalid index " + index + ", record has " + cells.size() + " cells");

        return cells.get(index);
    }

    /**
     * Returns the value for the cell with the specified column name.
     *
     * @throws IllegalStateException if no column name information is available.
     * @throws IllegalArgumentException if no column with that name exists.
     */
    public Property get(String column) {
        Preconditions.checkState(!columns.isEmpty(), "No column name information available");
        Preconditions.checkArgument(columns.contains(column), "No such column: " + column);

        int columnIndex = columns.indexOf(column);
        String value = cells.get(columnIndex);
        return Property.of(value);
    }

    /**
     * Returns the list of cells in this CSV record. The list will be sorted to
     * match the order in which the cells appear in the CSV.
     */
    public List<String> getCells() {
        return ImmutableList.copyOf(cells);
    }

    public boolean hasColumnNameInformation() {
        return !columns.isEmpty();
    }

    /**
     * Returns a list of all named columns in this CSV record. The columns will
     * be sorted in the same order as they appear in the CSV. If no column name
     * information is available, this will return an empty list.
     *
     * @throws IllegalStateException if no column name information is available.
     */
    public List<String> getColumns() {
        Preconditions.checkState(!columns.isEmpty(), "No column name information available");
        return ImmutableList.copyOf(columns);
    }

    /**
     * Returns a list of column values for all cells in this CSV record. Each
     * tuple in the list consists of the column name and corresponding cell
     * value. The list will be sorted to match the order in which the columns
     * appear in the CSV.
     *
     * @throws IllegalStateException if no column name information is available.
     */
    public TupleList<String, String> getColumnValues() {
        Preconditions.checkState(!columns.isEmpty(), "No column name information available");

        TupleList<String, String> tuples = TupleList.create();
        for (int i = 0; i < columns.size(); i++) {
            tuples.add(columns.get(i), cells.get(i));
        }
        return tuples;
    }

    private String toCSV(String delimiter) {
        Preconditions.checkArgument(delimiter.length() == 1,
            "Invalid CSV delimiter: " + delimiter);

        CharMatcher escaper = CharMatcher.anyOf("\r\n" + delimiter);

        return cells.stream()
            .map(cell -> cell == null ? "" : cell)
            .map(escaper::removeFrom)
            .collect(Collectors.joining(delimiter));
    }

    @Override
    public String toString() {
        return toCSV(";");
    }

    /**
     * Creates an indivudual CSV record from the specified list of cells and
     * column headers.
     */
    public static CSVRecord create(List<String> columns, List<String> cells) {
        return new CSVRecord(columns, cells);
    }

    /**
     * Creates an indivudual CSV record from the specified list of cells, with
     * no column headers.
     */
    public static CSVRecord create(List<String> cells) {
        return new CSVRecord(Collections.emptyList(), cells);
    }

    /**
     * Creates an individual CSV record from the specified array of cells.
     */
    public static CSVRecord create(String... cells) {
        return new CSVRecord(Collections.emptyList(), ImmutableList.copyOf(cells));
    }

    /**
     * Parses an individual CSV record, using the specified delimiter.
     */
    public static CSVRecord parseRecord(String csv, String delimiter) {
        Preconditions.checkArgument(delimiter.length() == 1,
            "Invalid CSV delimiter: " + delimiter);

        List<String> cells = Splitter.on(delimiter).splitToList(csv);
        return new CSVRecord(Collections.emptyList(), cells);
    }

    /**
     * Parses a CSV file containing any number of record. Cells within each
     * record are split using the specified delimiter. If {@code headers}
     * is true, the first record is used as the column header names. If false,
     * the first record is considered as a normal record, and no column header
     * information will be available.
     */
    public static List<CSVRecord> parseCSV(String csv, String delimiter, boolean headers) {
        Preconditions.checkArgument(delimiter.length() == 1,
            "Invalid CSV delimiter: " + delimiter);

        if (csv.isEmpty()) {
            return Collections.emptyList();
        }

        List<CSVRecord> records = new ArrayList<>();
        List<String> columns = Collections.emptyList();

        for (String row : RECORD_SPLITTER.split(csv)) {
            List<String> cells = parseRecord(row, delimiter).cells;
            if (headers && columns.isEmpty()) {
                columns = cells;
            } else {
                CSVRecord record = new CSVRecord(columns, cells);
                records.add(record);
            }
        }

        return records;
    }

    /**
     * Serializes a list of records to CSV, using the specified cell delimiter.
     * If the list of records is empty this will return an empty string. If
     * {@code headers} is true, the header information in the first record will
     * be used to include column headers in the generated CSV. If false, no
     * header information will be included.
     *
     * @throws IllegalStateException if {@code headers} is true, but no column
     *         header information is available for the first record. Also thrown
     *         when {@code headers} is true, but the records in list do not share
     *         the same headers.
     */
    public static String toCSV(List<CSVRecord> records, String delimiter, boolean headers) {
        if (records.isEmpty()) {
            return "";
        }

        List<String> rows = new ArrayList<>();

        for (CSVRecord record : records) {
            if (headers) {
                Preconditions.checkState(record.columns.size() > 0,
                    "Record missing column headers: " + record);
                Preconditions.checkState(record.columns.equals(records.get(0).columns),
                    "Records have inconsistent columns: " + record);

                if (rows.isEmpty()) {
                    CSVRecord headerRecord = CSVRecord.create(record.columns);
                    rows.add(headerRecord.toCSV(delimiter));
                }
            }

            rows.add(record.toCSV(delimiter));
        }

        return RECORD_JOINER.join(rows);
    }
}
