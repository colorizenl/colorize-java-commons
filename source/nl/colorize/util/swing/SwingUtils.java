//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import nl.colorize.util.DynamicResourceBundle;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.Platform;
import nl.colorize.util.ResourceFile;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Miscelleaneous utility and convenience methods for working with Swing. 
 */
public final class SwingUtils {
    
    private static AtomicBoolean isSwingInitialized = new AtomicBoolean(false);

    private static final DynamicResourceBundle CUSTOM_COMPONENTS_BUNDLE =
        new DynamicResourceBundle(new ResourceFile("custom-swing-components.properties"), Charsets.UTF_8);

    private static final Color STANDARD_ROW_COLOR = new Color(255, 255, 255);
    private static final Color AQUA_ROW_COLOR = new Color(237, 242, 253);
    private static final Color YOSEMITE_ROW_COLOR = new Color(245, 245, 245);
    private static final Color ROW_BORDER_COLOR = new Color(220, 220, 220);
    private static final int TOOLBAR_ICON_SIZE = 30;
    
    private SwingUtils() {
    }
    
    /**
     * Returns true if the current platform is a "headless" (non-graphical) 
     * environment, and AWT/Swing/JavaFX are not supported.
     */
    public static boolean isHeadlessEnvironment() {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        
        // Some platforms claim they are not headless, but in
        // reality still do not support graphical user interfaces.
        try {
            Desktop.getDesktop();
            return false;
        } catch (HeadlessException e) {
            return true;
        }
    }
    
    /**
     * Initializes Swing by selecting the best look-and-feel for the current
     * platform. On macOS this method also changes system properties so that
     * menus appear at the top of the screen, instead of inside the window.
     * <p>
     * This method must be called before the first window is shown. 
     * @throws RuntimeException if changing the look-and-feel fails.
     */
    public static void initializeSwing() {
        if (isSwingInitialized.get()) {
            return;
        }
        
        try {
            isSwingInitialized.set(true);
            
            MacIntegration.enableApplicationMenuBar();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            if (Platform.isMac()) {
                MacIntegration.augmentLookAndFeel();
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize Swing look-and-feel", e);
        }
    }

    /**
     * Returns the resource bundle containing the user interface text for all
     * custom Swing components provided by this library. This bundle can be
     * used to change and/or translate the text.
     */
    public static DynamicResourceBundle getCustomComponentsBundle() {
        return CUSTOM_COMPONENTS_BUNDLE;
    }

    /**
     * Returns the platform's current screen size. If the platform has multiple 
     * screens this will return the size of the primary screen. If the platform 
     * has no screens, or if the screen size cannot be determined, this method 
     * will return dimensions of 0x0. 
     */
    public static Dimension getScreenSize() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        try {
            return toolkit.getScreenSize();
        } catch (HeadlessException e) {
            return new Dimension(0, 0);
        }
    }
    
    /**
     * Returns the "device pixel ratio" for the current display. A return value 
     * higher than 1.0 indicates a "retina" or "HiDPI" screen.
     * @deprecated This method relies on Apple-specific AWT properties, which
     *             remain from the old Apple JDK but will be removed from the
     *             JDK in Java 9.
     */
    @Deprecated
    public static float getScreenPixelRatio() {
        //TODO replace with API from Java 9 once that is released
        if (Platform.isMac()) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Object contentScaleFactor = toolkit.getDesktopProperty("apple.awt.contentScaleFactor");
            if (contentScaleFactor instanceof Number) {
                return ((Number) contentScaleFactor).floatValue();
            } else {
                return 1.0f;
            }
        } else {
            return 1.0f;
        }
    }
    
    /**
     * Returns true if the current display is a "retina" display (a display with
     * a pixel density higher than 1). 
     * @deprecated This method relies on Apple-specific AWT properties, which 
     *             remain from the old Apple JDK but will be removed from the
     *             JDK in Java 9. 
     */
    @Deprecated
    public static boolean isRetinaDisplay() {
        return getScreenPixelRatio() > 1.0f;
    }
    
    /**
     * Returns a textual description the platform's current screen size.
     * See {@link #getScreenSize()} and {@link #isRetinaDisplay()} for fields
     * that will be included in the description.
     */
    public static String getScreenSizeDescription() {
        Dimension screenSize = getScreenSize();
        String description = screenSize.width + "x" + screenSize.height;
        if (isRetinaDisplay()) {
            description += String.format(" (x%.1f)", getScreenPixelRatio());
        }
        return description;
    }
    
    /**
     * Loads an icon from the specified file.
     * @throws RuntimeException if no icon could be created from the file's contents.
     */
    public static ImageIcon loadIcon(ResourceFile source) {
        return new ImageIcon(Utils2D.loadImage(source));
    }
    
    /**
     * Loads an icon from the specified file, for use in 
     * {@code javax.swing.JFrame#setIconImage(Image)}.
     * @throws RuntimeException if no icon could be created from the file's contents.
     */
    public static Image loadIconImage(ResourceFile source) {
        return Utils2D.loadImage(source);
    }
    
    /**
     * Loads a TrueType font and returns it as an AWT font. 
     * @throws IOException if the font could not be loaded from the file.
     * @throws FontFormatException if the file is not a valid TrueType font.
     */
    public static Font loadFont(InputStream input, int style, float size) 
            throws IOException, FontFormatException {
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, input);
        } finally {
            Closeables.close(input, true);
        }
        return font.deriveFont(style, size);
    }

    /**
     * Loads a TrueType font and returns it as an AWT font.
     * @throws IOException if the font could not be loaded from the file.
     * @throws FontFormatException if the file is not a valid TrueType font.
     */
    public static Font loadFont(ResourceFile file, int style, float size)
        throws IOException, FontFormatException {
        try (InputStream stream = file.openStream()) {
            return loadFont(stream, style, size);
        }
    }
    
    /**
     * Creates a new {@code JMenuItem} with the specified label and (optional)
     * keyboard shortcut, and adds it to a menu.
     * @param parent The new menu item will be added to this menu.
     * @param label The menu item's text label.
     * @param keycode One of the {@code KeyEvent.VK_X} fields, or -1 for none. 
     * @return The menu item that was created and added to the menu.
     */
    public static JMenuItem createMenuItem(JMenu parent, String label, int keycode) {
        JMenuItem menuitem = new JMenuItem(label);
        if (keycode != -1) {
            menuitem.setAccelerator(getKeyStroke(keycode, false));
        }
        parent.add(menuitem);
        return menuitem;
    }
    
    /**
     * Returns a keystroke for a key that uses the platform-specific modifier key
     * for menu shortcuts.
     * @param keycode One of the {@code KeyEvent.VK_X} fields, or -1 for none.
     */
    public static KeyStroke getKeyStroke(int keycode, boolean shift) {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        if (shift) {
            mask += KeyEvent.SHIFT_DOWN_MASK;
        }
        return KeyStroke.getKeyStroke(keycode, mask);
    }
    
    /**
     * Returns a keystroke for a key that uses the platform-specific modifier key
     * for menu shortcuts.
     * @param keycode One of the {@code KeyEvent.VK_X} fields, or -1 for none.
     */
    public static KeyStroke getKeyStroke(int keycode) {
        return getKeyStroke(keycode, false);
    }
    
    /**
     * Opens the platform's default browser with the specified URL. If the platform
     * has no browser or doesn't allow access to it this method does nothing.
     * @return True if the browser was opened.
     * @throws IllegalArgumentException if {@code uri} is not a valid URI.
     */
    public static boolean openBrowser(String uri) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        
        URI parsedURI = LoadUtils.toURI(uri);
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(parsedURI);
            return true;
        } catch (IOException e) {
            return false;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
    
    /**
     * Copies the specified text to the system clipboard. If the platform does
     * not have a clipboard or doesn't allow access to it this method does nothing.
     * @return True if the text was copied to the clipboard.
     */
    public static boolean copyToClipboard(String text) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection contents = new StringSelection(text);
            clipboard.setContents(contents, contents);
            return true;
        } catch (HeadlessException e) {
            return false;
        }
    }
    
    /**
     * Performs {@code task} in a background thread, and when done calls 
     * {@code swingCallback} on the Swing thread.  
     */
    public static void doInBackground(final Runnable task, final Runnable swingCallback) {
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                task.run();
                SwingUtilities.invokeLater(swingCallback);
            }
        });
        backgroundThread.start();
    }
    
    /**
     * Changes a component's preferred width without changing its preferred height.
     */
    public static void setPreferredWidth(JComponent c, int width) {
        Dimension size = c.getPreferredSize();
        size.width = width;
        c.setPreferredSize(size);
    }
    
    /**
     * Changes a component's preferred height without changing its preferred width.
     */
    public static void setPreferredHeight(JComponent c, int height) {
        Dimension size = c.getPreferredSize();
        size.height = height;
        c.setPreferredSize(size);
    }
    
    public static void setPreferredSize(JComponent c, int width, int height) {
        if (width < 0) {
            setPreferredHeight(c, height);
        } else if (height < 0) {
            setPreferredWidth(c, width);
        } else {
            c.setPreferredSize(new Dimension(width, height));
        }
    }
    
    /**
     * Creates a {@link javax.swing.JPanel} without any visible contents and a
     * transparent background.
     * @param width Preferred width, or -1 for default.
     * @param height Preferred height, or -1 for default.
     */
    public static JPanel createSpacerPanel(int width, int height) {
        return createSpacerPanel(width, height, null);
    }
    
    /**
     * Creates a {@link javax.swing.JPanel} without any visible contents and a
     * colored background.
     * @param width Preferred width, or -1 for default.
     * @param height Preferred height, or -1 for default.
     */
    public static JPanel createSpacerPanel(int width, int height, final Color backgroundColor) {
        JPanel spacer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundColor != null) {
                    g.setColor(backgroundColor);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        spacer.setOpaque(false);
        
        if (width > 0 && height > 0) {
            spacer.setPreferredSize(new Dimension(width, height));
        } else if (height > 0) {
            setPreferredHeight(spacer, height);
        } else {
            setPreferredWidth(spacer, width);
        }
        
        return spacer;
    }
    
    /**
     * Calls {@link javax.swing.JComponent#setOpaque(boolean)} recursively.
     */
    public static void setOpaque(JComponent component, boolean opaque) {
        component.setOpaque(opaque);
        for (int i = 0; i < component.getComponentCount(); i++) {
            Component child = component.getComponent(i);
            if (child instanceof JComponent) {
                setOpaque((JComponent) child, opaque);
            }
        }
    }
    
    /**
     * Calls {@link javax.swing.JComponent#setFont(Font)} on a component and
     * then recursively on its child component.
     */
    public static void setFont(JPanel component, Font font) {
        for (int i = 0; i < component.getComponentCount(); i++) {
            Component child = component.getComponent(i);
            Font childFont = child.getFont();
            if (childFont != null && childFont.getStyle() != font.getStyle()) {
                child.setFont(font.deriveFont(childFont.getStyle()));
            } else {
                child.setFont(font);
            }
            
            if (child instanceof JPanel) {
                setFont((JPanel) child, font);
            }
        }
    }
    
    public static WindowListener toCloseDelegate(final Runnable action) {
        return new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                action.run();
            }
        };
    }
    
    public static JScrollPane wrapInScrollPane(JComponent component, boolean flexibleScrollBars) {
        JScrollPane scrollPane = new JScrollPane(component);
        if (flexibleScrollBars) {
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        } else {
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }
        scrollPane.getVerticalScrollBar().setUnitIncrement(8);
        return scrollPane;
    }
    
    public static JScrollPane wrapInScrollPane(JComponent component) {
        return wrapInScrollPane(component, false);
    }
    
    public static JScrollPane wrapInScrollPane(JComponent component, int height) {
        boolean largeScrollArea = component.getPreferredSize().height >= 4 * height;
        int scrollAmount = largeScrollArea ? 16 : 8;
        
        JScrollPane scrollPane = wrapInScrollPane(component);
        setPreferredHeight(scrollPane, height);
        if (largeScrollArea) {
            scrollPane.getVerticalScrollBar().setUnitIncrement(scrollAmount);
        }
        return scrollPane;
    }
    
    public static JScrollPane wrapInScrollPane(JComponent component, int width, int height) {
        JScrollPane scrollPane = wrapInScrollPane(component, height);
        SwingUtils.setPreferredWidth(scrollPane, width);
        return scrollPane;
    }
    
    /**
     * Returns the width available to child components within a container. This 
     * excludes insets and other space used by the platform's look-and-feel.
     */
    public static int getAvailableWidth(Container parent) {
        Insets insets = parent.getInsets();
        return parent.getWidth() - insets.left - insets.right;
    }
    
    /**
     * Returns the width available to child components within a container. This 
     * excludes insets and other space used by the platform's look-and-feel.
     */
    public static int getAvailableHeight(Container parent) {
        Insets insets = parent.getInsets();
        return parent.getHeight() - insets.top - insets.bottom;
    }
    
    public static int getSelectedButton(List<? extends AbstractButton> radioButtons) {
        for (int i = 0; i < radioButtons.size(); i++) {
            if (radioButtons.get(i).isSelected()) {
                return i;
            }
        }
        return -1;
    }
    
    public static int getSelectedButton(ButtonGroup buttonGroup) {
        return getSelectedButton(Collections.list(buttonGroup.getElements()));
    }
    
    /**
     * Removes the platform's default look-and-feel from a button.
     * @param keepPadding If true, retains the padding the look-and-feel would
     *        normally leave around the button's text and icon.
     */
    public static void removeLookAndFeel(JButton button, boolean keepPadding) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        if (!keepPadding) {
            button.setBorder(null);
        }
    }
    
    /**
     * Looks through the available font families and returns the first match from
     * {@code requestedFontFamilies} which is available on the system. If there
     * are no matches the default font family of "SansSerif" (which is guaranteed
     * to be available by Swing) is returned.
     */
    public static String findAvailableFontFamily(String... requestedFontFamilies) {
        String[] systemFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getAvailableFontFamilyNames();
        for (String requestedFontFamily : requestedFontFamilies) {
            for (String fontFamily : systemFonts) {
                if (fontFamily.equalsIgnoreCase(requestedFontFamily)) {
                    return fontFamily;
                }
            }
        }
        return "SansSerif";
    }
    
    /**
     * Attaches a {@code MouseListener} to a Swing component and forwards "click"
     * events to a {@code ActionListener}. This can be used to make non-button
     * components still behave like buttons.
     */
    public static void attachPseudoActionListener(JComponent component, final ActionListener listener) {
        component.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                listener.actionPerformed(new ActionEvent(e.getSource(), e.getID(), ""));
            }
        });
    }
    
    /**
     * Returns an {@link java.awt.event.ActionListener} that will invoke the
     * specified callback function.
     * @param arg The argument that will be passed to the callback function.
     */
    public static <T> ActionListener toActionListener(Consumer<T> callback, T arg) {
        return e -> callback.accept(arg);
    }
    
    /**
     * Returns an {@link java.awt.event.ActionListener} that will invoke the
     * specified callback function. The argument to the callback function is
     * provided by {@code arg} supplier every time an action is performed. 
     */
    public static <T> ActionListener toActionListener(Consumer<T> callback, Supplier<T> arg) {
        return e -> callback.accept(arg.get());
    }

    /**
     * Creates a component that consists of a list of items, plus buttons to add
     * and/or remove items. Changes made using those buttons are immediately
     * reflected in the list of items. After using one of the buttons the list
     * is automatically updated.
     * @param itemSupplier Used to populate the list of items, both initially
     *                     and after updates.
     * @param addButtonAction Performed when the add button is used.
     * @param removeButtonAction Performed when the remove button is used.
     * @deprecated Use {@link PropertyEditor} instead.
     */
    @Deprecated
    public static JPanel createAddRemoveItemsPanel(Supplier<List<String>> itemSupplier, String header,
            Consumer<String> addButtonAction, Consumer<String> removeButtonAction) {
        Table<String> table = new Table<>(header);
        populateTable(table, itemSupplier);
        
        JButton addButton = new JButton("+");
        addButton.addActionListener(createInvokeCallbackAndPopulateTableAction(addButtonAction,
                table, itemSupplier));
        
        JButton removeButton = new JButton("-");
        removeButton.addActionListener(createInvokeCallbackAndPopulateTableAction(removeButtonAction,
                table, itemSupplier));
        
        JPanel buttonSubPanel = new JPanel(new BorderLayout(5, 0));
        buttonSubPanel.add(addButton, BorderLayout.WEST);
        buttonSubPanel.add(removeButton, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.add(buttonSubPanel);
        
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(table, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }
    
    private static void populateTable(Table<String> table, Supplier<List<String>> itemSupplier) {
        table.removeAllRows();
        for (String item : itemSupplier.get()) {
            table.addRow(item, item);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ActionListener createInvokeCallbackAndPopulateTableAction(
            Consumer callback, Table<String> table, Supplier<List<String>> itemSupplier) {
        return e -> {
            callback.accept(table.getSelectedRowKey());
            populateTable(table, itemSupplier);
        };
    }
    
    /**
     * Creates a button suitable for usage in a toolbar. The size of the button
     * and the size of the icon will depend on the platform's user interface
     * conventions.
     */
    public static JButton createToolBarButton(String label, ImageIcon icon) {
        if (icon.getIconWidth() != TOOLBAR_ICON_SIZE || icon.getIconHeight() != TOOLBAR_ICON_SIZE) {
            icon = new ImageIcon(Utils2D.scaleImage(icon.getImage(), TOOLBAR_ICON_SIZE, 
                    TOOLBAR_ICON_SIZE, true));
        }
        
        JButton button = new JButton(label, icon);
        button.setHorizontalTextPosition(JButton.CENTER);
        button.setVerticalTextPosition(JButton.BOTTOM);
        button.setFont(button.getFont().deriveFont(11f));
        removeLookAndFeel(button, true);
        return button;
    }
    
    /**
     * Creates a button suitable for usage in a toolbar. The size of the button
     * and the size of the icon will depend on the platform's user interface
     * conventions.
     */
    public static JButton createToolBarButton(String label, BufferedImage icon) {
        return createToolBarButton(label, new ImageIcon(icon));
    }
    
    /**
     * Creates a button that does not follow the platform's UI conventions and
     * changes foreground color on hover. 
     */
    public static JButton createHoverButton(String label, Color hoverColor) {
        final JButton button = new JButton(label);
        final Color normalColor = button.getForeground();

        removeLookAndFeel(button, false);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(hoverColor);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(normalColor);
            }
        });

        return button;
    }
    
    /**
     * Creates a button that does not follow the platform's UI conventions, but
     * only consists of a text label and an outline. 
     */
    public static JButton createOutlineButton(String label, final Color normalColor, 
            final Color hoverColor) {
        final Border normalBorder = BorderFactory.createLineBorder(normalColor, 1);
        final Border hoverBorder = BorderFactory.createLineBorder(hoverColor, 1);
        
        final JButton outlineButton = new JButton(label);
        removeLookAndFeel(outlineButton, true);
        outlineButton.setBorder(normalBorder);
        outlineButton.setBorderPainted(true);
        outlineButton.setForeground(normalColor);
        outlineButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                outlineButton.setForeground(hoverColor);
                outlineButton.setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                outlineButton.setForeground(normalColor);
                outlineButton.setBorder(normalBorder);
            }
        });
        return outlineButton;
    }

    public static <T> JComboBox<String> createComboBox(Collection<T> items, T selected) {
        String[] names = items.stream()
            .map(item -> item.toString())
            .sorted()
            .collect(Collectors.toList())
            .toArray(new String[0]);

        JComboBox<String> field = new JComboBox<>(names);

        if (selected != null) {
            field.setSelectedItem(selected.toString());
        }

        return field;
    }

    /**
     * Returns a text field that always returns a valid number when its
     * {@code getText()} method is called. If the value that was actually
     * entered is not a number it will return 0 instead.
     */
    public static JTextField createNumericTextField(int initialValue) {
        return new JTextField(String.valueOf(initialValue)) {
            @Override
            public String getText() {
                String text = super.getText();
                try {
                    return String.valueOf(Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    return "0";
                }
            }
        };
    }

    /**
     * Creates a {@link JPanel} that uses the supplied callback function to draw
     * its background graphics. The callback takes two arguments: the panel's
     * graphics and the panel's dimensions.
     */
    public static JPanel createCustomGraphicsPanel(BiConsumer<Graphics2D, Dimension> callback) {
        return new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = Utils2D.createGraphics(g, true, false);
                callback.accept(g2, getSize());
            }
        };
    }

    static Color getStripedRowColor(int row) {
        if (row % 2 == 0) {
            return STANDARD_ROW_COLOR;
        } else {
            if (Platform.isMac() && !MacIntegration.isAtLeast(MacIntegration.MACOS_YOSEMITE)) {
                return AQUA_ROW_COLOR;
            } else {
                return YOSEMITE_ROW_COLOR;
            }
        }
    }
    
    static Color getStripedRowBorderColor() {
        return ROW_BORDER_COLOR;
    }
    
    private static void paintStripedRows(Graphics2D g2, JComponent component, int rowHeight) {
        int row = 0;
        for (int y = 0; y <= component.getHeight(); y += rowHeight) {
            g2.setColor(getStripedRowColor(row));
            g2.fillRect(0, y, component.getWidth(), rowHeight);
            row++;
        }
    }
    
    /**
     * Creates a {@link javax.swing.JTree} that paints rows in alternating 
     * background colors, if allowed by the platform's UI conventions.
     */
    public static <E> JList<E> createStripedList(List<E> elements) {
        return new StripedList<E>(elements); 
    }

    public static JPanel createImagePanel(BufferedImage image) {
        JPanel imagePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = Utils2D.createGraphics(g, true, true);
                g2.drawImage(image, 0, 0, null);
            }
        };
        imagePanel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        return imagePanel;
    }

    private static class StripedList<E> extends JList<E> implements ListCellRenderer<E> {
        
        private ListCellRenderer<? super E> renderer;
        
        private static final int ESTIMATED_ROW_HEIGHT = 17;
        
        public StripedList(List<E> elements) {
            super(new Vector<E>(elements));
            renderer = getCellRenderer();
            setCellRenderer(this);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Utils2D.createGraphics(g, false, false);
            paintStripedRows(g2, this, ESTIMATED_ROW_HEIGHT);
            super.paintComponent(g);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends E> list, E value, 
                int index, boolean selected, boolean focus) {
            Component cell = renderer.getListCellRendererComponent(list, value, index, selected, focus);
            if (cell instanceof JComponent && !selected) {
                ((JComponent) cell).setOpaque(false);
            }
            return cell;
        }
    }
    
    /**
     * Creates a {@link javax.swing.JTree} that paints rows in alternating 
     * background colors, if allowed by the platform's UI conventions.
     */
    public static JTree createStripedTree(DefaultTreeModel treeModel) {
        return new StripedTree(treeModel);
    }
    
    private static class StripedTree extends JTree implements TreeCellRenderer {
        
        private TreeCellRenderer renderer;
        
        public StripedTree(DefaultTreeModel treeModel) {
            super(treeModel);
            setOpaque(false);
            renderer = getCellRenderer();
            ((DefaultTreeCellRenderer) renderer).setOpenIcon(null);
            ((DefaultTreeCellRenderer) renderer).setClosedIcon(null);
            ((DefaultTreeCellRenderer) renderer).setLeafIcon(null);
            setCellRenderer(this);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Utils2D.createGraphics(g, false, false);
            paintStripedRows(g2, this, getRowHeight());
            super.paintComponent(g);
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                boolean expanded, boolean leaf, int row, boolean focus) {
            Component cell = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, 
                    leaf, row, focus);
            if (cell instanceof JComponent && !selected) {
                ((JComponent) cell).setOpaque(false);
                ((JComponent) cell).setBackground(getStripedRowColor(row));
                ((DefaultTreeCellRenderer) renderer).setBackgroundNonSelectionColor(getStripedRowColor(row));
            }
            return cell;
        }
    }
}
