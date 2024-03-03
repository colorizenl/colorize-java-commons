//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Subscribable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link JTable} that allows rows to be identified by key,
 * instead of only by index. It also improves the appearance of the standard
 * {@link JTable} by adding column borders and a striped background, both
 * depending on the platform look-and-feel.
 *
 * @param <R> The type of object used as row keys. 
 */
public class Table<R> extends JPanel implements TableModel {

    private JTable table;
    private List<String> columns;
    private List<Row<R>> rows;
    private Map<R, String> tooltips;
    private List<TableModelListener> modelListeners;
    private Subscribable<Table<R>> doubleClick;

    private static final Logger LOGGER = LogHelper.getLogger(Table.class);

    public Table(List<String> columns) {
        super(new BorderLayout());

        Preconditions.checkArgument(!columns.isEmpty(), "No columns");

        this.columns = List.copyOf(columns);
        this.rows = new ArrayList<>();
        this.tooltips = new HashMap<>();
        this.modelListeners = new ArrayList<>();
        this.doubleClick = new Subscribable<>();

        createTable();
        initDefaultSortOrder();
    }

    public Table(String... columns) {
        this(ImmutableList.copyOf(columns));
    }
    
    private void createTable() {
        table = new StripedTable(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateColumnsFromModel(false);
        table.createDefaultColumnsFromModel();
        table.setAutoCreateRowSorter(true);
        table.addMouseListener(SwingUtils.toMouseReleasedListener(e -> {
            if (e.getClickCount() == 2) {
                doubleClick.next(Table.this);
            }
        }));

        add(SwingUtils.wrapInScrollPane(table), BorderLayout.CENTER);
    }

    @SuppressWarnings("unchecked")
    private void initDefaultSortOrder() {
        TableRowSorter<TableModel> rowSorter = (TableRowSorter<TableModel>) table.getRowSorter();
        rowSorter.setModel(this);
        rowSorter.setStringConverter(new TableStringConverter() {
            @Override
            public String toString(TableModel model, int row, int column) {
                Object value = getValueAt(row, column);
                return Objects.toString(value);
            }
        });

        for (int i = 0; i < columns.size(); i++) {
            setColumnSorter(i, this::sortRows);
        }
    }

    /**
     * Default sort order that is used when no explicit order is defined for
     * that column. Use {@link #setColumnSorter(int, Comparator)} to define
     * an explicit sort order.
     */
    private int sortRows(Object a, Object b) {
        System.out.println(a + "  " + b);
        return 0;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Set<Class<?>> cellClasses = List.copyOf(rows).stream()
            .map(row -> row.cells.get(columnIndex))
            .map(this::getCellClass)
            .collect(Collectors.toSet());

        if (cellClasses.isEmpty()) {
            return String.class;
        } else if (cellClasses.size() == 1) {
            return cellClasses.iterator().next();
        } else {
            LOGGER.info("Column combines cells of different types: " + cellClasses);
            return Object.class;
        }
    }

    private Class<?> getCellClass(Object cell) {
        return switch (cell) {
            case null -> Object.class;
            case Number n -> Number.class;
            default -> cell.getClass();
        };
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        assertRowIndex(rowIndex);
        assertColumnIndex(columnIndex);

        List<Object> rowCells = rows.get(rowIndex).cells;
        rowCells.set(columnIndex, value.toString());
        fireTableEvent(rowIndex, columnIndex, TableModelEvent.UPDATE);
    }
    
    private void fireTableEvent(int type, int rowIndex, int columnIndex) {
        TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, columnIndex, type);
        for (TableModelListener tml : modelListeners) {
            tml.tableChanged(event);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        assertRowIndex(rowIndex);
        assertColumnIndex(columnIndex);
        return rows.get(rowIndex).cells.get(columnIndex);
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Adds a row to this table. The {@code key} will be used to identify the
     * row, {@code cells} is used to populate the table.
     *
     * @throws IllegalArgumentException if the number of cells does not match
     *         the number of columns in this table.
     */
    public void addRow(R key, List<?> cells) {
        Preconditions.checkArgument(cells.size() == columns.size(),
            "Invalid number of columns: " + cells.size() + ", expected " + columns.size());

        List<Object> formattedCells = cells.stream()
            .map(this::formatCell)
            .toList();

        Row<R> row = new Row<>(key, formattedCells);
        rows.add(row);
        fireTableEvent(rows.size() - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
    }

    /**
     * Adds a row to this table. The {@code key} will be used to identify the
     * row, {@code cells} is used to populate the table.
     *
     * @throws IllegalArgumentException if the number of cells does not match
     *         the number of columns in this table.
     */
    public void addRow(R key, String firstCell, Object... otherCells) {
        List<Object> cells = new ArrayList<>();
        cells.add(firstCell);
        for (Object otherCell : otherCells) {
            cells.add(otherCell);
        }

        addRow(key, cells);
    }
    
    private Object formatCell(Object cell) {
        return switch (cell) {
            case null -> "";
            case String textCell -> textCell;
            case Number numericalCall -> numericalCall;
            case Boolean booleanCell -> booleanCell ? "\u2713" : "";
            default -> cell.toString();
        };
    }
    
    public void removeRow(R rowKey) {
        int rowIndex = getRowIndex(rowKey);
        removeRow(rowIndex);
    }
    
    public void removeRow(int rowIndex) {
        assertRowIndex(rowIndex);
        rows.remove(rowIndex);
        fireTableEvent(TableModelEvent.DELETE, rowIndex, TableModelEvent.ALL_COLUMNS);
    }
    
    public void removeAllRows() {
        while (!rows.isEmpty()) {
            removeRow(rows.size() - 1);
        }
    }
    
    /**
     * Replaces the table's columns with the specified values. Note that this
     * will also remove all rows from the table.
     */
    public void replaceColumns(List<String> newColumns) {
        Preconditions.checkArgument(!newColumns.isEmpty(),
            "Table must contain at least 1 column");
        
        columns = ImmutableList.copyOf(newColumns);
        removeAllRows();
        table.createDefaultColumnsFromModel();
    }
    
    @Override
    public int getRowCount() {
        return rows.size();
    }
    
    private R getRowKey(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex).key;
    }
    
    private int getRowIndex(R rowKey) {
        for (int i = 0; i < getRowCount(); i++) {
            if (rowKey.equals(getRowKey(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the row that is currently selected, or -1 when
     * no row is selected.
     *
     * @deprecated Use {@link #getSelectedRowKey()} instead. Using the row
     *             index is an unreliable way of identifying towsr, as
     *             sorting the table will actually change the row order.
     */
    @Deprecated
    public int getSelectedRowIndex() {
        if (table.getSelectedRow() == -1) {
            return -1;
        }
        return table.convertRowIndexToModel(table.getSelectedRow());
    }

    /**
     * Changes the selected row to the row associated with the specified key.
     * If the key does not match any of the rows currently displayed in this
     * table, this method does nothing.
     */
    public void setSelectedRowKey(R rowKey) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).key.equals(rowKey)) {
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
    }
    
    /**
     * Returns the key of the row that is currently selected, or {@code null}
     * when no row is selected.
     */
    public R getSelectedRowKey() {
        return getRowKey(getSelectedRowIndex());
    }

    private void assertColumnIndex(int columnIndex) {
        Preconditions.checkArgument(columnIndex >= 0 && columnIndex < columns.size(),
            "Invalid column index: " + columnIndex);
    }
    
    private void assertRowIndex(int rowIndex) {
        Preconditions.checkArgument(rowIndex >= 0 && rowIndex < rows.size(),
            "Invalid row index: " + rowIndex);
    }
    
    public void setColumnSorter(int columnIndex, Comparator<String> columnSorter) {
        assertColumnIndex(columnIndex);
        TableRowSorter<?> rowSorter = (TableRowSorter<?>) table.getRowSorter();
        rowSorter.setComparator(columnIndex, columnSorter);
    }
    
    public void setColumnWidth(int columnIndex, int width) {
        assertColumnIndex(columnIndex);
        table.getColumnModel().getColumn(columnIndex).setMaxWidth(width);
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(width);
    }
    
    public void setTableCellRenderer(int columnIndex, TableCellRenderer renderer) {
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
    }
    
    @Override
    public void addTableModelListener(TableModelListener tml) {
        modelListeners.add(tml);
    }

    @Override
    public void removeTableModelListener(TableModelListener tml) {
        modelListeners.remove(tml);
    }
    
    public void setRowTooltip(R row, String tooltip) {
        tooltips.put(row, tooltip);
    }
    
    public String getRowTooltip(R row) {
        return tooltips.get(row);
    }
    
    public void addActionListener(ActionListener al) {
        Object source = this;
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                al.actionPerformed(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, "selectedRow"));
            }
        });
    }

    /**
     * Returns a {@link Subscribable} that can be used to subscribe to events
     * whenever the selected row changes.
     */
    public Subscribable<R> onSelect() {
        Subscribable<R> subject = new Subscribable<>();
        addActionListener(e -> subject.next(getSelectedRowKey()));
        return subject;
    }

    /**
     * Returns a {@link Subscribable} that can be used to subscribe to events
     * whenever the selected row is double-clicked. The event passed to
     * subscribers is the currently selected row key, i.e. the row that was
     * double-clicked.
     */
    public Subscribable<R> onDoubleClick() {
        return doubleClick
            .map(table -> getSelectedRowKey())
            .filter(row -> row != null);
    }

    /**
     * Returns a {@link Subscribable} that can be used to subscribe to events
     * whenever the selected row is double-clicked. The event passed to
     * subscribers represents the currently selected row key.
     *
     * @deprecated Use {@link #onDoubleClick()} instead, which passes the
     *             double-clicked row as the event.
     */
    @Deprecated
    public Subscribable<Table<R>> getDoubleClick() {
        return doubleClick;
    }

    /**
     * Extension of {@code javax.swing.JTable} that paints rows in alternating
     * background colors, if allowed by the platform's UI conventions.
     */
    private static class StripedTable extends JTable {
        
        public StripedTable(TableModel tableModel) {
            super(tableModel);
            setFillsViewportHeight(true);
            setShowHorizontalLines(false);
            setShowVerticalLines(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = Utils2D.createGraphics(g, false, false);
            paintEmptyRows(g2);
            paintColumnLines(g2);
        }
        
        private void paintEmptyRows(Graphics2D g2) {
            int startY = 0;
            if (getRowCount() > 0) {
                Rectangle last = getCellRect(getRowCount() - 1, 0, true);
                startY = last.y + last.height;
            }

            int row = getRowCount();
            for (int y = startY; y <= getHeight(); y += getRowHeight()) {
                g2.setColor(SwingUtils.getStripedRowColor(row));
                g2.fillRect(0, y, getWidth(), getRowHeight());
                row++;
            }
        }
        
        private void paintColumnLines(Graphics2D g2) {
            int x = 0;
            g2.setColor(SwingUtils.getStripedRowBorderColor());
            for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
                x += getColumnModel().getColumn(i).getWidth();
                g2.drawLine(x - 1, 0, x - 1, getHeight() - 1);
            }
        }
        
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component cell = super.prepareRenderer(renderer, row, column);
            if (cell instanceof JComponent && !isRowSelected(row)) {
                ((JComponent) cell).setOpaque(true);
                cell.setBackground(SwingUtils.getStripedRowColor(row));
            }
            return cell;
        }
        
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public String getToolTipText(MouseEvent e) {
            int rowIndex = rowAtPoint(e.getPoint());

            if (rowIndex != -1) {
                rowIndex = convertRowIndexToModel(rowIndex);
                Table tableModel = (Table) getModel();
                String tooltip = tableModel.getRowTooltip(tableModel.getRowKey(rowIndex));
                if (tooltip != null) {
                    return tooltip;
                }
            }

            // Show the default tooltip when none has been specified
            // for the row, or when not hovering over a row.
            return super.getToolTipText(e);
        }
    }
    
    /**
     * Represents a row, which consists of a key by which it can be identified,
     * and a number of cells. The cells are matched to the table's columns
     * based on their index.
     */
    private record Row<R>(R key, List<Object> cells) {
    }
}
