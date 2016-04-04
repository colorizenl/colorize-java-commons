//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.FileDialog;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.primitives.Chars;

import nl.colorize.util.DynamicResourceBundle;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.Platform;

/**
 * A wrapper around both AWT's {@link java.awt.FileDialog} and Swing's
 * {@link javax.swing.JFileChooser}. While the Swing file dialog is superior in
 * terms of functionality, its appearance can be far from native dialogs on some 
 * platforms. AWT dialogs look like native dialogs, but provide less features.
 * This class will therefore make the tradeoff which file dialog is best for the
 * current platform. 
 */
public class ComboFileDialog {

	private String title;
	private File startDirectory;
	private FileExtFilter filter;
	
	protected DynamicResourceBundle bundle;
	
	private static final Charset FILENAME_CHARSET = Charsets.US_ASCII;
	private static final List<Character> ILLEGAL_CHARS = Chars.asList('\\', '/', ':', ';', '*', '|', '<', '>');
	private static final List<String> SYSTEM_FILES = ImmutableList.of("desktop.ini");
	
	/**
	 * Creates a file dialog with the specified title and start directory.
	 * @param title The dialog window's title.
	 * @param start The directory where the dialog will start.
	 */
	public ComboFileDialog(String title, File start) {
		setTitle(title);
		setStartDirectory(start);
		filter = null;
		
		bundle = createDefaultResourceBundle();
	}
	
	/**
	 * Creates a file dialog with the platform's default dialog title and start
	 * directory.
	 */
	public ComboFileDialog() {
		this(null, new File(""));
	}
	
	private DynamicResourceBundle createDefaultResourceBundle() {
		Properties defaults = LoadUtils.toProperties(
				"defaultOpenTitle", "Open File...",
				"defaultSaveTitle", "Save File...",
				"noName", "No file name was entered.",
				"illegalChars", "The file name contains characters that are not allowed.",
				"invalidExt", "Please select a file with one of the following extensions: {0}.",
				"overwrite", "The file '{0}' already exists.\nDo you want to overwrite it?");
		return new DynamicResourceBundle(defaults);
	}
	
	/**
	 * Returns whether file dialogs are created using Swing (if this returns true)
	 * or AWT (if this returns false). 
	 */
	public boolean usesSwingDialogs() {
		return !Platform.isOSX();
	}
	
	/**
	 * Shows an 'open file' dialog and returns the selected file. Returns
	 * {@code null} if the dialog was cancelled.
	 * @param parent The parent window for the dialog.
	 */
	public File showOpenDialog(JFrame parent) {
		File selected = usesSwingDialogs() ? showSwingOpenDialog(parent) : showAWTOpenDialog(parent);
		
		if (selected == null || !selected.exists()) {
			return null;
		}
		
		if (!hasValidExtension(selected)) {
			Popups.message(parent, bundle.getString("invalidExt",
					filter.extensions.toString().replaceAll("\\[(.+)\\]", "$1")));
			return showOpenDialog(parent);
		}
		
		return selected;
	}
	
	private File showAWTOpenDialog(JFrame parent) {
		FileDialog fileDialog = createAWTFileDialog(parent, false);
		fileDialog.setVisible(true);
		if (fileDialog.getDirectory() != null && fileDialog.getFile() != null) {
			return new File(fileDialog.getDirectory(), fileDialog.getFile());
		} else {
			return null;
		}
	}

	private File showSwingOpenDialog(JFrame parent) {
		JFileChooser fileDialog = createSwingFileDialog(parent, false);
		if (fileDialog.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			return fileDialog.getSelectedFile();
		} else {
			return null;
		}
	}
	
	/**
	 * Shows a 'save file' dialog, and returns the selected file. If the selected
	 * file already exists, an extra confirmation dialog is shown to make sure the
	 * user wants to overwrite the file. Returns {@code null} if the dialog was
	 * cancelled.
	 * @param parent The parent window for the dialog.
	 * @param extension File extension to use if none is entered.
	 */
	public File showSaveDialog(JFrame parent, String extension) {
		File selected = usesSwingDialogs() ? showSwingSaveDialog(parent) : showAWTSaveDialog(parent);
		
		if (selected == null) {
			return null;
		}
		
		if (!hasValidFileName(selected)) {
			Popups.message(null, bundle.getString("illegalChars"));
			return showSaveDialog(parent, extension);
		}
		
		if (selected.exists()) {
			return showOverrideFileDialog(parent, selected);
		}
		
		if (!hasValidExtension(selected, extension) && !Platform.isOSXAppSandboxEnabled()) {
			// Use the default file extension if none was entered. Note that this
			// is not allowed when running in the Mac App Store sandbox.
			selected = new File(selected.getParentFile(), selected.getName() + "." + extension);
		}
		
		return selected;
	}
	
	private File showSwingSaveDialog(JFrame parent) {
		JFileChooser fileDialog = createSwingFileDialog(parent, true);
		if (fileDialog.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			return fileDialog.getSelectedFile();
		} else {
			return null;
		}
	}

	private File showAWTSaveDialog(JFrame parent) {
		FileDialog fileDialog = createAWTFileDialog(parent, true);
		fileDialog.setVisible(true);
		if (fileDialog.getDirectory() != null && fileDialog.getFile() != null) {
			return new File(fileDialog.getDirectory(), fileDialog.getFile());
		} else {
			return null;
		}
	}
	
	private File showOverrideFileDialog(JFrame parent, File selected) {
		if (Popups.confirmMessage(parent, bundle.getString("overwrite", selected.getName()))) {
			return selected;
		} else {
			return null;
		}
	}

	private FileDialog createAWTFileDialog(JFrame parent, boolean saveMode) {
		FileDialog fileDialog = new FileDialog(parent);
		fileDialog.setMode(saveMode ? FileDialog.SAVE : FileDialog.LOAD);
		fileDialog.setTitle(getRealTitle(saveMode));
		fileDialog.setDirectory(startDirectory.getAbsolutePath());
		return fileDialog;
	}
	
	private JFileChooser createSwingFileDialog(JFrame parent, boolean saveMode) {
		JFileChooser dialog = new JFileChooser();
		dialog.setDialogTitle(getRealTitle(saveMode));
        dialog.setCurrentDirectory(startDirectory);
        dialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
        dialog.setMultiSelectionEnabled(false); 
        if (filter != null) {
        	dialog.setFileFilter(filter);
        }
        return dialog;	
	}
	
	private String getRealTitle(boolean saveMode) {
		if (title != null) {
			return title;
		} else {
			if (saveMode) {
				return bundle.getString("defaultSaveTitle");
			} else {
				return bundle.getString("defaultOpenTitle");
			}
		}
	}
	
	/**
	 * Checks if an entered file name is valid. This method is used when saving a
	 * file, and checks for things such as an empty name, use of illegal characters,
	 * or characters that are not ASCII (since that is the only charset that is
	 * guananteed to work on all platforms).
	 */
	private boolean hasValidFileName(File file) {
		String name = file.getName();
		
		for (Character c : ILLEGAL_CHARS) {
            if (name.contains(c.toString())) {
                return false;
            }
        }
		
		return name.trim().length() >= 1 && FILENAME_CHARSET.newEncoder().canEncode(name);
	}

	private boolean hasValidExtension(File file, String... extensions) {
		if (filter == null && extensions.length == 0) {
			return true;
		}
		
		if (filter != null && filter.accept(file)) {
			return true;
		}
		
		String ext = Files.getFileExtension(file.getName()).toLowerCase();
		return ImmutableList.copyOf(FileExtFilter.normalizeFileExtensions(extensions)).contains(ext);
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setStartDirectory(File start) {
		if (start == null || !start.exists()) {
			// Use the platform default directory
			this.startDirectory = getDefaultStartDirectory();
			return;
		}
		
		if (start.isDirectory()) {
			this.startDirectory = start;
		} else {
			this.startDirectory = start.getParentFile();
		}
	}
	
	public void setStartDirectory(String path) {
		if (path != null && path.length() > 0) {
			setStartDirectory(new File(path));
		} else {
			startDirectory = getDefaultStartDirectory();
		}
	}
	
	public File getStartDirectory() {
		return startDirectory;
	}
	
	private File getDefaultStartDirectory() {
		return Platform.getUserDataDirectory();
	}
	
	/**
	 * Sets a filter so that the dialog will only show files with the specified
	 * file extension(s). 
	 * @param description A textual description of the filter.
	 * @param extensions An array of file name extensions, such as "png", or "jpg".
	 * @throws IllegalArgumentException if the filter has no file extensions.
	 */
	public void setFilter(String description, String... extensions) {
		if (extensions.length == 0) {
			throw new IllegalArgumentException("Filter must have at least one file extension"); 
		}
		filter = new FileExtFilter(description, extensions);
	}
	
	/**
	 * Simple implementation of a file name filter that allows files with one of
	 * the passed file extensions, as well as all directories.
	 */
	private static class FileExtFilter extends FileFilter {
		
		private String description;
		private List<String> extensions;
	
		public FileExtFilter(String description, String... extensions) {
			this.description = buildDescriptionString(description, extensions);
			this.extensions = ImmutableList.copyOf(normalizeFileExtensions(extensions));
		}
		
		@Override
		public boolean accept(File file) {
			if (file.isDirectory()) {
				return true;
			} else {
				String ext = Files.getFileExtension(file.getName()).toLowerCase();
				return extensions.contains(ext) && !SYSTEM_FILES.contains(file.getName().toLowerCase());
			}
		}
		
		@Override
		public String getDescription() {
			return description;
		}
		
		private String buildDescriptionString(String base, String... extensions) {
			return "(" + Joiner.on(", ").join(extensions) + ")";
		}
		
		public static String[] normalizeFileExtensions(String[] ext) {
			for (int i = 0; i < ext.length; i++) {
				if (ext[i].indexOf('.') != -1) {
					ext[i] = ext[i].substring(ext[i].indexOf('.') + 1);
				}
			}
			return ext;
		}
	}
}
