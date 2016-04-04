//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Utility class for working with pop-up windows. This class can be used instead
 * of calling {@link javax.swing.JOptionPane} directly. It makes sure that dialogs
 * look native on each platform, automatically word-wraps long messages, and
 * provides a number of convenience methods. Like {@code JOptionPane}, pop-up
 * windows can specify a parent window or pass {@code null} to be considered as
 * global for the entire application.
 * <p>
 * It is recommended to not rely on the standard "OK", "Cancel", etc. button 
 * labels, since they use the JVM's locale, which might be different from the
 * locale of the application. In fact, both the OS X and Windows interface guidelines
 * recommend to not use generic button labels at all, and use something more meaningful.
 */
public final class Popups {
	
	public static final int MESSAGE_WIDTH = 350;
	public static final String DEFAULT_OK = "OK";
	public static final String DEFAULT_CANCEL = "Cancel";

	private Popups() {
	}
	
	/**
	 * Shows a dialog window that contains the specified component. The dialog 
	 * will also contain a number of buttons, the first of which is considered
	 * as the primary button. This method will block until the dialog is disposed.
	 * @param parent The window that owns the dialog, or null for a global dialog.
	 * @param title The dialog window's title.
	 * @param component The component that the dialog window will contain.
	 * @param buttons An array of labels for the dialog's buttons.
	 * @return The index of {@code buttons} that was used to dispose the dialog. 
	 * @throws IllegalArgumentException if the number of buttons is not 1 - 4.
	 */
	public static int message(JFrame parent, String title, JComponent component, List<String> buttons) {
		if ((buttons.size() < 1) || (buttons.size() > 4)) {
			throw new IllegalArgumentException("Invalid number of buttons: " + buttons.size());
		}
		
		// Wrap the component inside another panel so that it will be displayed
		// at its intended size.
		JPanel componentContainer = new JPanel(new BorderLayout());
		componentContainer.add(component, BorderLayout.NORTH);
		componentContainer.add(new JLabel(""), BorderLayout.CENTER);
								
		JOptionPane pane = new JOptionPane(componentContainer, JOptionPane.INFORMATION_MESSAGE);
		pane.setOptions(buttons.toArray(new String[0]));
		pane.setInitialValue(buttons.get(0));
		
		JDialog dialog = pane.createDialog(parent, title); 
		dialog.pack();
		dialog.setVisible(true);
		dialog.dispose();
		
		return getSelectedPopupButton(pane, buttons);
	}

	private static int getSelectedPopupButton(JOptionPane pane, List<String> buttons) {
		Object value = pane.getValue();
		for (int i = 0; i < buttons.size(); i++) {
			if (buttons.get(i).equals(value)) {
				return i;
			}
		}
		// Mimic the expected default button
		return (buttons.size() == 2) ? 1 : 0;
	}

	/**
	 * Shows a dialog window with a text message.
	 * @return The index of the button that was clicked.
	 */
	public static int message(JFrame parent, String title, String message, List<String> buttons) {
		return message(parent, title, new MultiLabel(message, MESSAGE_WIDTH), buttons);
	}
	
	/**
	 * Shows a dialog window with the specified component and the default OK
	 * button.
	 */
	public static void message(JFrame parent, String title, JComponent message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK);
		message(parent, title, message, buttons);
	}
	
	/**
	 * Shows a dialog window with a text message and a default OK button.
	 */
	public static void message(JFrame parent, String title, String message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK);
		message(parent, title, message, buttons);
	}
	
	/**
	 * Shows a dialog window with a text message and a default OK button.
	 */
	public static void message(JFrame parent, String message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK);
		message(parent, "", message, buttons);
	}
	
	/**
	 * Shows a dialog window with default OK and cancel buttons.
	 * @return True when OK, false when cancelled.
	 */
	public static boolean confirmMessage(JFrame parent, String title, JComponent message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK, DEFAULT_CANCEL);
		return message(parent, title, message, buttons) == 0;
	}
	
	/**
	 * Shows a dialog window with a text message and default OK and cancel 
	 * buttons.
	 * @return True when OK, false when cancelled.
	 */
	public static boolean confirmMessage(JFrame parent, String message) {
		return confirmMessage(parent, "", new MultiLabel(message, MESSAGE_WIDTH));
	}
		
	/**
	 * Shows a dialog window with a textfield and default OK and cancel buttons.
	 * @return The entered text, or {@code null} when cancelled.
	 */
	public static String inputMessage(JFrame parent, String title, String label, String value) {
		JTextField input = new JTextField();
		if (value != null) {
			input.setText(value);
		}
		
		JPanel inputPanel = new JPanel(new BorderLayout(0, 5));
		inputPanel.add(new JLabel(label), BorderLayout.NORTH);
		inputPanel.add(input, BorderLayout.CENTER);
		SwingUtils.setPreferredWidth(inputPanel, MESSAGE_WIDTH);
		
		if (message(parent, title, inputPanel, Arrays.asList(DEFAULT_OK, DEFAULT_CANCEL)) == 0) {
			return input.getText();
		} else {
			return null;
		}
	}
	
	/**
	 * Shows a dialog window with a textfield and default OK and cancel buttons.
	 * @return The entered text, or {@code null} when cancelled.
	 * @deprecated Use {@link #inputMessage(JFrame, String, String, String)} instead.
	 */
	@Deprecated
	public static String inputMessage(JFrame parent, String label, String value) {
		return inputMessage(parent, "", label, value);
	}
	
	/**
	 * Shows a dialog window with a text area.
	 * @return The entered text, or {@code null} when cancelled.
	 * @throws IllegalArgumentException if there is not at least 1 button.
	 */
	public static String textareaMessage(JFrame parent, String title, String label, List<String> buttons) {
		if (buttons.size() < 1) {
			throw new IllegalArgumentException("Dialog must have at least 1 button");
		}
		
		JTextArea textarea = new JTextArea();
		JScrollPane textpane = new JScrollPane(textarea);
		textpane.setPreferredSize(new Dimension(MESSAGE_WIDTH, 100));
		textpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.add(new MultiLabel(label, MESSAGE_WIDTH), BorderLayout.NORTH);
		panel.add(textpane);
		
		if (message(parent, title, panel, buttons) == 0) {
			return textarea.getText();
		} else {
			return null;
		}
	}

	/**
	 * Shows a dialog window with a combobox and default OK and cancel buttons.
	 * @return The selected value, or {@code null} when cancelled.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"}) // For Java 6 -> 7
	public static <T> T selectMessage(JFrame parent, String title, String label, List<T> options, T value) {
		JComboBox select = new JComboBox(options.toArray(new Object[0]));
		if (value != null) {
			select.setSelectedItem(value);
		}
		
		JPanel selectPanel = new JPanel(new BorderLayout(0, 5));
		selectPanel.add(new JLabel(label), BorderLayout.NORTH);
		selectPanel.add(select, BorderLayout.CENTER);
		SwingUtils.setPreferredWidth(selectPanel, MESSAGE_WIDTH);
		
		if (message(parent, title, selectPanel, Arrays.asList(DEFAULT_OK, DEFAULT_CANCEL)) == 0) {
			return options.get(select.getSelectedIndex());
		} else {
			return null;
		}
	}
	
	/**
	 * Shows a dialog window with a combobox and default OK and cancel buttons.
	 * @return The selected value, or {@code null} when cancelled.
	 */
	public static <T> T selectMessage(JFrame parent, String title, String label, List<T> options) {
		return selectMessage(parent, title, label, options, null);
	}
}
