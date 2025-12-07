//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Describes the format of a CSV file, which is necessary since the CSV file
 * format is not fully standardized. Instances of the format can then be used
 * to parse CSV files into {@link CSVRecord}s, or to serialize records into
 * CSV files.
 * <p>
 * <strong>Note:</strong> This class is intended for basic CSV support.
 * When working with CSV files produced by other applications, prefer using
 * the <a href="https://github.com/osiegmar/FastCSV">FastCSV</a> library.
 */
public class CSVFormat {

    private boolean headers;
    private char delimiter;
    private String lineSeparator;
    private boolean quotes;

    public static final CSVFormat COMMA = withHeaders(',');
    public static final CSVFormat TAB = withHeaders('\t');
    public static final CSVFormat SEMICOLON = withHeaders(';');

    private static final CharMatcher NEWLINE_MATCHER = CharMatcher.is('\n');
    private static final CharMatcher QUOTE_MATCHER = CharMatcher.is('"');

    private CSVFormat(boolean headers, char delimiter, String lineSeparator, boolean quotes) {
        this.headers = headers;
        this.delimiter = delimiter;
        this.lineSeparator = lineSeparator;
        this.quotes = quotes;
    }

    public CSVFormat withQuotes() {
        return new CSVFormat(headers, delimiter, lineSeparator, true);
    }

    public CSVFormat withLineSeparator(String lineSeparator) {
        return new CSVFormat(headers, delimiter, lineSeparator, quotes);
    }

    /**
     * Returns a {@link CSVRecord} based on this CSV format and consisting of
     * the specified cells and column header information.
     *
     * @throws IllegalArgumentException if the number of column headers does
     *         not match the number of cells, or if there are no cells.
     */
    public CSVRecord of(List<String> columns, List<String> cells) {
        return new CSVRecord(columns, cells, this);
    }

    /**
     * Returns a {@link CSVRecord} based on this CSV format and consisting of
     * the specified cells and column header information.
     *
     * @throws IllegalArgumentException if the number of column headers does
     *         not match the number of cells, or if there are no cells.
     */
    public CSVRecord of(List<String> columns, String... cells) {
        return new CSVRecord(columns, List.of(cells), this);
    }

    /**
     * Returns a {@link CSVRecord} based on this CSV format and consisting of
     * the specified cells. The record will not have any column header
     * information.
     *
     * @throws IllegalArgumentException if there are no cells.
     */
    public CSVRecord of(String... cells) {
        return new CSVRecord(null, List.of(cells), this);
    }

    /**
     * Parses the specified CSV file using this {@link CSVFormat}, and returns
     * the resulting records. If this CSV format includes header information,
     * the first record in the file is assumed to contain the headers.
     */
    public List<CSVRecord> parseCSV(String csv) {
        List<CSVRecord> records = new ArrayList<>();
        List<String> columns = null;

        for (String line : Splitter.on(lineSeparator).omitEmptyStrings().split(csv)) {
            List<String> cells = parseLine(line);

            if (headers && columns == null) {
                columns = cells;
            } else {
                records.add(new CSVRecord(columns, cells, this));
            }
        }

        return records;
    }

    private List<String> parseLine(String line) {
        if (quotes) {
            return parseQuotedLine(line);
        } else {
            return Splitter.on(delimiter).splitToList(line);
        }
    }

    private List<String> parseQuotedLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean quoting = false;

        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == delimiter && !quoting) {
                cells.add(buffer.toString());
                buffer.delete(0, buffer.length());
            } else if (line.charAt(i) == '"') {
                quoting = !quoting;
                if (i > 0 && line.charAt(i - 1) == '\"') {
                    buffer.append('"');
                }
            } else {
                buffer.append(line.charAt(i));
            }
        }

        if (!buffer.isEmpty()) {
            cells.add(buffer.toString());
        }

        return cells;
    }

    /**
     * Serializes the specified records using this CSV format. If this format
     * includes column header information, the records will be preceded by a
     * record containing the column headers. If the list of records is empty,
     * this returns an empty string.
     *
     * @throws IllegalStateException if this CSV format includes column header
     *         information, but some of the records do not include column
     *         headers or include different column headers.
     */
    public String toCSV(List<CSVRecord> records) {
        StringBuilder buffer = new StringBuilder();
        List<String> usedColumns = Collections.emptyList();

        for (CSVRecord record : records) {
            if (headers) {
                if (usedColumns.isEmpty()) {
                    usedColumns = record.getColumns();
                    buffer.append(encodeRow(usedColumns));
                } else {
                    Preconditions.checkState(record.getColumns().equals(usedColumns));
                }
            }

            buffer.append(encodeRow(record.getCells()));
        }

        return buffer.toString();
    }

    /**
     * Serializes the specified record using this CSV format. The serialized
     * line will end with a trailing line separator.
     */
    public String toCSV(CSVRecord record) {
        return encodeRow(record.getCells());
    }

    /**
     * Serializes the specified record using this CSV format. The serialized
     * line will end with a trailing line separator. This method can be used
     * for both rows and headers, since CSV files do not differentiate
     * between the two apart from their location within the file.
     */
    private String encodeRow(List<String> cells) {
        Preconditions.checkArgument(!cells.isEmpty(), "Empty CSV record");

        return cells.stream()
            .map(this::encodeCell)
            .collect(Collectors.joining(String.valueOf(delimiter))) + lineSeparator;
    }

    private String encodeCell(String value) {
        CharMatcher delimiterMatcher = CharMatcher.is(delimiter);
        String cell = NEWLINE_MATCHER.replaceFrom(value, " ");

        if (quotes) {
            return "\"" + QUOTE_MATCHER.replaceFrom(cell, "\"\"") + "\"";
        } else {
            return QUOTE_MATCHER.removeFrom(delimiterMatcher.removeFrom(cell));
        }
    }

    /**
     * Uses this CSV format to serialize a number of objects to CSV. The
     * specified callback function is used to map each object to a CSV record.
     *
     * @throws IllegalArgumentException if the number of column headers does
     *         not match the number of cells in the records.
     */
    public <T> String serialize(List<T> rows, Function<T, CSVRecord> mapper) {
        List<CSVRecord> records = rows.stream()
            .map(mapper)
            .toList();

        return toCSV(records);
    }

    /**
     * Uses this CSV format to serialize a number of objects to CSV. The
     * specified callback function is used to map each object to a CSV record,
     * with each record receiving the column header information from
     * {@code columns}.
     *
     * @throws IllegalArgumentException if the number of column headers does
     *         not match the number of cells in the records.
     */
    public <T> String serialize(List<String> columns, List<T> rows, Function<T, List<String>> mapper) {
        return serialize(rows, row -> {
            List<String> cells = mapper.apply(row);
            return of(columns, cells);
        });
    }

    /**
     * Uses this CSV format to deserialize CSV records into objects. The
     * specified callback function is used to map each CSV record to an object.
     */
    public <T> List<T> deserialize(List<CSVRecord> records, Function<CSVRecord, T> mapper) {
        return records.stream()
            .map(mapper)
            .toList();
    }

    /**
     * Uses this CSV format to deserialize CSV records into objects. The
     * specified callback function is used to map each CSV record to an object.
     */
    public <T> List<T> deserialize(String csv, Function<CSVRecord, T> mapper) {
        List<CSVRecord> records = parseCSV(csv);
        return deserialize(records, mapper);
    }

    /**
     * Creates a {@link CSVFormat} with the specified delimiter, which includes
     * column header information. The first record in the CSV will be used to
     * provide column headers to the remaining records.
     */
    public static CSVFormat withHeaders(char delimiter) {
        return new CSVFormat(true, delimiter, System.lineSeparator(), false);
    }

    /**
     * Creates a {@link CSVFormat} with the specified delimiter, which does not
     * include column header information. The first record in the CSV will be
     * parsed as a normal record, not as column headers.
     */
    public static CSVFormat withoutHeaders(char delimiter) {
        return new CSVFormat(false, delimiter, System.lineSeparator(), false);
    }
}
