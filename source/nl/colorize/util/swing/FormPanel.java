//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nl.colorize.util.Platform;

/**
 * Panel that arranges components in a 2 x N grid. This layout is similar to how 
 * most web pages arrange their forms, and provides a good template layout for 
 * creating property panels and such. Some aspects of the form's appearance depend
 * on the platform's user interface conventions.
 * <p>
 * Cells in the label column will get a certain percentage of the total available 
 * width, with all components in the value column sharing the remaining space.
 * Components that are not marked as being a row (i.e. that were not added using 
 * one of the {@code addRow(...)} methods, will take up the entire row.
 * <p>
 * This class uses a custom {@code LayoutManager} to achieve the desired effect,
 * changing the layout manager will mean these capabilities are lost.
 */
public class FormPanel extends JPanel {
	
	private static final String FORM_COMPONENT_PROPERTY = "nl.colorize.FormPanel.FORM_COMPONENT";
	private static final String LABEL_CELL = "nl.colorize.FormPanel.LABEL_CELL";
	private static final String VALUE_CELL = "nl.colorize.FormPanel.VALUE_CELL";
	private static final float LABEL_COLUMN_FRACTION = 0.4f;
	private static final float VALUE_COLUMN_FRACTION = 0.6f;
	
	/**
	 * Creates a {@code FormPanel} with alignments and margins conforming to the
	 * user interface conventions for the underlying platform.
	 */
	public FormPanel() {
		super(null);
		
		// Platform-dependent layout options.
		int horizontalMargin = 10;
		int verticalMargin = Platform.isOSX() ? 2 : 5;
		boolean rightAlignLabels = Platform.isOSX();
		
		FormLayout layoutManager = new FormLayout(horizontalMargin, verticalMargin, rightAlignLabels);
		setLayout(layoutManager);
		
		// The component needs to have some initial default size.
		setPreferredSize(new Dimension(400, 400));
	}
	
	/**
	 * Adds an empty row.
	 */
	public void addRow() {
		//TODO deprecate this in favor of addEmptyRow()
		addEmptyRow();
	}
	
	/**
	 * Adds a row that contains a single component that will use the entire
	 * available width.
	 */
	public void addRow(JComponent component) {
		addFullWidthRow(component);
	}
	
	/**
	 * Adds a row that contains a single component that will use the entire
	 * available width and the specified height.
	 */
	public void addRow(JComponent component, int height) {
		SwingUtils.setPreferredHeight(component, height);
		addFullWidthRow(component);
	}
	
	/**
	 * Adda a row that consists of the specified label and value cells.
	 */
	public void addRow(JComponent labelCell, JComponent valueCell) {
		addSplitWidthRow(labelCell, valueCell);
	}
	
	/**
	 * Adds a row that consists of the specified text label and value cells.
	 */
	public void addRow(String label, JComponent valueCell) {
		addSplitWidthRow(new JLabel(label), valueCell);
	}
	
	/**
	 * Adds a row that consists of a text label and a value label.
	 */
	public void addRow(String label, String valueLabel) {
		addRow(label, new JLabel(valueLabel));
	}
	
	/**
	 * Adds a row that consists of a text label in and a number of radio buttons. 
	 */
	public void addRow(String label, JRadioButton... choices) {
		for (int row = 0; row < choices.length; row++) {
			JLabel labelCell = new JLabel(row == 0 ? label : "");
			addSplitWidthRow(labelCell, choices[row]);
		}
	}
	
	/**
	 * Adds a row that consists of a text label and a slider.
	 * @param addValueLabel When true, show the (numerical) slider value in a
	 *        label displayed behind the slider. 
	 */
	public void addRow(String label, final JSlider slider, boolean addValueLabel) {
		JPanel rightColumn = new JPanel(new BorderLayout(getHorizontalMargin(), 0));
		rightColumn.setOpaque(false);
		rightColumn.add(slider, BorderLayout.CENTER);
		
		if (addValueLabel) {
			final JLabel valueLabel = new JLabel(String.valueOf(slider.getValue()));
			valueLabel.setOpaque(false);
			rightColumn.add(valueLabel, BorderLayout.EAST);
			
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					valueLabel.setText(String.valueOf(slider.getValue()));
				}
			});
		}
		
		addSplitWidthRow(new JLabel(label), rightColumn);
	}
	
	/**
	 * Adds a row that consists of a text label, a value label, and a button.
	 * Clicking the button will perform an action that results in the value
	 * label being updated.
	 * @param action Callback action invoked when the button is clicked. This
	 *        should return the new text to be used for the value label. 
	 */
	public void addRow(String label, String initialValueLabel, String actionButtonText, 
			final Callable<String> action) {
		final JLabel valueLabel = new JLabel(initialValueLabel);
		JButton actionButton = new JButton(actionButtonText);
		actionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					valueLabel.setText(action.call());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});
		
		JPanel valuePanel = new JPanel(new BorderLayout(getHorizontalMargin(), 0));
		valuePanel.add(valueLabel, BorderLayout.CENTER);
		valuePanel.add(actionButton, BorderLayout.EAST);
		addSplitWidthRow(new JLabel(label), valuePanel);
	}
	
	/**
	 * Adds an empty row that takes vertical space, but does not have any
	 * components in it.
	 */
	public void addEmptyRow() {
		JPanel spacer = new JPanel(new BorderLayout(0, 0));
		SwingUtils.setPreferredHeight(spacer, spacer.getFont().getSize());
		addFullWidthRow(spacer);
	}
	
	/**
	 * Adds an empty row that takes the specified amount of vertical space.
	 */
	public void addSpacerRow(int height) {
		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		addRow(spacer, height);
	}
	
	private void addFullWidthRow(JComponent component) {
		add(component);
	}
	
	private void addSplitWidthRow(JComponent... components) {
		for (int i = 0; i < components.length; i++) {
			if (i == 0) {
				if (components[i] instanceof JLabel) {
					applyLabelLayout((JLabel) components[i]);	
				}
				components[i].putClientProperty(FORM_COMPONENT_PROPERTY, LABEL_CELL);
			} else {
				components[i].putClientProperty(FORM_COMPONENT_PROPERTY, VALUE_CELL);
			}
			
			add(components[i]);
		}
	}
	
	private void applyLabelLayout(JLabel label) {
		FormLayout layoutManager = (FormLayout) getLayout();
		if (layoutManager.rightAlignLabels) {
			label.setHorizontalAlignment(JLabel.RIGHT);
		}
	}
	
	/**
	 * Updates the form's preferred height, indicated by {@link #getPreferredSize()},
	 * to reflect the minimum possible height that fits all rows.
	 */
	public void packFormHeight() {
		//TODO integrate this into FormLayout so that the form's preferred size
		//     is updated automatically.
		Dimension preferredFormSize = getLayout().preferredLayoutSize(this);
		SwingUtils.setPreferredHeight(this, preferredFormSize.height);
	}
	
	public void setMargin(int horizontalMargin, int verticalMargin) {
		FormLayout layoutManager = (FormLayout) getLayout();
		layoutManager.horizontalMargin = horizontalMargin;
		layoutManager.verticalMargin = verticalMargin;
	}
	
	private int getHorizontalMargin() {
		FormLayout layoutManager = (FormLayout) getLayout();
		return layoutManager.horizontalMargin;
	}
	
	public void setRightAlignLabels(boolean rightAlignLabels) {
		FormLayout layoutManager = (FormLayout) getLayout();
		layoutManager.rightAlignLabels = rightAlignLabels;
	}
	
	/**
	 * Layout manager that places components as described in the class documentation.
	 * Client properties are used to mark child components as either label cells or
	 * value cells. Components without these properties will take up all available 
	 * width. 
	 */
	private static class FormLayout implements LayoutManager {
		
		private int horizontalMargin;
		private int verticalMargin;
		private boolean rightAlignLabels;
		
		public FormLayout(int horizontalMargin, int verticalMargin, boolean rightAlignLabels) {
			this.horizontalMargin = horizontalMargin;
			this.verticalMargin = verticalMargin;
			this.rightAlignLabels = rightAlignLabels;
		}

		public void addLayoutComponent(String name, Component component) { 
		}
		
		public void removeLayoutComponent(Component component) { 
		}
		
		public Dimension preferredLayoutSize(Container parent) {
			Insets insets = parent.getInsets();
			int height = insets.top + insets.bottom;
			for (List<Component> row : calculateRows(parent)) {
				height += calculateRowHeight(row) + verticalMargin;
			}
			return new Dimension(parent.getPreferredSize().width, height);
		}
		
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}
		
		public void layoutContainer(Container parent) {
			Insets insets = parent.getInsets();
			int availableWidth = getAvailableWidth(parent);
			boolean checkBoxesInValueColumn = rightAlignLabels && 
					canFitCheckBoxesInValueColumn(parent, availableWidth);
			
			int x = insets.left;
			int y = insets.top;
			
			for (List<Component> row : calculateRows(parent)) {
				int rowHeight = calculateRowHeight(row);
				
				for (Component component : row) {
					if (checkBoxesInValueColumn && component instanceof JCheckBox) {
						x += Math.round(LABEL_COLUMN_FRACTION * availableWidth) + horizontalMargin;
					}
					int cellWidth = calculateCellWidth(component, availableWidth);
					component.setBounds(x, y, cellWidth, rowHeight);
					x += cellWidth + horizontalMargin;
				}
				
				x = insets.left;
				y += rowHeight + verticalMargin;
			}
		}
		
		/**
		 * Divides all components added to the form into rows. The returned list
		 * contains the rows, with each containing the components within that row.
		 */
		private List<List<Component>> calculateRows(Container parent) {
			int pos = 0;
			List<List<Component>> rows = new ArrayList<List<Component>>();
			
			while (pos < parent.getComponentCount()) {
				List<Component> componentsInRow = eatComponentsInRow(parent, pos);
				rows.add(componentsInRow);
				pos += componentsInRow.size();
			}
			
			return rows;
		}
		
		private List<Component> eatComponentsInRow(Container parent, int pos) {
			List<Component> inRow = new ArrayList<Component>();
			for (int i = pos; i < parent.getComponentCount(); i++) {
				Component component = parent.getComponent(i);
				if (inRow.size() >= 1 && (isLabelCell(component) || isFullWidthCell(component))) {
					return inRow;
				}
				inRow.add(component);
			}
			return inRow;
		}
		
		private int calculateCellWidth(Component component, int availableWidth) {
			if (isLabelCell(component)) {
				return Math.round(LABEL_COLUMN_FRACTION * availableWidth);
			} else if (isValueCell(component)) {
				return Math.round(VALUE_COLUMN_FRACTION * availableWidth);
			} else {
				return availableWidth;
			}
		}
		
		private int calculateRowHeight(List<Component> componentsInRow) {
			int rowHeight = 0;
			for (Component component : componentsInRow) {
				if (component.isVisible()) {
					rowHeight = Math.max(rowHeight, component.getPreferredSize().height);
				}
			}
			return rowHeight;
		}
		
		private boolean canFitCheckBoxesInValueColumn(Container parent, int availableWidth) {
			int checkboxes = 0;
			int widest = 0;
			
			for (int i = 0; i < parent.getComponentCount(); i++) {
				Component child = parent.getComponent(i);
				if (child instanceof JCheckBox && !isLabelCell(child)) {
					checkboxes++;
					widest = Math.max(widest, child.getPreferredSize().width);
				}
			}
			
			return checkboxes > 0 && widest <= Math.round(VALUE_COLUMN_FRACTION * availableWidth);
		}
		
		private boolean isLabelCell(Component component) {
			return hasClientProperty(component, FORM_COMPONENT_PROPERTY, LABEL_CELL);
		}
		
		private boolean isValueCell(Component component) {
			return hasClientProperty(component, FORM_COMPONENT_PROPERTY, VALUE_CELL);
		}
		
		private boolean isFullWidthCell(Component component) {
			return !isLabelCell(component) && !isValueCell(component);
		}
		
		private int getAvailableWidth(Container parent) {
			Insets insets = parent.getInsets();
			return parent.getWidth() - insets.left - insets.right - horizontalMargin;
		}
		
		private boolean hasClientProperty(Component component, String property, String value) {
			return component instanceof JComponent && 
					value.equals(((JComponent) component).getClientProperty(property));
		}
	}
}
