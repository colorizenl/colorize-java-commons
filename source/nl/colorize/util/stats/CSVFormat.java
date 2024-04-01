//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;
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
            } else if (line.charAt(i) == '\"') {
                quoting = !quoting;
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
     * Serializes the specified record using this CSV format. The serialized
     * line will end with a trailing line separator. This method can be used
     * for both rows and headers, since CSV files do not differentiate
     * between the two apart from their location within the file.
     *
     * @throws IllegalArgumentException if the record does not contain at
     *         least one cell.
     */
    public String toCSV(List<String> cells) {
        Preconditions.checkArgument(!cells.isEmpty(), "Empty CSV record");

        CharMatcher illegalCharacters = CharMatcher.anyOf("\r\n\"" + delimiter);

        return cells.stream()
            .map(illegalCharacters::removeFrom)
            .collect(Collectors.joining(String.valueOf(delimiter))) + lineSeparator;
    }

    /**
     * Serializes the specified record using this CSV format. The serialized
     * line will end with a trailing line separator. This method can be used
     * for both rows and headers, since CSV files do not differentiate
     * between the two apart from their location within the file.
     *
     * @throws IllegalArgumentException if the record does not contain at
     *         least one cell.
     */
    public String toCSV(String... cells) {
        return toCSV(List.of(cells));
    }

    /**
     * Serializes the specified record using this CSV format. The serialized
     * line will end with a trailing line separator.
     *
     * @throws IllegalArgumentException if the record does not contain at
     *         least one cell.
     */
    public String toCSV(CSVRecord record) {
        return toCSV(record.getCells());
    }

    public static CSVFormat withHeaders(char delimiter) {
        return new CSVFormat(true, delimiter, System.lineSeparator(), false);
    }

    public static CSVFormat withoutHeaders(char delimiter) {
        return new CSVFormat(false, delimiter, System.lineSeparator(), false);
    }
}
