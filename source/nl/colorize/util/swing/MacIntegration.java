//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import nl.colorize.util.CommandRunner;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;
import nl.colorize.util.Version;

import javax.swing.JFrame;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static AppleJavaExtensionsProxy appleExtensionsProxy;
    
    private static final Version MACOS_YOSEMITE = Version.parse("10.10");
    private static final Version MACOS_BIG_SUR = Version.parse("10.16");

    /**
     * System property to enforce the use of Apple's San Fransisco font, which
     * is the default in newer macOS versions. However, Swing's font rendering
     * has some issues with this font, which is why Helvetica Neue is instead
     * used in the Swing look-and-feel. Setting this system property to true
     * will enforce the use of the San Fransisco font in Swing.
     */
    public static final String SYSTEM_PROPERTY_FONT = "colorize.useAppleSystemFont";

    // Apple-specific system properties
    private static final String SYSTEM_PROPERTY_MENUBAR = "apple.laf.useScreenMenuBar";
    
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

    // Note: the logger should go first, in case something goes wrong in one
    // of the initialisers that rely on reflection.
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
     * Returns true if the used JRE supports the Apple Java extensions. These
     * extensions were introduced in macOS Snow Leopard (an older version used
     * in older macOS versions is no longer supported by this class). These APIs
     * were a part of the macOS version of the JRE in Java 5-8, but have been
     * removed from the JRE as of Java 9. However, it can still be accessed
     * through reflection, though this will result in a warning.
     *
     * @deprecated The Apple Java extensions should no longer be accessed, even
     *             using reflection, and should be phased out. The only reason
     *             some of these classes are still used is because not all
     *             functionality can be accessed through AWT or Swing.
     */
    @Deprecated
    private static boolean supportsAppleJavaExtensions() {
        return Platform.isMac();
    }

    @Deprecated
    private static synchronized AppleJavaExtensionsProxy getAppleExtensionsProxy() {
        Preconditions.checkState(supportsAppleJavaExtensions(),
            "Apple Java extensions are not supported on this platform");

        if (appleExtensionsProxy == null) {
            appleExtensionsProxy = new AppleJavaExtensionsProxy();
        }

        return appleExtensionsProxy;
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
     * <p>
     * This method is called by {@link SwingUtils#initializeSwing()}, meaning
     * there is normally no reason for calling this method from application code.
     */
    protected static void augmentLookAndFeel() {
        if (isAtLeast(MACOS_YOSEMITE)) {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Set<String> systemFonts = ImmutableSet.copyOf(environment.getAvailableFontFamilyNames());

            if (systemFonts.contains(".AppleSystemUIFont") &&
                    System.getProperty(SYSTEM_PROPERTY_FONT, "").equals("true")) {
                changeSwingSystemFont(".AppleSystemUIFont");
            } else {
                changeSwingSystemFont("Helvetica Neue");
            }
        }

        if (isAtLeast(MACOS_BIG_SUR)) {
            UIManager.put("TabbedPane.foreground", Color.BLACK);
        }
    }
    
    private static void changeSwingSystemFont(String... fontFamilies) {
        UIDefaults swingDefaults = UIManager.getDefaults();
        Font systemFont = new Font(SwingUtils.findAvailableFontFamily(fontFamilies), Font.PLAIN, 13);
                
        List<Object> fontKeys = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : swingDefaults.entrySet()) {
            if (entry.getKey().toString().endsWith(".font")) {
                fontKeys.add(entry.getKey());
            }
        }
        
        for (Object fontKey : fontKeys) {
            Font existingFont = swingDefaults.getFont(fontKey);
            if (existingFont != null) {
                Font newFont = systemFont.deriveFont(existingFont.getStyle(), existingFont.getSize());
                swingDefaults.put(fontKey, newFont);
            }
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

            CommandRunner commandRunner = new CommandRunner("osascript", "-e", script);
            commandRunner.execute();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Sending notification failed", e);
        }
    }
    
    /**
     * Opens the default browser with the specified URL.
     * @deprecated Use {@link SwingUtils#openBrowser(String)} instead.
     */
    @Deprecated
    public static void openBrowser(String url) {
        SwingUtils.openBrowser(url);
    }
    
    /**
     * Opens the default application for the specified file. Returns true if
     * the file was successfully opened.
     */
    @Deprecated
    public static boolean openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to open file: " + file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Enables fullscreen mode for the specified window. This has the same effect
     * as the user clicking the green window button.
     */
    public static void goFullScreen(JFrame window) {
        if (supportsAppleJavaExtensions()) {
            getAppleExtensionsProxy().invokeApplication("requestToggleFullScreen", Window.class, window);
        }
    }

    /**
     * Accesses the Apple Java extensions through reflection. The reason that 
     * reflection is used is that these APIs are only available on specific
     * versions of the JRE on macOS.
     *
     * @deprecated Direct usage of the Apple extensions through reflection
     *             will be phased out. These classes are still used in this
     *             way for the reasons that are outlined in the documentation
     *             for {@link #supportsAppleJavaExtensions()}.
     */
    @Deprecated
    private static class AppleJavaExtensionsProxy {

        private Object application;

        public AppleJavaExtensionsProxy() {
            try {
                Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
                Method getApplication = applicationClass.getMethod("getApplication");
                application = getApplication.invoke(null);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error while initializing Apple Java extensions", e);
            }
        }

        public Object invokeApplication(String methodName, Class<?>[] argTypes, Object[] argValues) {
            try {
                Method appMethod = application.getClass().getMethod(methodName, argTypes);
                return appMethod.invoke(application, argValues);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception while calling Apple method", e);
                return null;
            }
        }

        public Object invokeApplication(String methodName, Class<?> argType, Object argValue) {
            return invokeApplication(methodName, new Class[] { argType }, new Object[] { argValue });
        }
    }
}
