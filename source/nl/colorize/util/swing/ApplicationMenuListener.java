//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

/**
 * Interface for receiving events from the macOS application menu. Once
 * a listener has been registered using
 * {@link MacIntegration#setApplicationMenuListener(ApplicationMenuListener)}
 * they are notified whenever a menu item in the application menu is clicked. 
 */
public interface ApplicationMenuListener {

    /**
     * Called when the "Quit" application menu item is clicked. This method
     * is invoked right before the application exits.
     */
    public void onQuit();
    
    /**
     * Called when the "About" application menu item is clicked.
     */
    public void onAbout();

    /**
     * When true, this will show the "Preferences" entry as part of the
     * application menu. If this returns false, the preferences will be
     * hidden, meaning that {@link #onPreferences()} is inaccessible.
     */
    public boolean hasPreferencesMenu();
    
    /**
     * Called when the "Preferences" application menu item (if available) is
     * clicked. This method is not accessible if {@link #hasPreferencesMenu()}
     * returns false.
     */
    public void onPreferences();
}
