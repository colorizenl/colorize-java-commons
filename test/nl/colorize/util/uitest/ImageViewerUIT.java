//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
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
import java.util.List;
import java.util.Random;

/**
 * Simple test application for {@link ImageViewer} that displays all images in
 * a directory. Will display a random image when the spacebar is pressed.
 */
public class ImageViewerUIT {

    private List<File> imageFiles;
    private Cache<File, BufferedImage> imageCache;
    private List<File> history;

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
        this.imageCache = Cache.from(this::loadImage, IMAGE_CACHE_SIZE);
        this.history = new ArrayList<>();

        imageViewer = new ImageViewer(true);
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
            if (e.getID() == KeyEvent.KEY_RELEASED) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    selectRandomImage();
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_O) {
                    openSelectedFile();
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    previous();
                    return true;
                }
            }

            return false;
        });
    }

    private void createImageList() {
        TranslationBundle bundle = SwingUtils.getCustomComponentsBundle();
        imageList = new Table<>(
            bundle.getString("ImageViewer.imageFile"),
            bundle.getString("ImageViewer.imageFileSize")
        );
        imageList.setColumnWidth(1, 80);
        imageList.onDoubleClick().subscribe(this::selectImage);
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
            imageCache.forgetAll();
            refreshImageList();
            selectImage(selected);
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
            history.clear();
            return;
        }

        if (history.size() == imageFiles.size()) {
            history.clear();
        }

        List<File> remaining = imageFiles.stream()
            .filter(file -> !history.contains(file))
            .toList();

        int index = new Random().nextInt(remaining.size());
        File next = remaining.get(index);
        selectImage(next);
    }

    private void selectImage(File file) {
        if (file != null) {
            imageViewer.display(imageCache.get(file));
            imageList.setSelectedRowKey(file);
            history.add(file);
        }
    }

    private void openSelectedFile() {
        if (!history.isEmpty()) {
            if (Platform.isMac()) {
                MacIntegration.revealInFinder(history.getLast());
            } else {
                SwingUtils.openFile(history.getLast());
            }
        }
    }

    private void previous() {
        if (history.size() >= 2) {
            history.removeLast();
            File previousFile = history.removeLast();
            imageViewer.display(imageCache.get(previousFile));
            imageList.setSelectedRowKey(previousFile);
            history.add(previousFile);
        }
    }
}
