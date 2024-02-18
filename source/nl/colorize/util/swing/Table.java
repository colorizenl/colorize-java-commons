//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import nl.colorize.util.Subscribable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link javax.swing.JTable}. It allows for rows to be identified
 * by key instead of by index. Apart from that, it improves the appearance of the
 * table by adding column borders and a striped background (both depending on the
 * platform). Finally, it makes common operations such as tracking the selected
 * row, resizing columns, and filtering rows easier.
 * @param <R> The type of object used as row keys. 
 */
public class Table<R> extends JPanel implements TableModel {

    private JTable table;
    private List<String> columns;
    private List<Row<R>> rows;
    private Map<R, String> tooltips;
    private List<TableModelListener> modelListeners;
    private Subscribable<Table<R>> doubleClick;

    /**
     * Creates a new table with the specified columns.
     * @throws IllegalArgumentException when there are no columns.
     */
    public Table(List<String> columns) {
        super(new BorderLayout());

        Preconditions.checkArgument(!columns.isEmpty(), "No columns");

        this.columns = List.copyOf(columns);
        this.rows = new ArrayList<>();
        this.tooltips = new HashMap<>();
        this.modelListeners = new ArrayList<>();
        this.doubleClick = new Subscribable<>();

        createTable();
        initRowSorter();
    }
    
    public Table(String... columns) {
        this(ImmutableList.copyOf(columns));
    }
    
    private void createTable() {
        table = new StripedTable(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateColumnsFromModel(false);
        table.createDefaultColumnsFromModel();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doubleClick.next(Table.this);
                }
            }
        });

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(tablePane, BorderLayout.CENTER);
    }

    private void initRowSorter() {
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(this);
        table.setRowSorter(rowSorter);

        for (int i = 0; i < getColumnCount(); i++) {
            rowSorter.setComparator(i, this::sortRows);
        }
    }

    /**
     * Auto-detects if the data is a string or a number, and sorts the rows
     * based on that. This will not produce the intended results for every
     * possible table, but it is still more reasonable default behavior
     * without having to define a custom comparator for every indiviudal
     * column.
     */
    private int sortRows(Object a, Object b) {
        try {
            float numberA = Float.parseFloat(a.toString());
            float numberB = Float.parseFloat(b.toString());
            return Float.compare(numberA, numberB);
        } catch (NumberFormatException e) {
            return a.toString().compareTo(b.toString());
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }
    
    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Preconditions.checkArgument(rowIndex >= 0 && columnIndex >= 0 && columnIndex < columns.size(),
            "Invalid cell");
        Preconditions.checkArgument(rowIndex < rows.size(),
            "Cannot use setValueAt(...) to insert rows, use addRow(...) instead");
        
        List<String> rowCells = rows.get(rowIndex).cells;
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
    public String getValueAt(int rowIndex, int columnIndex) {
        assertRowIndex(rowIndex);
        assertColumnIndex(columnIndex);
        return rows.get(rowIndex).cells.get(columnIndex);
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
    public void addRow(R key, List<String> data) {
        Preconditions.checkArgument(key != null, "Rows keys cannot be null");
        Preconditions.checkArgument(data.size() == columns.size(),
            "Invalid number of columns: " + data.size());
        
        rows.add(new Row<R>(key, replaceNulls(data)));
        fireTableEvent(rows.size() - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
    }
    
    public void addRow(R key, String... data) {
        addRow(key, Arrays.asList(data));
    }
    
    private List<String> replaceNulls(List<String> data) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) == null) {
                data.set(i, "");
            }
        }
        return data;
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
            if (getRowKey(i).equals(rowKey)) {
                return i;
            }
        }
        return -1;
    }
    
    public List<R> getRowKeys() {
        return rows.stream()
            .map(row -> row.key)
            .collect(Collectors.toList());
    }
    
    public List<String> getRowData(R rowKey) {
        for (Row<R> row : rows) {
            if (row.key.equals(rowKey)) {
                return row.cells;
            }
        }
        return null;
    }
    
    /**
     * Returns the index of the row that is currently selected, or -1 when no
     * row is selected.
     *
     * @deprecated Use {@link #getSelectedRowKey()} instead.
     */
    @Deprecated
    public int getSelectedRowIndex() {
        if (table.getSelectedRow() == -1) {
            return -1;
        }
        return table.convertRowIndexToModel(table.getSelectedRow());
    }
    
    /**
     * Returns the key of the row that is currently selected, or {@code null}
     * when no row is selected.
     */
    public R getSelectedRowKey() {
        return getRowKey(getSelectedRowIndex());
    }
    
    /**
     * Returns a {@link Supplier} that produces the currentlty selected row key
     * when called. Returns {@code null} when no row is selected. 
     */
    public Supplier<R> getSelectedRowKeySupplier() {
        return () -> getSelectedRowKey();
    }

    private void assertColumnIndex(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columns.size()) {
            throw new IllegalArgumentException("Invalid column: " + columnIndex);
        }
    }
    
    private void assertRowIndex(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IllegalArgumentException("Invalid row: " + rowIndex);
        }
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

    public Subscribable<R> subscribeSelected() {
        Subscribable<R> subject = new Subscribable<>();
        addActionListener(e -> subject.next(getSelectedRowKey()));
        return subject;
    }

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
    private record Row<R>(R key, List<String> cells) {
    }
}
