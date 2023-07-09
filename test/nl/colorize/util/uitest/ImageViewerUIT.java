//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import com.google.common.base.Preconditions;
import nl.colorize.util.FileUtils;
import nl.colorize.util.Platform;
import nl.colorize.util.TranslationBundle;
import nl.colorize.util.stats.Cache;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.ImageViewer;
import nl.colorize.util.swing.MacIntegration;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Table;
import nl.colorize.util.swing.Utils2D;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Simple test application for {@link ImageViewer} that displays all images in
 * a directory. Will display a random image when the spacebar is pressed.
 */
public class ImageViewerUIT {

    private List<File> imageFiles;
    private Set<File> seen;
    private File selectedFile;
    private Cache<File, BufferedImage> imageCache;

    private ImageViewer imageViewer;
    private Table<File> imageList;

    private static final int IMAGE_CACHE_SIZE = 1100;

    public static void main(String[] args) {
        SwingUtils.initializeSwing();

        ImageViewerUIT imageViewer = new ImageViewerUIT();
        imageViewer.openImageDirectory();
    }

    public ImageViewerUIT() {
        this.imageFiles = new ArrayList<>();
        this.seen = new HashSet<>();
        this.imageCache = Cache.from(this::loadImage, IMAGE_CACHE_SIZE);

        createImageViewer();
        createImageList();
        createKeyboardListener();

        JFrame window = new JFrame("Image Viewer");
        window.setPreferredSize(new Dimension(1000, 600));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout(0, 0));
        window.add(imageViewer, BorderLayout.CENTER);
        window.add(imageList, BorderLayout.WEST);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setResizable(true);
        window.setVisible(true);
    }

    private void createKeyboardListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_SPACE) {
                selectRandomImage();
                return true;
            } else if (e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == KeyEvent.VK_O) {
                openSelectedFile();
                return true;
            } else {
                return false;
            }
        });
    }

    private void createImageViewer() {
        imageViewer = new ImageViewer(true);
    }

    private void createImageList() {
        TranslationBundle bundle = SwingUtils.getCustomComponentsBundle();
        imageList = new Table<>(bundle.getString("ImageViewer.imageFile"),
            bundle.getString("ImageViewer.imageFileSize"));
        imageList.setColumnWidth(1, 80);
        imageList.addDoubleClickListener(e -> selectImage(imageList.getSelectedRowKey()));
        SwingUtils.setPreferredWidth(imageList, 300);
    }

    private void refreshImageList() {
        imageList.removeAllRows();

        for (File imageFile : imageFiles) {
            String size = FileUtils.formatFileSize(imageFile);
            imageList.addRow(imageFile, imageFile.getName(), size);
        }
    }

    private void openImageDirectory() {
        ComboFileDialog fileDialog = new ComboFileDialog();
        File selected = fileDialog.showOpenDialog(null);

        if (selected != null) {
            File dir = selected.getParentFile();
            imageFiles = locateImageFiles(dir);
            seen.clear();
            imageCache.forgetAll();
            refreshImageList();
            selectRandomImage();
        }
    }

    private List<File> locateImageFiles(File dir) {
        File[] images = dir.listFiles((file, name) -> name.endsWith("jpg") || name.endsWith(".png"));
        Preconditions.checkState(images != null, "Unable to open directory: " + dir.getAbsolutePath());

        return Arrays.stream(images)
            .sorted()
            .toList();
    }

    private BufferedImage loadImage(File file) {
        try {
            return Utils2D.loadImage(file);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load image: " + file.getAbsolutePath(), e);
        }
    }

    private void selectRandomImage() {
        if (imageFiles.isEmpty()) {
            selectedFile = null;
            return;
        }

        if (seen.size() == imageFiles.size()) {
            seen.clear();
        }

        List<File> remaining = imageFiles.stream()
            .filter(file -> !seen.contains(file))
            .toList();

        int index = new Random().nextInt(remaining.size());
        selectedFile = remaining.get(index);
        BufferedImage displayedImage = imageCache.get(selectedFile);
        imageViewer.display(displayedImage);
        seen.add(selectedFile);
    }

    private void selectImage(File file) {
        if (file != null) {
            imageViewer.display(imageCache.get(file));
        }
    }

    private void openSelectedFile() {
        if (selectedFile != null) {
            if (Platform.isMac()) {
                MacIntegration.revealInFinder(selectedFile);
            } else {
                SwingUtils.openFile(selectedFile);
            }
        }
    }
}
