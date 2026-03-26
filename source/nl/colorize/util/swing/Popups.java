//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Preconditions;
import nl.colorize.util.TranslationBundle;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for working with pop-up modal dialog windows in Swing
 * applications.
 * <p>
 * There are two ways to create pop-up windows: By using {@link #builder()}
 * and its methods to configure and show the pop-up window, or by using one
 * of the "pre-baked" static methods that create a pop-up dialog in one go.
 * <p>
 * Both approaches wrap the underlying {@link JOptionPane}. It makes sure
 * that pop-up windows look native on each platform, automatically word-wraps
 * long messages, and provides a number of convenience methods.
 * <p>
 * It's recommended to not rely on generic button labels such as "OK" or
 * "cancel", since they can be confusing depending on the action performed by
 * the pop-up window. Instead, try to use button labels that convey the action
 * that is going to be performed, such as "save" or "download file".
 */
public final class Popups {

    private String title;
    private JComponent panel;
    private List<String> buttonLabels;
    private int iconType;
    
    public static final int MESSAGE_WIDTH = 350;

    private static final TranslationBundle BUNDLE = SwingUtils.getCustomComponentsBundle();
    private static final String DEFAULT_OK = BUNDLE.getString("Popups.ok");
    private static final String DEFAULT_CANCEL = BUNDLE.getString("Popups.cancel");

    private Popups() {
        this.title = "";
        this.panel = new MultiLabel("", MESSAGE_WIDTH);
        this.buttonLabels = List.of(DEFAULT_OK);
        this.iconType = JOptionPane.INFORMATION_MESSAGE;
    }

    public Popups withTitle(String title) {
        this.title = title;
        return this;
    }

    public Popups withButtons(List<String> buttonLabels) {
        Preconditions.checkArgument(!buttonLabels.isEmpty(), "Pop-up window must have buttons");
        this.buttonLabels = List.copyOf(buttonLabels);
        return this;
    }

    public Popups withButtons(String... buttonLabels) {
        return withButtons(List.of(buttonLabels));
    }

    public Popups withConfirmButtons() {
        return withButtons(List.of(DEFAULT_OK, DEFAULT_CANCEL));
    }

    public Popups withPanel(JComponent panel) {
        this.panel = panel;
        return this;
    }

    public Popups withMessage(String message) {
        return withPanel(new MultiLabel(message, MESSAGE_WIDTH));
    }

    public Popups withWarningIcon() {
        this.iconType = JOptionPane.WARNING_MESSAGE;
        return this;
    }

    public Popups withErrorIcon() {
        this.iconType = JOptionPane.ERROR_MESSAGE;
        return this;
    }

    /**
     * Displays a pop-up window based on the configuration in this builder.
     * The pop-up window will be modal for the specified parent window.
     *
     * @return The index of the button that was clicked. Returns -1 if the
     *         pop-up window was disposed of without explicitly clicking one
     *         of its buttons.
     */
    public int show(JFrame parentWindow) {
        JPanel container = new JPanel(new BorderLayout());
        container.add(panel, BorderLayout.NORTH);
        container.add(new JLabel(""), BorderLayout.CENTER);

        JOptionPane popup = new JOptionPane(container, iconType);
        popup.setOptions(buttonLabels.toArray(new String[0]));
        popup.setInitialValue(buttonLabels.getFirst());

        JDialog dialog = popup.createDialog(parentWindow, title);
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();

        return getSelectedButtonIndex(popup);
    }

    /**
     * Displays a pop-up window based on the configuration in this builder.
     * The pop-up window will be modal for the entire application.
     *
     * @return The index of the button that was clicked. Returns -1 if the
     *         pop-up window was disposed of without explicitly clicking one
     *         of its buttons.
     */
    public int show() {
        return show(null);
    }

    private int getSelectedButtonIndex(JOptionPane popup) {
        for (int i = 0; i < buttonLabels.size(); i++) {
            if (buttonLabels.get(i).equals(popup.getValue())) {
                return i;
            }
        }

        // On most platforms the pop-up window can be canceled without
        // clicking a button, usually by clicking the window's close
        // button in the title bar.
        return -1;
    }

    /**
     * Starts building a new pop-up window. The initial state will have no title,
     * an empty message, and a single default "OK" button.
     */
    public static Popups builder() {
        return new Popups();
    }

    /**
     * Shows a pop-up window that consists of the specified component and a number
     * of buttons. The first button in the list is considered the "primary" button. 
     * This method will block until either one of the buttons is clicked or the 
     * dialog was cancelled.
     *
     * @return The index of the button that was clicked. For example, if the first
     *         button from {@code buttons} was clicked this will return 0. 
     *
     * @throws IllegalArgumentException if no buttons were supplied.
     */
    public static int message(JFrame parent, String title, JComponent panel, List<String> buttons) {
        return builder()
            .withTitle(title)
            .withPanel(panel)
            .withButtons(buttons)
            .show(parent);
    }

    /**
     * Shows a pop-up window that consists of the specified component and a number
     * of buttons. The first button in the list is considered the "primary" button. 
     * This method will block until either one of the buttons is clicked or the 
     * dialog was cancelled.
     *
     * @return The index of the button that was clicked. For example, if the first
     *         button from {@code buttons} was clicked this will return 0. 
     *
     * @throws IllegalArgumentException if no buttons were supplied.
     */
    public static int message(JFrame parent, String title, JComponent panel, String... buttons) {
        return message(parent, title, panel, Arrays.asList(buttons));
    }

    /**
     * Shows a pop-up window that consists of the specified component and a number
     * of buttons. The first button in the list is considered the "primary" button. 
     * This method will block until either one of the buttons is clicked or the 
     * dialog was cancelled.
     *
     * @return The index of the button that was clicked. For example, if the first
     *         button from {@code buttons} was clicked this will return 0. 
     *
     * @throws IllegalArgumentException if no buttons were supplied.
     */
    public static int message(JFrame parent, String title, String message, List<String> buttons) {
        MultiLabel panel = new MultiLabel(message, MESSAGE_WIDTH);
        return message(parent, title, panel, buttons);
    }
    
    /**
     * Shows a pop-up window that consists of the specified component and a number
     * of buttons. The first button in the list is considered the "primary" button. 
     * This method will block until either one of the buttons is clicked or the 
     * dialog was cancelled.
     *
     * @return The index of the button that was clicked. For example, if the first
     *         button from {@code buttons} was clicked this will return 0. 
     *
     * @throws IllegalArgumentException if no buttons were supplied.
     */
    public static int message(JFrame parent, String title, String message, String... buttons) {
        return message(parent, title, message, Arrays.asList(buttons));
    }
    
    /**
     * Shows a simple pop-up window that displays a component and contains a
     * default "OK" button.
     */
    public static void message(JFrame parent, String title, JComponent message) {
        List<String> buttons = List.of(DEFAULT_OK);
        message(parent, title, message, buttons);
    }
    
    /**
     * Shows a simple pop-up window that displays a text message and contains a
     * default "OK" button.
     */
    public static void message(JFrame parent, String title, String message) {
        List<String> buttons = List.of(DEFAULT_OK);
        message(parent, title, message, buttons);
    }
    
    /**
     * Shows a simple pop-up window that displays a text message and contains a
     * default "OK" button.
     */
    public static void message(JFrame parent, String message) {
        List<String> buttons = List.of(DEFAULT_OK);
        message(parent, "", message, buttons);
    }

    /**
     * Shows a simple pop-up window that displays an error message with a
     * generic "Error" title and a single "OK" button.
     */
    public static void errorMessage(JFrame parent, String message) {
        Popups.builder()
            .withTitle(BUNDLE.getString("Popups.error"))
            .withMessage(message)
            .withErrorIcon()
            .show(parent);
    }
}
