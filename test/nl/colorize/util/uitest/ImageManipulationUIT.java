//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import nl.colorize.util.http.URLLoader;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Utils2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Swing application that showcases the graphical effects for Java2D.
 */
public class ImageManipulationUIT extends JPanel {

    private BufferedImage image;
    private int scale;
    private int blur;
    private Color tintColor;
    private int tintIntensity;
    private int shadowSize;
    private int shadowBlur;
    private int shadowAlpha;

    private static final String TEST_IMAGE_URL = "https://www.colorize.nl/logo.png";
    private static final int PADDING = 50;
    
    public static void main(String[] args) throws IOException {
        ImageManipulationUIT test = new ImageManipulationUIT();
        test.createWindow();
    }
    
    public ImageManipulationUIT() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
    
    private void createWindow() {
        loadPhoto();

        scale = 100;
        blur = 0;
        tintColor = Color.WHITE;
        tintIntensity = 0;
        shadowSize = 0;
        shadowBlur = 0;
        shadowAlpha = 255;
        
        add(createEditPanel(), BorderLayout.SOUTH);
        add(createToggleImagePanel(), BorderLayout.NORTH);
        
        JFrame window = new JFrame("Test Image Manipulation");
        window.setSize(650, 600);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);
        window.setLayout(new BorderLayout());
        window.add(this, BorderLayout.CENTER);
        window.setVisible(true);
    }
    
    private void loadPhoto() {
        URLLoader loader = URLLoader.get(TEST_IMAGE_URL);

        try (InputStream stream = loader.send().openStream()) {
            image = Utils2D.loadImage(stream);
            image = Utils2D.makeImageCompatible(image);
            image = Utils2D.addPadding(image, PADDING);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private JPanel createToggleImagePanel() {
        JButton toggleImage = new JButton("Change image");
        toggleImage.addActionListener(e -> changeImage());
        
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.add(toggleImage);
        return panel;
    }
    
    private void changeImage() {
        try {
            ComboFileDialog fileDialog = new ComboFileDialog();
            fileDialog.setFilter("Image files", ".png", ".jpg");
            File file = fileDialog.showOpenDialog(null);
            if (file != null) {
                image = Utils2D.loadImage(file);
                image = Utils2D.makeImageCompatible(image);
                image = Utils2D.addPadding(image, PADDING);
                repaint();
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    private JPanel createEditPanel() {
        JTabbedPane tabs = new JTabbedPane();
        SwingUtils.setPreferredHeight(tabs, 200);
        tabs.addTab("Scaling", createScalingPanel());
        tabs.addTab("Blur", createBlurPanel());
        tabs.addTab("Tint", createTintPanel());
        tabs.addTab("Drop Shadow", createDropShadowPanel());
        
        JPanel editPanel = new JPanel(new BorderLayout());
        editPanel.add(tabs, BorderLayout.CENTER);
        return editPanel;
    }
    
    private FormPanel createScalingPanel() {
        final JSlider scaleSlider = new JSlider(10, 200, 100);
        scaleSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                scale = scaleSlider.getValue();
                repaint();
            }
        });
        
        FormPanel panel = new FormPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.addRow("Scale:", scaleSlider, false);
        return panel;
    }

    private FormPanel createBlurPanel() {
        final JSlider blurSlider = new JSlider(0, 10, 0);
        blurSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                blur = blurSlider.getValue();
                repaint();
            }
        });
        
        FormPanel panel = new FormPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.addRow("Gaussian blur:", blurSlider, true);
        return panel;
    }
    
    private FormPanel createTintPanel() {
        JSlider redSlider = new JSlider(0, 255, 255);
        JSlider greenSlider = new JSlider(0, 255, 255);
        JSlider blueSlider = new JSlider(0, 255, 255);
        
        ChangeListener tintListener = e -> {
            tintColor = new Color(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            repaint();
        };
        redSlider.addChangeListener(tintListener);
        greenSlider.addChangeListener(tintListener);
        blueSlider.addChangeListener(tintListener);
        
        JSlider intensitySlider = new JSlider(0, 100, 0);
        intensitySlider.addChangeListener(e -> {
            tintIntensity = intensitySlider.getValue();
            repaint();
        });
        
        FormPanel panel = new FormPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.addRow("Red:", redSlider, true);
        panel.addRow("Green:", greenSlider, true);
        panel.addRow("Blue:", blueSlider, true);
        panel.addRow("Intensity:", intensitySlider, true);
        return panel;
    }
    
    private FormPanel createDropShadowPanel() {
        JSlider shadowSizeSlider = new JSlider(0, 50, 0);
        JSlider shadowBlurSlider = new JSlider(0, 20, 5);
        JSlider shadowAlphaSlider = new JSlider(0, 255, 255);
        
        ChangeListener shadowListener = e -> {
            shadowSize = shadowSizeSlider.getValue();
            shadowBlur = shadowBlurSlider.getValue();
            shadowAlpha = shadowAlphaSlider.getValue();
            repaint();
        };
        shadowSizeSlider.addChangeListener(shadowListener);
        shadowBlurSlider.addChangeListener(shadowListener);
        shadowAlphaSlider.addChangeListener(shadowListener);
        
        FormPanel panel = new FormPanel();
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.addRow("Shadow size:", shadowSizeSlider, true);
        panel.addRow("Shadow blur:", shadowBlurSlider, true);
        panel.addRow("Shadow alpha:", shadowAlphaSlider, true);
        return panel;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int targetWidth = Math.round((scale / 100f) * image.getWidth());
        int targetHeight = Math.round((scale / 100f) * image.getHeight());
        int x = getWidth() / 2 - targetWidth / 2;
        int y = 20;
        
        Graphics2D g2 = Utils2D.createGraphics(g, true, true);
        g2.drawImage(getPhotoToDraw(targetWidth, targetHeight), x, y, null);
    }

    private BufferedImage getPhotoToDraw(int targetWidth, int targetHeight) {
        BufferedImage scaledPhoto = Utils2D.scaleImage(image, targetWidth, targetHeight, true);

        if (blur > 0) {
            return Utils2D.applyGaussianBlur(scaledPhoto, blur);
        } else if (tintIntensity > 0) {
            return Utils2D.applyTint(scaledPhoto, tintColor);
        } else if (shadowSize > 0) {
            Color shadowColor = new Color(0, 0, 0, shadowAlpha);
            return Utils2D.applyDropShadow(scaledPhoto, shadowColor, shadowSize, shadowBlur);
        } else {
            return scaledPhoto;
        }
    }
}
