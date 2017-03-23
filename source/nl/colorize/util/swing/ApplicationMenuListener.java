//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

/**
 * Interface for receiving events from the macOS Application Menu. Once listeners
 * have been registered using 
 * {@link MacIntegration#setApplicationMenuListener(ApplicationMenuListener, boolean)}
 * they are notified whenever a menu item in the application menu is clicked. 
 */
public interface ApplicationMenuListener {

	/**
	 * Called when the "Quit" application menu item is clicked.
	 */
	public void onQuit();
	
	/**
	 * Called when the "About" application menu item is clicked.
	 */
	public void onAbout();
	
	/**
	 * Called when the "Preferences" application menu item (if available) is
	 * clicked.
	 */
	public void onPreferences();
}
