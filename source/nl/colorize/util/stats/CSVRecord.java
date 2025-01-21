//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;
import lombok.Getter;

import java.util.List;

/**
 * One of the records witin a CSV file. Records are usually parsed from a
 * CSV file using {@link CSVFormat}, but can also be created programmatically.
 * Fields within the record can be retrieved by index or by column name.
 * The latter is only available if the CSV contains column information.
 * <p>
 * <strong>Note:</strong> This class is intended for basic CSV support.
 * When working with CSV files produced by other applications, prefer using
 * the <a href="https://github.com/osiegmar/FastCSV">FastCSV</a> library.
 */
@Getter
public class CSVRecord {

    private List<String> columns;
    private List<String> cells;
    private CSVFormat format;

    /**
     * Creates a new CSV record from the specified cells. The column
     * information is optional: {@code null} indicates that no column
     * information is available, meaning that cells can only be retrieved
     * by index.
     * <p>
     * {@link CSVRecord} instances are normally by obtained by parsing CSV
     * files using {@link CSVFormat}. There is normally no need for
     * applications to use this constructor directly.
     */
    protected CSVRecord(List<String> columns, List<String> cells, CSVFormat format) {
        Preconditions.checkArgument(!cells.isEmpty(), "Empty CSV record");
        Preconditions.checkArgument(columns == null || columns.size() == cells.size(),
            "Invalid number of columns");

        this.columns = columns == null ? null : List.copyOf(columns);
        this.cells = List.copyOf(cells);
        this.format = format;
    }

    /**
     * Returns the value of the cell with the specified index. This method is
     * always available, even if the CSV file does not include any column
     * information.
     *
     * @throws IllegalArgumentException if the column index is invalid.
     */
    public String get(int index) {
        Preconditions.checkArgument(index >= 0 && index < cells.size(),
            "Invalid column index: " + index);
        return cells.get(index);
    }

    /**
     * Returns true if this CSV record included column information. If false,
     * cells can only be accessed by index, not by column name.
     */
    public boolean hasColumnInformation() {
        return columns != null;
    }

    /**
     * Returns the value for the cell with the specified column name.
     *
     * @throws IllegalStateException if no column name information is available.
     * @throws IllegalArgumentException if no column with that name exists.
     */
    public String get(String column) {
        Preconditions.checkState(columns != null, "No column name information available");
        Preconditions.checkArgument(columns.contains(column), "No such column: " + column);

        int columnIndex = columns.indexOf(column);
        return cells.get(columnIndex);
    }

    /**
     * Returns a list of all named columns in this CSV record. The columns will
     * be sorted in the same order as they appear in the CSV.
     *
     * @throws IllegalStateException if no column name information is available.
     */
    public List<String> getColumns() {
        Preconditions.checkState(columns != null, "No column name information available");
        return columns;
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
        return TupleList.combine(columns, cells);
    }

    @Override
    public String toString() {
        return format.toCSV(this);
    }
}
