//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;

import com.google.common.io.Closeables;

import nl.colorize.util.LoadUtils;
import nl.colorize.util.Platform;
import nl.colorize.util.ResourceFile;

/**
 * Miscelleaneous utility and convenience methods for working with Swing. 
 */
public final class SwingUtils {
	
	private static AtomicBoolean isSwingInitialized = new AtomicBoolean(false);
	
	private static final Color STANDARD_ROW_COLOR = new Color(255, 255, 255);
	private static final Color AQUA_ROW_COLOR = new Color(237, 242, 253);
	private static final Color YOSEMITE_ROW_COLOR = new Color(245, 245, 245);
	private static final Color ROW_BORDER_COLOR = new Color(220, 220, 220);
	
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
		try {
			return new ImageIcon(Utils2D.loadImage(source));
		} catch (IOException e) {
			throw new RuntimeException("Could not create icon from file " + source);
		}
	}
	
	/**
	 * Loads an icon from the specified file, for use in 
	 * {@code javax.swing.JFrame#setIconImage(Image)}.
	 * @throws RuntimeException if no icon could be created from the file's contents.
	 */
	public static Image loadIconImage(ResourceFile source) {
		try {
			return Utils2D.loadImage(source);
		} catch (IOException e) {
			throw new RuntimeException("Could not create icon from file " + source);
		}
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
		c.setPreferredSize(new Dimension(width, height));
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
	 * Calls {@link javax.swing.JPanel#setOpaque(boolean)} recursively on a panel
	 * and all its sub-panels.
	 */
	public static void setOpaque(JPanel panel, boolean opaque) {
		panel.setOpaque(opaque);
		for (int i = 0; i < panel.getComponentCount(); i++) {
			Component child = panel.getComponent(i);
			if (child instanceof JPanel) {
				setOpaque((JPanel) child, opaque);
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
	
	public static WindowListener toCloseDelegate(final ActionDelegate action) {
		return new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				action.actionPerformed(null);
			}
		};
	}
	
	public static JScrollPane wrapInScrollPane(JComponent component) {
		JScrollPane scrollPane = new JScrollPane(component);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		return scrollPane;
	}
	
	public static JScrollPane wrapInScrollPane(JComponent component, int height) {
		JScrollPane scrollPane = wrapInScrollPane(component);
		setPreferredHeight(scrollPane, height);
		return scrollPane;
	}
	
	public static JScrollPane wrapInScrollPane(JComponent component, int width, int height) {
		JScrollPane scrollPane = wrapInScrollPane(component);
		setPreferredSize(scrollPane, width, height);
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
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static JComboBox createComboBox(String... items) {
		return new JComboBox(items);
	}
	
	@SuppressWarnings("rawtypes")
	public static JComboBox createComboBox(List<String> items) {
		return createComboBox(items.toArray(new String[0]));
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
	
	static boolean isStripedComponentAllowed() {
		return Platform.isMac();
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
			
			if (isStripedComponentAllowed()) {
				setOpaque(false);
				renderer = getCellRenderer();
				((DefaultTreeCellRenderer) renderer).setOpenIcon(null);
				((DefaultTreeCellRenderer) renderer).setClosedIcon(null);
				((DefaultTreeCellRenderer) renderer).setLeafIcon(null);
				setCellRenderer(this);
			}
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			if (isStripedComponentAllowed()) {
				Graphics2D g2 = Utils2D.createGraphics(g, false, false);
				paintEmptyRows(g2);
			}
			
			super.paintComponent(g);
		}

		private void paintEmptyRows(Graphics2D g2) {
			int row = 0;
			for (int y = 0; y <= getHeight(); y += getRowHeight()) {
				g2.setColor(getStripedRowColor(row));
				g2.fillRect(0, y, getWidth(), getRowHeight());
				row++;
			}
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