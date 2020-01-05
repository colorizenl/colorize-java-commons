//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import nl.colorize.util.CommandRunner;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;
import nl.colorize.util.Version;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    
    protected static final Version MACOS_LION = Version.parse("10.7");
    protected static final Version MACOS_MOUNTAIN_LION = Version.parse("10.8");
    protected static final Version MACOS_YOSEMITE = Version.parse("10.10");
    protected static final Version MACOS_EL_CAPITAN = Version.parse("10.11");

    // Apple-specific system properties
    protected static final String SYSTEM_PROPERTY_MENUBAR = "apple.laf.useScreenMenuBar";
    
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

    private static final AppleJavaExtensionsProxy APPLE_PROXY = new AppleJavaExtensionsProxy();
    private static final DesktopProxy DESKTOP_PROXY = new DesktopProxy();
    private static final TaskBarProxy TASKBAR_PROXY = new TaskBarProxy();

    private MacIntegration() {
    }
    
    private static Version getMacOSVersion() {
        String version = System.getProperty("os.version");
        if (Version.canParse(version)) {
            return Version.parse(version);
        } else {
            LOGGER.warning("Cannot parse macOS version " + version);
            return MACOS_LION;
        }
    }
    
    protected static boolean isAtLeast(Version minVersion) {
        if (!Platform.isMac()) {
            return false;
        }
        return getMacOSVersion().isAtLeast(minVersion);
    }
    
    /**
     * Returns true if the used JRE supports the Apple Java extensions. These
     * extensions were introduced in macOS Snow Leopard (an older version used
     * in older macOS versions is no longer supported by this class). These APIs
     * were a part of the macOS version of the JRE in Java 5-8, but have been
     * removed from the JRE as of Java 9. 
     */
    private static boolean supportsAppleJavaExtensions() {
        if (!Platform.isMac()) {
            return false;
        }
        
        Version jre = Platform.getJavaVersion();
        return !jre.isAtLeast(Version.parse("1.9")) && jre.isAtLeast(Version.parse("1.6"));
    }

    /**
     * Returns true if the used JRE supports the enhanced desktop API that was
     * introduced in Java 9.
     */
    private static boolean supportsEnhancedDesktop() {
        Version jre = Platform.getJavaVersion();
        return Platform.isMac() && jre.isAtLeast(Version.parse("11"));
    }

    private static boolean supportsNotificationCenter() {
        return Platform.isMac() && isAtLeast(MACOS_MOUNTAIN_LION);
    }
    
    /**
     * Enables the application menu bar for Swing applications. By default, Swing
     * will show separate menu bars for each window. This approach is common on
     * Windows and Linux, but macOS applications generally use the same menu bar
     * for the entire application.
     */
    public static void enableApplicationMenuBar() {
        if (Platform.isMac()) {
            System.setProperty(MacIntegration.SYSTEM_PROPERTY_MENUBAR, "true");
        }
    }

    /**
     * Changes the default Swing look-and-feel to look more like native macOS
     * applications. This includes different look-and-feels for different versions
     * of macOS.
     */
    public static void augmentLookAndFeel() {
        if (isAtLeast(MACOS_EL_CAPITAN)) {
            //TODO macOS now uses the San Fransisco font, which is not
            //     available to the user.
            changeSwingSystemFont("Helvetica Neue");
        } else if (isAtLeast(MACOS_YOSEMITE)) {
            changeSwingSystemFont("Helvetica Neue");
        }
    }
    
    private static void changeSwingSystemFont(String... fontFamilies) {
        UIDefaults swingDefaults = UIManager.getDefaults();
        Font systemFont = new Font(SwingUtils.findAvailableFontFamily(fontFamilies), Font.PLAIN, 13);
                
        List<Object> fontKeys = new ArrayList<Object>();
        for (Map.Entry<Object, Object> entry : swingDefaults.entrySet()) {
            if (entry.getKey().toString().endsWith(".font")) {
                fontKeys.add(entry.getKey());
            }
        }
        
        for (Object fontKey : fontKeys) {
            Font existingFont = swingDefaults.getFont(fontKey);
            if (existingFont != null) {
                swingDefaults.put(fontKey, 
                        systemFont.deriveFont(existingFont.getStyle(), existingFont.getSize()));
            }
        }
    }

    /**
     * Registers a listener that will be notified every time a menu item in the
     * macOS application menu is clicked.
     * @param showPreferences Indicates whether the "preferences" menu item should
     *        be shown as part of the application menu.
     */
    public static void setApplicationMenuListener(ApplicationMenuListener listener,
                                                  boolean showPreferences) {
        if (supportsEnhancedDesktop()) {
            DESKTOP_PROXY.applicationMenu = listener;
        } else {
            APPLE_PROXY.setApplicationMenuListener(listener, showPreferences);
        }
    }

    /**
     * Changes the application's dock icon to the specified image.
     * @deprecated Applications should not change their dock icon at runtime.
     *             Instead, rely on the application icon specified in the
     *             application bundle. 
     */
    @Deprecated
    public static void setDockIcon(Image icon) {
        if (supportsAppleJavaExtensions()) {
            APPLE_PROXY.invokeApplication("setDockIconImage", Image.class, icon);
        } else if (supportsEnhancedDesktop()) {
            TASKBAR_PROXY.call("setIconImage", new Class[] {Image.class}, new Object[] {icon});
        }
    }

    /**
     * Adds a badge to the application's dock icon with the specified text. Use
     * {@code null} as argument to remove the badge.
     */
    public static void setDockBadge(String badge) {
        if (supportsAppleJavaExtensions()) {
            APPLE_PROXY.invokeApplication("setDockIconBadge", String.class, badge);
        } else if (supportsEnhancedDesktop()) {
            TASKBAR_PROXY.call("setIconBadge", new Class[] {String.class}, new Object[] {badge});
        }
    }
    
    /**
     * Notifies the user of an event by bouncing the application's dock icon.
     * @param important True if this is an important event.
     */
    public static void bounceDockIcon(boolean important) {
        if (supportsAppleJavaExtensions()) {
            APPLE_PROXY.invokeApplication("requestUserAttention", boolean.class, important);
        } else if (supportsEnhancedDesktop()) {
            TASKBAR_PROXY.call("requestUserAttention", new Class[] {boolean.class, boolean.class},
                new Object[] {true, true});
        }
    }

    /**
     * Sends a notification to the macOS Notification Center.
     * <p>
     * Integration with Notification Center is only allowed if the JVM was started
     * from inside an application bundle. Notifications sent from other locations
     * will not be shown.
     */
    public static void showNotification(String title, String message) {
        if (supportsNotificationCenter()) {
            title = title.replace("\"", "'");
            message = message.replace("\"", "'");
            
            try {
                CommandRunner commandRunner = new CommandRunner("osascript", "-e", 
                        String.format("display notification \"%s\" with title \"%s\"", message, title));
                commandRunner.execute();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Sending notification failed", e);
            }
        }
    }
    
    /**
     * Opens the default browser with the specified URL.
     * @deprecated Use {@link SwingUtils#openBrowser(String)} instead.
     */
    @Deprecated
    public static void openBrowser(String url) {
        DESKTOP_PROXY.call("browse", URI.class, URI.create(url));
    }
    
    /**
     * Opens the default application for the specified file.
     * @deprecated Use {@link Desktop#open(File)} instead.
     */
    @Deprecated
    public static void openFile(File file) {
        DESKTOP_PROXY.call("open", File.class, file);
    }
    
    /**
     * Accesses the Apple Java extensions through reflection. The reason that 
     * reflection is used is that these APIs are only available on specific
     * versions of the JRE on macOS.
     */
    private static class AppleJavaExtensionsProxy implements InvocationHandler {

        private Object application;
        private boolean applicationMenuInitialized;
        private ApplicationMenuListener applicationMenuListener;

        public AppleJavaExtensionsProxy() {
            if (supportsAppleJavaExtensions()) {
                try {
                    Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
                    Method getApplication = applicationClass.getMethod("getApplication");
                    application = getApplication.invoke(null, new Object[0]);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error while initializing Apple Java extensions", e);
                }

                applicationMenuInitialized = false;
            }
        }

        private void setApplicationMenuListener(ApplicationMenuListener listener, boolean showPreferences) {
            this.applicationMenuListener = listener;

            if (application != null && !applicationMenuInitialized) {
                applicationMenuInitialized = true;
                registerApplicationMenuHandlers(showPreferences);
            }
        }

        private void registerApplicationMenuHandlers(boolean showPreferences) {
            try {
                Class<?> quitHandlerClass = Class.forName("com.apple.eawt.QuitHandler");
                invokeApplication("setQuitHandler", quitHandlerClass, proxy(quitHandlerClass));

                Class<?> aboutHandlerClass = Class.forName("com.apple.eawt.AboutHandler");
                invokeApplication("setAboutHandler", aboutHandlerClass, proxy(aboutHandlerClass));

                Class<?> prefsHandlerClass = Class.forName("com.apple.eawt.PreferencesHandler");
                invokeApplication("setPreferencesHandler", prefsHandlerClass,
                    showPreferences ? proxy(prefsHandlerClass) : null);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error while registering application menu listener", e);
            }
        }

        private Object proxy(Class<?> target) {
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {target}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            if (name.equals("handleQuit") || name.equals("handleQuitRequestWith")) {
                applicationMenuListener.onQuit();
                Object quitResponse = args[1];
                quitResponse.getClass().getMethod("performQuit").invoke(quitResponse);
            } else if (name.equals("handleAbout")) {
                applicationMenuListener.onAbout();
            } else if (name.equals("handlePreferences")) {
                applicationMenuListener.onPreferences();
            }

            return null;
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
    
    /**
     * Calls methods from the AWT desktop API. Like the Apple Java extensions,
     * this API is called using reflection as it is only available on certain
     * versions of the JRE.
     */
    private static class DesktopProxy implements InvocationHandler {
        
        private Desktop desktop;
        private ApplicationMenuListener applicationMenu;
        
        public DesktopProxy() {
            try {
                desktop = Desktop.getDesktop();

                if (supportsEnhancedDesktop()) {
                    Class<?> aboutHandler = Class.forName("java.awt.desktop.AboutHandler");
                    call("setAboutHandler", aboutHandler, proxy(aboutHandler));

                    Class<?> prefsHandler = Class.forName("java.awt.desktop.PreferencesHandler");
                    call("setPreferencesHandler", prefsHandler, proxy(prefsHandler));

                    Class<?> quitHandler = Class.forName("java.awt.desktop.QuitHandler");
                    call("setQuitHandler", quitHandler, proxy(quitHandler));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Cannot access AWT desktop API", e);
            }
        }

        private Object proxy(Class<?> target) {
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {target}, this);
        }
        
        public void call(String methodName, Class<?>[] argTypes, Object[] args) {
            try {
                Method method = desktop.getClass().getMethod(methodName, argTypes);
                method.invoke(desktop, args);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception while calling AWT desktop API", e);
            }
        }
        
        public void call(String methodName, Class<?> argType, Object arg) {
            call(methodName, new Class<?>[] { argType }, new Object[] { arg });
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (applicationMenu != null) {
                if (method.getName().equals("handleAbout")) {
                    applicationMenu.onAbout();
                } else if (method.getName().equals("handlePreferences")) {
                    applicationMenu.onPreferences();
                } else if (method.getName().equals("handleQuitRequestWith")) {
                    applicationMenu.onQuit();
                    // Must request a force quit since setting a quit handler
                    // overrides the default behavior.
                    System.exit(0);
                }
            }

            return null;
        }
    }

    /**
     * Calls methods from the task bar API that was introduced in Java 9
     * using reflection.
     */
    private static class TaskBarProxy {

        private Object taskbar;

        public TaskBarProxy() {
            if (supportsEnhancedDesktop()) {
                try {
                    taskbar = Class.forName("java.awt.Taskbar")
                        .getDeclaredMethod("getTaskbar")
                        .invoke(null);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Cannot initialize task bar", e);
                }
            }
        }

        public void call(String methodName, Class<?>[] argTypes, Object[] args) {
            if (supportsEnhancedDesktop()) {
                try {
                    Method method = taskbar.getClass().getMethod(methodName, argTypes);
                    method.invoke(taskbar, args);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception while calling task bar API", e);
                }
            }
        }
    }
}
