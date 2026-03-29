//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import nl.colorize.util.Platform;
import nl.colorize.util.TranslationBundle;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;
import java.awt.FileDialog;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

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
    private FileFilter filter;

    protected TranslationBundle bundle;
    
    private static final Charset FILENAME_CHARSET = StandardCharsets.US_ASCII;
    private static final CharMatcher INVALID_CHARS = CharMatcher.anyOf("\\/:;*|<>");
    private static final List<String> SYSTEM_FILES = List.of("desktop.ini", ".DS_Store");

    /**
     * Creates a new file dialog that will start in the specified location
     * and will only accept files that match the file filter.
     */
    public ComboFileDialog(@Nullable String title, @Nullable File start, FileFilter filter) {
        this.title = title;
        this.filter = filter;
        this.bundle = SwingUtils.getCustomComponentsBundle();

        if (start == null) {
            startDirectory = Platform.getUserDataDirectory();
        } else if (start.isDirectory()) {
            startDirectory = start;
        } else {
            startDirectory = start.getParentFile();
        }
    }

    /**
     * Creates a new file dialog that will start in the specified location
     * and will only accept files with one of the specified file extensions.
     */
    public ComboFileDialog(@Nullable String title, @Nullable File start, List<String> extensions) {
        this(title, start, new FileExtFilter(extensions));
    }

    /**
     * Creates a new file dialog that will start in the platform's default
     * user data location and will only accept files with one of the
     * specified file extensions.
     */
    public ComboFileDialog(List<String> extensions) {
        this(null, null, extensions);
    }

    /**
     * Creates a new file dialog that will start in the specified location
     * and does not have any restrictions in terms of which files it will
     * or will not accept.
     *
     * @deprecated It's pretty uncommon to have a file dialog in applications
     *             without <em>any</em> form of restrictions. It's generally
     *             recommended to be specific within the file dialog itself
     *             on which types of files you want users to be able to select.
     */
    @Deprecated
    public ComboFileDialog() {
        this(null, null, new AcceptAllFilter());
    }

    /**
     * Returns whether file dialogs are created using Swing (if this returns true)
     * or AWT (if this returns false). 
     */
    private boolean usesSwingDialogs() {
        return !Platform.isMac();
    }
    
    /**
     * Shows an "open file" dialog and returns the selected file. Does not
     * return a file if the user canceled the dialog window.
     *
     * @param parent The parent window for the file dialog. A value of
     *               {@code null} means the file dialog is considered global
     *               for the entire (Swing) application.
     */
    public Optional<File> showOpenDialog(@Nullable JFrame parent) {
        File selected = usesSwingDialogs() ? showSwingOpenDialog(parent) : showAWTOpenDialog(parent);
        
        if (selected == null || !selected.exists()) {
            return Optional.empty();
        }
        
        if (!hasValidFileExtension(selected)) {
            Popups.builder()
                .withWarningIcon()
                .withMessage(bundle.getString("ComboFileDialog.invalidExt", filter.getDescription()))
                .show(parent);

            return showOpenDialog(parent);
        }
        
        return Optional.of(selected);
    }

    /**
     * Shows an "open file" dialog and returns the selected file. Does not
     * return a file if the user canceled the dialog window.
     */
    public Optional<File> showOpenDialog() {
        return showOpenDialog(null);
    }
    
    private File showAWTOpenDialog(JFrame parent) {
        FileDialog fileDialog = createAWTFileDialog(parent, false);
        fileDialog.setVisible(true);
        if (fileDialog.getDirectory() == null || fileDialog.getFile() == null) {
            return null;
        }
        return new File(fileDialog.getDirectory(), fileDialog.getFile());
    }

    private File showSwingOpenDialog(JFrame parent) {
        JFileChooser fileDialog = createSwingFileDialog(false);
        if (fileDialog.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fileDialog.getSelectedFile();
    }
    
    /**
     * Shows a "save file" dialog and returns the selected file. Does not
     * return a file if the user canceled the dialog window. If the selected
     * file already exists, an extra confirmation dialog is shown to make
     * sure the user wants to overwrite the file.
     * <p>
     * If this file dialog was created with a filter on file extension, and
     * the entered file name does not have a file extension, it will be
     * automatically added based on the filter.
     *
     * @param parent The parent window for the file dialog. A value of
     *               {@code null} means the file dialog is considered global
     *               for the entire (Swing) application.
     */
    public Optional<File> showSaveDialog(@Nullable JFrame parent) {
        File selected = usesSwingDialogs() ? showSwingSaveDialog(parent) : showAWTSaveDialog(parent);
        
        if (selected == null) {
            return Optional.empty();
        }
        
        if (!hasValidFileName(selected)) {
            Popups.message(null, bundle.getString("ComboFileDialog.illegalChars"));
            return showSaveDialog(parent);
        }
        
        if (selected.exists()) {
            return Optional.ofNullable(showOverrideFileDialog(parent, selected));
        }

        if (!hasValidFileExtension(selected) && !Platform.isMacAppStore()) {
            // Use the default file extension if none was entered. Note that this
            // is not allowed when running in the Mac App Store sandbox.
            String defaultExt = getDefaultFileExtension();
            if (defaultExt != null) {
                selected = new File(selected.getParentFile(), selected.getName() + "." + defaultExt);
            }
        }
        
        return Optional.of(selected);
    }

    /**
     * Shows a "save file" dialog and returns the selected file. Does not
     * return a file if the user canceled the dialog window. If the selected
     * file already exists, an extra confirmation dialog is shown to make
     * sure the user wants to overwrite the file.
     * <p>
     * If this file dialog was created with a filter on file extension, and
     * the entered file name does not have a file extension, it will be
     * automatically added based on the filter.
     */
    public Optional<File> showSaveDialog() {
        return showSaveDialog(null);
    }
    
    private File showSwingSaveDialog(JFrame parent) {
        JFileChooser fileDialog = createSwingFileDialog(true);
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
        String yesButton = bundle.getString("ComboFileDialog.overwriteYes");
        String noButton = bundle.getString("ComboFileDialog.overwriteNo");
        String message = bundle.getString("ComboFileDialog.overwrite", selected.getName());

        int choice = Popups.builder()
            .withWarningIcon()
            .withMessage(message)
            .withButtons(yesButton, noButton)
            .show(parent);

        return choice == 0 ? selected : null;
    }

    private FileDialog createAWTFileDialog(JFrame parent, boolean saveMode) {
        FileDialog fileDialog = new FileDialog(parent);
        fileDialog.setMode(saveMode ? FileDialog.SAVE : FileDialog.LOAD);
        fileDialog.setTitle(getTitle(saveMode));
        fileDialog.setDirectory(startDirectory.getAbsolutePath());
        return fileDialog;
    }
    
    private JFileChooser createSwingFileDialog(boolean saveMode) {
        JFileChooser dialog = new JFileChooser();
        dialog.setDialogTitle(getTitle(saveMode));
        dialog.setCurrentDirectory(startDirectory);
        dialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
        dialog.setMultiSelectionEnabled(false); 
        if (filter != null) {
        	dialog.setFileFilter(filter);
        }
        return dialog;	
    }
    
    private String getTitle(boolean saveMode) {
        if (title != null && !title.isEmpty()) {
            return title;
        } else if (saveMode) {
            return bundle.getString("ComboFileDialog.defaultSaveTitle");
        } else {
            return bundle.getString("ComboFileDialog.defaultOpenTitle");
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

        return !name.trim().isEmpty() &&
            INVALID_CHARS.matchesNoneOf(name) &&
            FILENAME_CHARSET.newEncoder().canEncode(name);
    }

    private boolean hasValidFileExtension(File file) {
        if (filter.accept(file)) {
            return true;
        }
        
        if (filter instanceof FileExtFilter extFilter) {
            String ext = Files.getFileExtension(file.getName()).toLowerCase();
            return extFilter.extensions.contains(ext);
        } else {
            return true;
        }
    }

    private String getDefaultFileExtension() {
        if (filter instanceof FileExtFilter extFilter) {
            return extFilter.extensions.getFirst();
        } else {
            return null;
        }
    }

    /**
     * Simple implementation of a file filter that allows files with one of
     * the passed file extensions, as well as all directories.
     */
    private static class FileExtFilter extends FileFilter {
        
        private List<String> extensions;
    
        public FileExtFilter(List<String> extensions) {
            Preconditions.checkArgument(!extensions.isEmpty(), "No file extensions");
            this.extensions = normalizeFileExtensions(extensions);
        }

        public List<String> normalizeFileExtensions(List<String> extensions) {
            return extensions.stream()
                .map(ext -> ext.startsWith(".") ? ext.substring(1) : ext)
                .map(String::toLowerCase)
                .toList();
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            } else if (SYSTEM_FILES.contains(file.getName())) {
                return false;
            } else {
                String ext = Files.getFileExtension(file.getName()).toLowerCase();
                return extensions.contains(ext);
            }
        }
        
        @Override
        public String getDescription() {
            return String.join(", ", extensions);
        }
    }

    /**
     * Implementation of a file filter that simply accepts all files without
     * any restrictions
     */
    private static class AcceptAllFilter extends FileFilter {

        @Override
        public boolean accept(File file) {
            return true;
        }

        @Override
        public String getDescription() {
            return SwingUtils.getCustomComponentsBundle().getString("ComboFileDialog.allFiles");
        }
    }
}
