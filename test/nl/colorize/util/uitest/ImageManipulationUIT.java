//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import lombok.AllArgsConstructor;
import nl.colorize.util.FileUtils;
import nl.colorize.util.Platform;
import nl.colorize.util.ResourceFile;
import nl.colorize.util.Signal;
import nl.colorize.util.TranslationBundle;
import nl.colorize.util.Tuple;
import nl.colorize.util.TupleList;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.ImageViewer;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Table;
import nl.colorize.util.swing.Utils2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Swing application that can be used to transform one or more images using
 * Java2D image manipulation. This acts as a showcase for the functionality
 * provided by this library, but it also acts as an application that is useful
 * in itself.
 */
public class ImageManipulationUIT {

    private List<EditedImage> images;
    private TranslationBundle bundle;
    private TupleList<String, ImageFilter> availableFilters;
    private TupleList<String, ImageFilter> activeFilters;

    // UI
    private JFrame window;
    private Table<EditedImage> imageTable;
    private FormPanel filterForm;
    private ImageViewer originalViewer;
    private ImageViewer processedViewer;

    // Filter options
    private int rotationDegrees = 0;
    private int sheerAngle = 45;
    private int blurSize = 2;
    private Color tintColor = Color.WHITE;
    private Color shadowColor = Color.BLACK;
    private int shadowAlpha = 128;
    private int shadowSize = 1;
    private int shadowBlur = 2;

    private static final ResourceFile EXAMPLE_IMAGE = new ResourceFile("colorize-logo-180.png");
    private static final ResourceFile BUNDLE_FILE = new ResourceFile("custom-swing-components.properties");
    private static final Color BORDER_COLOR = new Color(173, 173, 173);
    private static final Color SELECTED_COLOR = new Color(228, 93, 97);
    private static final List<String> BACKGROUNDS = List.of("None", "Checkerboard", "Isometric");

    public static void main() {
        SwingUtils.initializeSwing();

        ImageManipulationUIT app = new ImageManipulationUIT();
        app.show();
    }

    private ImageManipulationUIT() {
        this.images = new ArrayList<>();
        this.bundle = TranslationBundle.from(Locale.US, BUNDLE_FILE);
        this.availableFilters = new TupleList<>();
        this.activeFilters = new TupleList<>();

        availableFilters.add("Mirror horizontal", this::mirrorHorizontal);
        availableFilters.add("Mirror vertical", this::mirrorVertical);
        availableFilters.add("Rotate", this::rotate);
        availableFilters.add("Shear", this::shear);
        availableFilters.add("Isometric", this::isometric);
        availableFilters.add("Blur", this::blur);
        availableFilters.add("Tint", this::tint);
        availableFilters.add("Drop shadow", this::dropShadow);

        BufferedImage exampleImage = Utils2D.loadImage(EXAMPLE_IMAGE);
        images.add(new EditedImage("example", exampleImage, exampleImage));
    }

    private void show() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.add(createPreviewPanel(), BorderLayout.CENTER);
        contentPanel.add(createSelectionPanel(), BorderLayout.WEST);

        window = new JFrame("Image Manipulation");
        window.setSize(1100, 700);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout(0, 20));
        window.add(contentPanel, BorderLayout.CENTER);
        window.setVisible(true);

        refreshImageTable();
        refreshImageViewers(true);
    }

    private JPanel createSelectionPanel() {
        imageTable = new Table<>("Image", "Size");
        imageTable.setColumnWidth(1, 80);
        imageTable.addActionListener(_ -> refreshImageViewers(false));
        SwingUtils.setPreferredWidth(imageTable, 250);

        filterForm = new FormPanel();
        refreshFilterForm();

        JPanel filterPanel = new JPanel(new BorderLayout(0, 20));
        filterPanel.add(filterForm, BorderLayout.NORTH);
        filterPanel.add(createFilterOptionsPanel(), BorderLayout.SOUTH);
        SwingUtils.setPreferredWidth(filterPanel, 250);

        JPanel imagePanel = new JPanel(new BorderLayout(0, 20));
        imagePanel.add(createButtonPanel(), BorderLayout.NORTH);
        imagePanel.add(imageTable, BorderLayout.CENTER);
        imagePanel.add(createViewOptionsForm(), BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.add(imagePanel, BorderLayout.WEST);
        panel.add(filterPanel, BorderLayout.CENTER);
        return panel;
    }

    private FormPanel createViewOptionsForm() {
        FormPanel form = new FormPanel();
        form.addRow(createHeader("View options"));
        form.addStringField("Background:", BACKGROUNDS, "Checkerboard")
            .getChanges().subscribe(this::changeBackground);
        form.addBooleanField("Show outline", false).getChanges().subscribe(value -> {
            originalViewer.setOutlineColor(value ? Color.PINK : null);
            processedViewer.setOutlineColor(value ? Color.PINK : null);
        });
        SwingUtils.setPreferredWidth(form, 250);
        return form;
    }

    private void changeBackground(String value) {
        if (value.equals("Checkerboard")) {
            originalViewer.useCheckerboardBackground();
            processedViewer.useCheckerboardBackground();
        } else if (value.equals("Isometric")) {
            originalViewer.useEmptyBackground();
            processedViewer.changeZoom(1f);
            processedViewer.useCustomBackground(this::renderIsometricBackground);
        } else {
            originalViewer.useEmptyBackground();
            processedViewer.useEmptyBackground();
        }
    }

    private void renderIsometricBackground(Graphics2D g2) {
        int baseX = processedViewer.getWidth() / 2;
        int baseY = processedViewer.getHeight() / 2;

        for (int offset : List.of(45, 65, 85, 105, 125)) {
            int[] x = {baseX, baseX + 2 * offset, baseX, baseX - 2 * offset};
            int[] y = {baseY - offset, baseY, baseY + offset, baseY};

            g2.setColor(BORDER_COLOR);
            g2.drawPolygon(x, y, 4);
        }
    }

    private JPanel createFilterOptionsPanel() {
        FormPanel form = new FormPanel();
        form.addRow(createHeader("Filter options"));
        updateFilter(form.addIntField("Rotate:", rotationDegrees, 0, 359), value -> rotationDegrees = value);
        updateFilter(form.addIntField("Shear:", sheerAngle, 0, 45), value -> sheerAngle = value);
        updateFilter(form.addIntField("Blur:", blurSize, 1, 20), value -> blurSize = value);
        updateColorFilter(form.addStringField("Tint color:", Utils2D.toHexColor(tintColor)),
            value -> tintColor = value);
        updateColorFilter(form.addStringField("Shadow color:", Utils2D.toHexColor(shadowColor)),
            value -> shadowColor = value);
        updateFilter(form.addIntField("Shadow alpha:", shadowAlpha, 0, 255), value -> shadowAlpha = value);
        updateFilter(form.addIntField("Shadow size:", shadowSize, 1, 20), value -> shadowSize = value);
        updateFilter(form.addIntField("Shadow blur:", shadowBlur, 1, 20), value -> shadowBlur = value);
        return form;
    }

    private void updateFilter(Signal<Integer> field, Consumer<Integer> callback) {
        field.getChanges().subscribe(value -> {
            callback.accept(value);
            applyFilters();
        });
    }

    private void updateColorFilter(Signal<String> field, Consumer<Color> callback) {
        field.getChanges().subscribe(value -> {
            callback.accept(Utils2D.parseHexColor(value));
            applyFilters();
        });
    }

    private JPanel createButtonPanel() {
        JButton openButton = new JButton("Open");
        openButton.addActionListener(_ -> openImages());

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(_ -> exportImages());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panel.add(openButton);
        panel.add(exportButton);
        return panel;
    }

    private JPanel createPreviewPanel() {
        originalViewer = new ImageViewer();
        originalViewer.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        originalViewer.useCheckerboardBackground();

        processedViewer = new ImageViewer();
        processedViewer.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        processedViewer.useCheckerboardBackground();

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 20));
        panel.add(wrapImageViewer("Original image", originalViewer));
        panel.add(wrapImageViewer("Processed image", processedViewer));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        return panel;
    }

    private JPanel wrapImageViewer(String header, ImageViewer imageViewer) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.add(createHeader(header), BorderLayout.NORTH);
        panel.add(imageViewer, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createHeader(String header) {
        JLabel label = new JLabel(header);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return label;
    }

    private void openImages() {
        ComboFileDialog fileDialog = new ComboFileDialog();
        fileDialog.showOpenDialog(null).ifPresent(selected -> {
            images.clear();

            try {
                for (File file : FileUtils.walkFiles(selected.getParentFile(), this::isImageFile)) {
                    images.add(loadImage(file));
                }

                refreshImageTable();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load image", e);
            }
        });
    }

    private boolean isImageFile(File file) {
        return file.getName().endsWith(".jpg") || file.getName().endsWith(".png");
    }

    private EditedImage loadImage(File source) throws IOException {
        String name = source.getName().replaceAll("[.](png|jpg)$", "");
        BufferedImage original = Utils2D.loadImage(source);
        return new EditedImage(name, original, original);
    }

    private void exportImages() {
        File desktop = Platform.getUserDesktopDir();
        File outputDir = new File(desktop, "images-" + System.currentTimeMillis());
        outputDir.mkdir();

        try {
            for (EditedImage image : images) {
                File outputFile = new File(outputDir, image.name + ".png");
                Utils2D.savePNG(image.processed, outputFile);
            }

            SwingUtils.openFile(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export images", e);
        }
    }

    private void refreshImageViewers(boolean zoomToFit) {
        EditedImage selected = imageTable.getSelectedRowKey();
        if (selected != null) {
            originalViewer.display(selected.original, zoomToFit);
            processedViewer.display(selected.processed, zoomToFit);
        }
    }

    private void refreshImageTable() {
        imageTable.removeAllRows();

        for (EditedImage image : images) {
            String size = image.original.getWidth() + "x" + image.original.getHeight();
            imageTable.addRow(image, image.name, size);
        }

        if (!images.isEmpty()) {
            imageTable.setSelectedRowKey(images.getFirst());
        }
    }

    private void refreshFilterForm() {
        filterForm.removeAll();
        filterForm.addRow(createHeader("Image filters"));

        for (Tuple<String, ImageFilter> filter : availableFilters) {
            JButton filterButton = new JButton(filter.left());
            if (activeFilters.containsLeft(filter.left())) {
                filterButton.setFont(filterButton.getFont().deriveFont(Font.BOLD));
                filterButton.setForeground(SELECTED_COLOR);
                filterButton.addActionListener(_ -> removeFilter(filter));
            } else {
                filterButton.addActionListener(_ -> addFilter(filter));
            }
            filterForm.addRow(filterButton);
        }

        filterForm.revalidate();
    }

    private void addFilter(Tuple<String, ImageFilter> filter) {
        activeFilters.add(filter);
        refreshFilterForm();
        applyFilters();
    }

    private void removeFilter(Tuple<String, ImageFilter> filter) {
        activeFilters.remove(filter);
        refreshFilterForm();
        applyFilters();
    }

    private void applyFilters() {
        for (EditedImage image : images) {
            BufferedImage processed = image.original;
            for (ImageFilter filter : activeFilters.getRight()) {
                processed = filter.apply(processed);
            }
            image.processed = processed;
        }

        refreshImageViewers(false);
    }

    private BufferedImage mirrorHorizontal(BufferedImage image) {
        BufferedImage out = new BufferedImage(image.getWidth() * 2, image.getHeight(), TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.drawImage(image, 2 * image.getWidth(), 0, -image.getWidth(), image.getHeight(), null);
        g2.dispose();
        return out;
    }

    private BufferedImage mirrorVertical(BufferedImage image) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight() * 2, TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.drawImage(image, 0, 2 * image.getHeight(), image.getWidth(), -image.getHeight(), null);
        g2.dispose();
        return out;
    }

    private BufferedImage rotate(BufferedImage image) {
        int rotatedSize = Math.round(1.5f * Math.max(image.getWidth(), image.getHeight()));

        AffineTransform transform = new AffineTransform();
        transform.setToIdentity();
        transform.translate(rotatedSize / 2f,  rotatedSize / 2f);
        transform.rotate(Math.toRadians(rotationDegrees), rotatedSize / 2f, rotatedSize / 2f);

        BufferedImage result = new BufferedImage(rotatedSize, rotatedSize, TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(result, true, true);
        g2.translate(rotatedSize / 2, rotatedSize / 2);
        g2.rotate(Math.toRadians(rotationDegrees));
        g2.drawImage(image, -image.getWidth() / 2, -image.getHeight() / 2,
            image.getWidth(), image.getHeight(), null);
        g2.dispose();
        return result;
    }

    private BufferedImage shear(BufferedImage image) {
        double shearedWidth = image.getWidth();
        double shearedHeight = (((sheerAngle / 90.0) * 1.5) + 1.0) * image.getHeight();

        AffineTransform transform = new AffineTransform();
        transform.setToIdentity();
        transform.translate(0.0, (shearedHeight - image.getHeight()) / 2.0);
        transform.translate(shearedWidth / 2.0, shearedHeight / 2.0);
        transform.shear(0.0, Math.toRadians(sheerAngle));
        transform.translate(-shearedWidth / 2.0, -shearedHeight / 2.0);

        BufferedImage result = new BufferedImage((int) Math.round(shearedWidth),
            (int) Math.round(shearedHeight), TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(result, true, true);
        g2.drawImage(image, transform, null);
        g2.dispose();
        return result;
    }

    private BufferedImage isometric(BufferedImage image) {
        rotationDegrees = 45;
        BufferedImage rotated = rotate(image);
        return Utils2D.scaleImage(rotated, rotated.getWidth(), rotated.getHeight() / 2, true);
    }

    private BufferedImage blur(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(result, true, true);
        g2.drawImage(Utils2D.applyGaussianBlur(image, Math.max(blurSize, 1)), 0, 0, null);
        g2.dispose();
        return result;
    }

    private BufferedImage tint(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(result, true, true);
        g2.drawImage(Utils2D.applyTint(image, tintColor), 0, 0, null);
        g2.dispose();
        return result;
    }

    private BufferedImage dropShadow(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(result, true, true);
        g2.drawImage(Utils2D.applyDropShadow(image, Utils2D.withAlpha(shadowColor, shadowAlpha),
            shadowSize, shadowBlur), 0, 0, null);
        g2.dispose();
        return result;
    }

    /**
     * One of the images that is being edited by this application. Contains
     * both the original image and the image after applying all currently
     * active filters.
     */
    @AllArgsConstructor
    private static class EditedImage {

        private String name;
        private BufferedImage original;
        private BufferedImage processed;
    }

    @FunctionalInterface
    private static interface ImageFilter {

        public BufferedImage apply(BufferedImage image);
    }
}
