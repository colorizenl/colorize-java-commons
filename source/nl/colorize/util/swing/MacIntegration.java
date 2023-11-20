//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Preconditions;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;
import nl.colorize.util.Version;

import javax.swing.JFrame;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for making macOS specific behavior available to Swing applications.
 * This includes interaction with the application menu, the dock, fullscreen mode,
 * and the notification center.
 * <p>
 * Which features are available depend on both the version of macOS and the JRE.
 * In older versions of the JRE these features were accessed through the Apple
 * Java extensions. As of Java 9 these extensions are no longer part of the JRE,
 * with the functionality being added to the AWT desktop API. This class abstracts
 * over these differences, using the appropriate API depending on the used JRE.
 * <p>
 * Some features were only introduced in later version of macOS, and are not
 * available in earlier versions. Attempting to use a feature that is not available
 * in the current macOS version will have no effect, rather than throwing an 
 * exception. This is done to prevent application code from being littered with 
 * feature detection checks. The same applies to attempting to use this class on a 
 * platform other than macOS, in which case all features will silently fail.   
 */
public final class MacIntegration {

    private static final Version MACOS_YOSEMITE = Version.parse("10.10");
    private static final Version MACOS_BIG_SUR = Version.parse("10.16");

    // Apple-specific system properties
    private static final String SYSTEM_PROPERTY_MENUBAR = "apple.laf.useScreenMenuBar";
    private static final String SYSTEM_PROPERTY_METAL = "sun.java2d.metal";

    // Apple-specific Swing client properties
    public static final String AQUA_SIZE = "JComponent.sizeVariant";
    public static final String AQUA_BUTTON = "JButton.buttonType";
    public static final String AQUA_BUTTON_SEGMENT_POS = "JButton.segmentPosition";
    public static final String AQUA_TEXTFIELD = "JTextField.variant";
    public static final String AQUA_PROGRESSBAR = "JProgressBar.style";
    public static final String AQUA_VALUE_SMALL = "small";
    public static final String AQUA_VALUE_TEXTURED = "textured";
    public static final String AQUA_VALUE_SEGMENTED = "segmented";
    public static final String AQUA_VALUE_SEARCH = "search";
    public static final String AQUA_VALUE_CIRCULAR = "circular";
    public static final String AQUA_VALUE_FIRST = "first";
    public static final String AQUA_VALUE_MIDDLE = "middle";
    public static final String AQUA_VALUE_LAST = "last";

    private static final Logger LOGGER = LogHelper.getLogger(MacIntegration.class);

    private MacIntegration() {
    }
    
    private static Version getMacOSVersion() {
        String version = System.getProperty("os.version");
        if (Version.canParse(version)) {
            return Version.parse(version);
        } else {
            LOGGER.warning("Cannot parse macOS version " + version);
            return MACOS_YOSEMITE;
        }
    }
    
    private static boolean isAtLeast(Version minVersion) {
        return Platform.isMac() && getMacOSVersion().isAtLeast(minVersion);
    }

    /**
     * Enables the application menu bar for Swing applications. By default, Swing
     * will show separate menu bars for each window. This approach is common on
     * Windows and Linux, but macOS applications generally use the same menu bar
     * for the entire application.
     * <p>
     * This method is called by {@link SwingUtils#initializeSwing()}, meaning
     * there is normally no reason for calling this method from application code.
     */
    protected static void enableApplicationMenuBar() {
        if (Platform.isMac()) {
            System.setProperty(MacIntegration.SYSTEM_PROPERTY_MENUBAR, "true");
        }
    }

    /**
     * Augments the Swing look-and-feel to look more like native macOS applications.
     * This includes different look-and-feels for different versions of macOS.
     */
    protected static void augmentLookAndFeel() {
        if (isAtLeast(MACOS_BIG_SUR)) {
            UIManager.put("TabbedPane.foreground", Color.BLACK);
        }
    }

    /**
     * Registers a listener that will be notified every time a menu item in the
     * macOS application menu is clicked.
     */
    public static void setApplicationMenuListener(ApplicationMenuListener listener) {
        Desktop desktop = Desktop.getDesktop();
        desktop.setAboutHandler(e -> listener.onAbout());
        desktop.setPreferencesHandler(e -> listener.onPreferences());

        desktop.setQuitHandler((e, response) -> {
            listener.onQuit();
            // Must request a force quit since setting a quit handler
            // overrides the default behavior.
            System.exit(0);
        });
    }

    /**
     * Changes the application's dock icon to the specified image.
     */
    public static void setDockIcon(Image icon) {
        Taskbar.getTaskbar().setIconImage(icon);
    }

    /**
     * Adds a badge to the application's dock icon with the specified text. Use
     * {@code null} as argument to remove the badge.
     */
    public static void setDockBadge(String badge) {
        Taskbar.getTaskbar().setIconBadge(badge);
    }
    
    /**
     * Notifies the user of an event by bouncing the application's dock icon.
     */
    public static void bounceDockIcon() {
        Taskbar.getTaskbar().requestUserAttention(true, true);
    }

    /**
     * Sends a notification to the macOS Notification Center.
     * <p>
     * Integration with Notification Center is only allowed if the JVM was started
     * from inside an application bundle. Notifications sent from other locations
     * will not be shown.
     */
    public static void showNotification(String title, String message) {
        try {
            String script = String.format("display notification \"%s\" with title \"%s\"",
                message.replace("\"", "'"), title.replace("\"", "'"));

            Process process = new ProcessBuilder("osascript", "-e", script).start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                LOGGER.warning("osascript returned exit code " +  exitCode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Sending notification failed", e);
        }
    }
    
    /**
     * Opens the default browser with the specified URL.
     *
     * @deprecated Use {@link SwingUtils#openBrowser(String)} instead, which
     *             provides the same behavior but uses a cross-platform API
     *             that also supports other operating systems.
     */
    @Deprecated
    public static void openBrowser(String url) {
        SwingUtils.openBrowser(url);
    }
    
    /**
     * Opens the default application for the specified file. Returns true
     * if the file was successfully opened.
     *
     * @deprecated Use {@link SwingUtils#openFile(File)} instead, which
     *             provides the same behavior but uses a cross-platform
     *             API that also supports other operating systems.
     */
    @Deprecated
    public static boolean openFile(File file) {
        return SwingUtils.openFile(file);
    }

    /**
     * Reveals the specified file or directory in the Finder. If the provided
     * file is <em>not</em> a directory, the Finder will be opened for its
     * parent directory, with the requested file selected.
     */
    public static void revealInFinder(File file) {
        Preconditions.checkArgument(file.exists(), "File does not exist: " + file.getAbsolutePath());

        if (file.isDirectory()) {
            SwingUtils.openFile(file);
        } else {
            try {
                int exitCode = new ProcessBuilder("open", "-R", file.getAbsolutePath())
                    .start()
                    .waitFor();

                if (exitCode != 0) {
                    throw new IOException("Command returned exit code " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Failed to open Finder", e);
            }
        }
    }

    /**
     * Enables fullscreen mode for the specified window. This has the same
     * effect as the user clicking the green window button.
     * <p>
     * The approach to enable fullscreen mode depends on the Java runtime.
     * If the Apple Java extensions are available, this will trigger Mac OS's
     * "native" fullscreen mode. If the Apple Java extensions are <em>not</em>
     * available, this behavior will be emulated by changing the window
     * dimensions. Mac OS native fullscreen mode cannot be triggered with the
     * standard library, see https://bugs.openjdk.java.net/browse/JDK-8228638
     * for details.
     */
    public static void goFullScreen(JFrame window) {
        try {
            Class<?> appleApplicationClass = Class.forName("com.apple.eawt.Application");
            Object instance = appleApplicationClass.getDeclaredMethod("getApplication").invoke(null);
            appleApplicationClass.getMethod("requestToggleFullScreen", Window.class)
                .invoke(instance, window);
        } catch (Exception e) {
            window.setUndecorated(true);
            window.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    /**
     * Returns true if a Swing or Java2D application is using the Metal renderer
     * introduced in Java 17, and false if it is using the OpenGL renderer.
     */
    public static boolean usesMetalRenderer() {
        return System.getProperty(SYSTEM_PROPERTY_METAL, "").equalsIgnoreCase("true");
    }
}
