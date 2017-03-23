//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.google.common.base.Supplier;

import nl.colorize.util.Relation;
import nl.colorize.util.Tuple;

/**
 * Wrapper around {@link javax.swing.JTable}. It allows for rows to be identified
 * by key instead of by index. Apart from that, it improves the appearance of the
 * table by adding column borders and a striped background (both depending on the
 * platform).
 * @param <R> The type of object used as row keys. 
 */
public class SimpleTable<R> extends JPanel implements TableModel {
	
	private List<String> columns;
	private Relation<R, List<String>> rowdata;
	private Map<R, String> tooltips;
	
	private List<TableModelListener> modelListeners;
	private List<ActionListener> doubleClickListeners;
	
	private JTable table;
	private AutoDetectRowSorter sorter;
	
	/**
	 * Creates a new table with the specified columns.
	 * @throws IllegalArgumentException when there are no columns.
	 */
	public SimpleTable(List<String> columns) {
		super(new BorderLayout());

		if (columns.isEmpty()) {
			throw new IllegalArgumentException("No columns");
		}
		
		this.columns = new ArrayList<String>(columns);
		rowdata = new Relation<R, List<String>>();
		tooltips = new HashMap<R, String>();
		
		modelListeners = new ArrayList<TableModelListener>();
		doubleClickListeners = new ArrayList<ActionListener>();
		
		initTable();
	}
	
	private void initTable() {
		table = new StripedTable(this);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateColumnsFromModel(false);
		table.createDefaultColumnsFromModel();
		table.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.getClickCount() == 2) {
					notifyDoubleClickListeners();
				}
			}
		});
		
		sorter = new AutoDetectRowSorter(this);
		table.setRowSorter(sorter);
		
		JScrollPane tablePane = new JScrollPane(table);
		tablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		add(tablePane, BorderLayout.CENTER);
	}

	public SimpleTable(String... columns) {
		this(Arrays.asList(columns));
	}

	public String getColumnName(int columnIndex) {
		return columns.get(columnIndex);
	}

	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}
	
	public int getColumnCount() {
		return columns.size();
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (rowIndex < 0 || columnIndex < 0 || columnIndex >= columns.size()) {
			throw new IllegalArgumentException("Invalid cell");
		}
		
		if (rowIndex >= rowdata.size()) {
			throw new IllegalArgumentException("Cannot use setValueAt(...) to insert rows, " +
					"use addRow(...) instead");
		}
		
		List<String> row = rowdata.get(rowIndex).getRight();
		row.set(columnIndex, value.toString());
		fireTableEvent(rowIndex, columnIndex, TableModelEvent.UPDATE);
	}
	
	private void fireTableEvent(int type, int rowIndex, int columnIndex) {
		TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, columnIndex, type);
		for (TableModelListener tml : modelListeners) {
			tml.tableChanged(event);
		}
	}

	public String getValueAt(int rowIndex, int columnIndex) {
		assertRowIndex(rowIndex);
		assertColumnIndex(columnIndex);
		return rowdata.get(rowIndex).getRight().get(columnIndex);
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	public void addRow(R key, List<String> data) {
		if (key == null) {
			throw new NullPointerException("Rows keys cannot be null");
		}
		
		if (data.size() != columns.size()) {
			throw new IllegalArgumentException("Invalid number of columns: " + data.size());
		}
		
		rowdata.add(Tuple.of(key, replaceNulls(data)));
		fireTableEvent(rowdata.size() - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
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
		rowdata.remove(rowIndex);
		fireTableEvent(TableModelEvent.DELETE, rowIndex, TableModelEvent.ALL_COLUMNS);
	}
	
	public void removeAllRows() {
		while (!rowdata.isEmpty()) {
			removeRow(rowdata.size() - 1);
		}
	}
	
	public int getRowCount() {
		return rowdata.size();
	}
	
	private R getRowKey(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= rowdata.size()) {
			return null;
		}
		return rowdata.get(rowIndex).getLeft();
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
		return rowdata.domainList();
	}
	
	private Tuple<R, List<String>> getRow(R rowKey) {
		return rowdata.findInDomain(rowKey);
	}
	
	public List<String> getRowData(R rowKey) {
		return getRow(rowKey).getRight();
	}
	
	/**
	 * Returns the index of the row that is currently selected, or -1 when no
	 * row is selected.
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
		return new Supplier<R>() {
			public R get() {
				return getSelectedRowKey();
			}
		};
	}

	private void assertColumnIndex(int columnIndex) {
		if (columnIndex < 0 || columnIndex >= columns.size()) {
			throw new IllegalArgumentException("Invalid column: " + columnIndex);
		}
	}
	
	private void assertRowIndex(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= rowdata.size()) {
			throw new IllegalArgumentException("Invalid row: " + rowIndex);
		}
	}
	
	public void setColumnSorter(int columnIndex, Comparator<String> columnSorter) {
		assertColumnIndex(columnIndex);
		sorter.setComparator(columnIndex, columnSorter);
	}
	
	public void setColumnWidth(int columnIndex, int width) {
		assertColumnIndex(columnIndex);
		table.getColumnModel().getColumn(columnIndex).setMaxWidth(width);
		table.getColumnModel().getColumn(columnIndex).setPreferredWidth(width);
	}
	
	public void setTableCellRenderer(int columnIndex, TableCellRenderer renderer) {
		table.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
	}
	
	public void addTableModelListener(TableModelListener tml) {
		modelListeners.add(tml);
	}

	public void removeTableModelListener(TableModelListener tml) {
		modelListeners.remove(tml);
	}
	
	public void setRowTooltip(R row, String tooltip) {
		tooltips.put(row, tooltip);
	}
	
	public String getRowTooltip(R row) {
		return tooltips.get(row);
	}
	
	public void addActionListener(final ActionListener al) {
		final Object source = this;
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					al.actionPerformed(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, "selectedRow"));
				}
			}
		});
	}
	
	public void addDoubleClickListener(ActionListener al) {
		doubleClickListeners.add(al);
	}
	
	public void removeDoubleClickListener(ActionListener al) {
		doubleClickListeners.remove(al);
	}
	
	private void notifyDoubleClickListeners() {
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "doubleClick");
		for (ActionListener listener : doubleClickListeners) {
			listener.actionPerformed(event);
		}
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
				((JComponent) cell).setBackground(SwingUtils.getStripedRowColor(row));
			}
			return cell;
		}
		
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public String getToolTipText(MouseEvent e) {
			int rowIndex = rowAtPoint(e.getPoint());
			if (rowIndex != -1) {
				rowIndex = convertRowIndexToModel(rowIndex);
				SimpleTable tableModel = (SimpleTable) getModel();
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
	 * Auto-detects if the data is a string or a number, and sorts the rows
	 * based on that.
	 */
	private static class AutoDetectRowSorter extends TableRowSorter<SimpleTable<?>>
			implements Comparator<Object>, Serializable {
		
		public AutoDetectRowSorter(SimpleTable<?> model) {
			super(model);
			for (int i = 0; i < model.getColumnCount(); i++) {
				setComparator(i, this);
			}
		}

		public int compare(Object a, Object b) {
			try {
				Float numberA = Float.parseFloat(a.toString());
				Float numberB = Float.parseFloat(b.toString());
				return numberA.compareTo(numberB);
			} catch (NumberFormatException e) {
				return a.toString().compareTo(b.toString());
			}
		}
	}
}
