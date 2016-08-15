//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import nl.colorize.util.LogHelper;
import nl.colorize.util.swing.Action;
import nl.colorize.util.swing.ActionDelegate;
import nl.colorize.util.swing.CircularLoader;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.MultiLabel;
import nl.colorize.util.swing.Popups;
import nl.colorize.util.swing.SimpleTable;
import nl.colorize.util.swing.SwingUtils;

/**
 * Graphical test for a number of custom Swing components. These components are
 * displayed in different tabs. 
 */
public class TestCustomComponents {
	
	private JFrame frame;
	private SimpleTable<String> table;
	
	private static final Logger LOGGER = LogHelper.getLogger(TestCustomComponents.class);

	public static void main(String[] args) {
		TestCustomComponents test = new TestCustomComponents();
		test.createWindow();
	}
	
	private void createWindow() {	
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("CircularLoader", createCircularLoaderTab());
		tabs.addTab("FormPanel", createFormPanelTab());
		tabs.addTab("MultiLabel", createMultiLabelTab());
		tabs.addTab("SimpleTable", createSimpleTableTab());
		tabs.addTab("Dialogs", createDialogsTab());
		tabs.setSelectedIndex(1);
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPanel.add(tabs, BorderLayout.CENTER);
		
		frame = new JFrame("Test Custom Components");
		frame.setSize(700, 500);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout(0, 20));
		frame.add(contentPanel, BorderLayout.CENTER);
		frame.setVisible(true);
	}
	
	private JPanel createCircularLoaderTab() {
		JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		tab.add(new CircularLoader(40, 25));
		return tab;
	}

	private JPanel createFormPanelTab() {
		JPanel customHeightLabel = new JPanel();
		customHeightLabel.setBackground(new Color(255, 200, 200));
		SwingUtils.setPreferredHeight(customHeightLabel, 50);
		
		FormPanel form = new FormPanel();
		form.addRow("Row with textfield:", new JTextField());
		form.addRow("Row with other component:", new JButton("Button"));
		form.addRow("Custom height:", customHeightLabel);
		form.addRow("Radio buttons:", new JRadioButton("First"), new JRadioButton("Second"));
		form.addRow(new JCheckBox("Row with checkbox"));
		form.addEmptyRow();
		form.addRow(new JCheckBox("Another checkbox with a very long label that doesn't fit"));
		return form;
	}

	private JPanel createMultiLabelTab() {
		String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec sit " +
				"amet gravida justo. Nunc nec elit a orci facilisis fermentum. Aliquam placerat " +
				"rutrum ornare. Donec fermentum pellentesque egestas. Morbi posuere consequat " +
				"augue ac convallis. In et arcu ante, sed vestibulum nibh.\n\n" +
				"Phasellus tincidunt arcu at elit fermentum vehicula. Cras a orci est. Aenean " +
				"pulvinar nunc ut felis ultrices posuere. Integer odio purus, tempor imperdiet " +
				"semper in, congue adipiscing turpis. Nulla sed ligula urna, vitae sodales " +
				"justo. Fusce mi nisl, imperdiet in venenatis id, tempor quis turpis. Fusce " +
				"tempor facilisis tincidunt. Aenean libero dolor, varius id feugiat molestie, " +
				"blandit sit amet lectus. Cras rhoncus, purus vel posuere malesuada, sapien " +
				"libero imperdiet quam, quis cursus purus velit non mauris.";
		
		MultiLabel multiLabel = new MultiLabel(text, 300);
		multiLabel.setBackground(Color.WHITE);
		multiLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		tab.add(multiLabel);
		return tab;
	}

	private JPanel createSimpleTableTab() {
		table = new SimpleTable<String>("Name", "Year of Birth");
		table.setColumnWidth(1, 100);
		table.addRow("D", "Dennis", "1984");
		table.addRow("J", "Jasper", "1983");
		table.addRow("N", "Nikki", "1984");
		table.setRowTooltip("D", "This is a tooltip text for a row which is different from its cells"); 
		table.addActionListener(new ActionDelegate(this, "tableSelectionChanged"));
		table.addDoubleClickListener(new ActionDelegate(this, "tableDoubleClicked"));
		return table;
	}

	private JPanel createDialogsTab() {
		JButton popupButton = new JButton("Show Pop-up");
		popupButton.addActionListener(new ActionDelegate(this, "showPopup"));
		
		JButton fileDialogButton = new JButton("Show File Dialog");
		fileDialogButton.addActionListener(new ActionDelegate(this, "showFileDialogs"));
		
		JPanel tab = new JPanel(new GridLayout(3, 1, 0, 5));
		tab.add(popupButton);
		tab.add(fileDialogButton);
		return tab;
	}
	
	@Action
	public void showPopup() {
		int button = Popups.message(frame, "Window title", 
				"This is a message that spans multiple  lines and word wraps across multiple lines.",
				Arrays.asList("One", "Two"));
		LOGGER.info("Selected button: " + button);
	}
	
	@Action
	public void showFileDialogs() {
		ComboFileDialog dialog = new ComboFileDialog();
		dialog.setFilter("XML Files", "xml");
		Popups.message(frame, "Open file dialog");
		LOGGER.info("Selected file: " + dialog.showOpenDialog(frame));
		Popups.message(frame, "Save file dialog");
		LOGGER.info("Selected file: " + dialog.showSaveDialog(frame, "txt"));
	}
	
	@Action
	public void tableSelectionChanged() {
		LOGGER.info("Selected row: " + table.getSelectedRowKey());
	}
	
	@Action
	public void tableDoubleClicked() {
		LOGGER.info("Double-clicked on row: " + table.getSelectedRowKey());
	}
}
