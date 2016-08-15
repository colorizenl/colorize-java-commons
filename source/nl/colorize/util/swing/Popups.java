//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.google.common.collect.ImmutableList;

/**
 * Utility class for working with pop-up windows. This class can be used instead
 * of calling {@link javax.swing.JOptionPane} directly. It makes sure that dialogs
 * look native on each platform, automatically word-wraps long messages, and
 * provides a number of convenience methods. Like {@code JOptionPane}, pop-up
 * windows can specify a parent window or pass {@code null} to be considered as
 * global for the entire application.
 * <p>
 * It's recommended to not rely on generic button labels such as "OK" or "cancel",
 * since they can be confusing depending on the action performed by the pop-up
 * window. Instead, try to use button labels that convey the action that is going
 * to be performed, such as "save" or "download file".
 */
public final class Popups {
	
	public static final int MESSAGE_WIDTH = 350;
	private static final String DEFAULT_OK = "OK";
	private static final String DEFAULT_CANCEL = "Cancel";

	private Popups() {
	}
	
	/**
	 * Shows a pop-up window that consists of the specified component and a number
	 * of buttons. The first button in the list is considered the "primary" button. 
	 * This method will block until either one of the buttons is clicked or the 
	 * dialog was cancelled. 
	 * @return The index of the button that was clicked. For example, if the first
	 *         button from {@code buttons} was clicked this will return 0. 
	 * @throws IllegalArgumentException if no buttons were supplied.
	 */
	public static int message(JFrame parent, String title, JComponent panel, List<String> buttons) {
		if (buttons.isEmpty()) {
			throw new IllegalArgumentException("Invalid number of buttons: " + buttons.size());
		}
		
		// Wrap the panel inside another panel so that it will be displayed
		// at its intended size.
		JPanel componentContainer = new JPanel(new BorderLayout());
		componentContainer.add(panel, BorderLayout.NORTH);
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
		
		// On most platforms the pop-up window can be cancelled without
		// clicking a button (for example by clicking the window's "X"
		// button or by using a keyboard shortcut. This usually has the
		// effect of "cancelling" the action.
		// This assumes that on a pop-window with 2 buttons the
		// non-primary button cancels the dialog. This behavior seems
		// reasonable for most cases, but should be documented 
		// explicitly when creating the pop-up window. 
		return buttons.size() == 2 ? 1 : 0;
	}
	
	/**
	 * Shows a pop-up window that consists of the specified component and a number
	 * of buttons. The first button in the list is considered the "primary" button. 
	 * This method will block until either one of the buttons is clicked or the 
	 * dialog was cancelled. 
	 * @return The index of the button that was clicked. For example, if the first
	 *         button from {@code buttons} was clicked this will return 0. 
	 * @throws IllegalArgumentException if no buttons were supplied.
	 */
	public static int message(JFrame parent, String title, JComponent panel, String... buttons) {
		return message(parent, title, panel, ImmutableList.copyOf(buttons));
	}

	/**
	 * Shows a pop-up window that consists of the specified component and a number
	 * of buttons. The first button in the list is considered the "primary" button. 
	 * This method will block until either one of the buttons is clicked or the 
	 * dialog was cancelled. 
	 * @return The index of the button that was clicked. For example, if the first
	 *         button from {@code buttons} was clicked this will return 0. 
	 * @throws IllegalArgumentException if no buttons were supplied.
	 */
	public static int message(JFrame parent, String title, String message, List<String> buttons) {
		return message(parent, title, new MultiLabel(message, MESSAGE_WIDTH), buttons);
	}
	
	/**
	 * Shows a pop-up window that consists of the specified component and a number
	 * of buttons. The first button in the list is considered the "primary" button. 
	 * This method will block until either one of the buttons is clicked or the 
	 * dialog was cancelled. 
	 * @return The index of the button that was clicked. For example, if the first
	 *         button from {@code buttons} was clicked this will return 0. 
	 * @throws IllegalArgumentException if no buttons were supplied.
	 */
	public static int message(JFrame parent, String title, String message, String... buttons) {
		return message(parent, title, message, ImmutableList.copyOf(buttons));
	}
	
	/**
	 * Shows a simple pop-up window that displays a component and contains a
	 * default "OK" button.
	 * @deprecated Pop-up windows should not rely on generic button labels.
	 *             Use {@link #message(JFrame, String, JComponent, List)} instead.
	 */
	@Deprecated
	public static void message(JFrame parent, String title, JComponent message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK);
		message(parent, title, message, buttons);
	}
	
	/**
	 * Shows a simple pop-up window that displays a text message and contains a
	 * default "OK" button.
	 */
	public static void message(JFrame parent, String title, String message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK);
		message(parent, title, message, buttons);
	}
	
	/**
	 * Shows a simple pop-up window that displays a text message and contains a
	 * default "OK" button.
	 */
	public static void message(JFrame parent, String message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK);
		message(parent, "", message, buttons);
	}
	
	/**
	 * Shows a simple pop-up window that displays a text message and contains
	 * default "OK" and "cancel" buttons.
	 * @return True when OK, false when cancelled.
	 */
	public static boolean confirmMessage(JFrame parent, String title, JComponent message) {
		List<String> buttons = Arrays.asList(DEFAULT_OK, DEFAULT_CANCEL);
		return message(parent, title, message, buttons) == 0;
	}
	
	/**
	 * Shows a simple pop-up window that displays a text message and contains
	 * default "OK" and "cancel" buttons.
	 * @return True when OK, false when cancelled.
	 */
	public static boolean confirmMessage(JFrame parent, String message) {
		return confirmMessage(parent, "", new MultiLabel(message, MESSAGE_WIDTH));
	}
}
