//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import nl.colorize.util.ResourceFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.awt.Color.RED;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Utils2DTest {

    private static final Color EMPTY = new Color(0, 0, 0, 0);
    
    @Test
    public void testLoadAndSaveImage() throws IOException {
        BufferedImage image = image(100, 80);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Utils2D.savePNG(image, buffer);
        BufferedImage reloaded = Utils2D.loadImage(new ByteArrayInputStream(buffer.toByteArray()));

        assertEquals(image.getWidth(), reloaded.getWidth());
        assertEquals(image.getHeight(), reloaded.getHeight());
    }
    
    @Test
    public void testSaveImagePNG(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "test.png");
        BufferedImage image = image(200, 200);
        Utils2D.savePNG(image, new FileOutputStream(tempFile));
        BufferedImage loaded = Utils2D.loadImage(new ResourceFile(tempFile));

        assertEquals(image.getWidth(), loaded.getWidth());
        assertEquals(image.getHeight(), loaded.getHeight());
    }
    
    @Test
    public void testSaveImageJPEG(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "test.jpg");
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_BGR);
        Utils2D.saveJPEG(image, new FileOutputStream(tempFile));
        BufferedImage loaded = Utils2D.loadImage(new ResourceFile(tempFile));

        assertEquals(image.getWidth(), loaded.getWidth());
        assertEquals(image.getHeight(), loaded.getHeight());
    }

    @Test
    public void testImageTransparency(@TempDir File tempDir) throws IOException {
        BufferedImage original = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = original.createGraphics();
        g2.setColor(RED);
        g2.fillOval(0, 0, 200, 200);
        g2.dispose();
        
        File pngFile = new File(tempDir, "test.png");
        Utils2D.savePNG(original, pngFile);
        
        BufferedImage png = Utils2D.loadImage(new FileInputStream(pngFile));
        assertEquals(Transparency.TRANSLUCENT, png.getTransparency());

        original = new BufferedImage(200, 200, BufferedImage.TYPE_INT_BGR);
        g2 = original.createGraphics();
        g2.setColor(RED);
        g2.fillRect(0, 0, 200, 200);
        g2.dispose();
        
        File jpgFile = new File(tempDir, "test.jpg");
        Utils2D.saveJPEG(original, jpgFile);
        
        BufferedImage jpg = Utils2D.loadImage(new FileInputStream(jpgFile));
        assertEquals(Transparency.OPAQUE, jpg.getTransparency());
    }
    
    @Test
    public void testScaleImage() {
        BufferedImage result = Utils2D.scaleImage(image(100, 100), 60, 40, true);
        assertEquals(60, result.getWidth());
        assertEquals(40, result.getHeight());
    }
    
    @Test
    public void testWithAlpha() {
        assertEquals(new Color(255, 0, 0, 128), Utils2D.withAlpha(RED, 128));
        assertEquals(new Color(255, 0, 0, 128), Utils2D.withAlpha(RED, 0.5f));
        assertEquals(new Color(255, 0, 0, 128), Utils2D.withAlpha("#FF0000", 128));
        assertEquals(new Color(255, 0, 0, 128), Utils2D.withAlpha("#FF0000", 0.5f));
    }
    
    @Test
    public void testHexColor() {
        assertEquals("#FF0000", Utils2D.toHexColor(RED));
        assertEquals("#000000", Utils2D.toHexColor(new Color(0, 0, 0, 128)));
    }

    @Test
    void parseHexColor() {
        assertEquals(RED, Utils2D.parseHexColor("#FF0000"));
        assertEquals(Color.GREEN, Utils2D.parseHexColor("00FF00"));
    }

    @Test
    public void testInterpolateColor() {
        assertEquals(new Color(255, 0, 0), Utils2D.interpolateColor(RED, Color.ORANGE, 0f));
        assertEquals(new Color(255, 100, 0), Utils2D.interpolateColor(RED, Color.ORANGE, 0.5f));
        assertEquals(new Color(255, 150, 0), Utils2D.interpolateColor(RED, Color.ORANGE, 0.75f));
        assertEquals(new Color(255, 200, 0), Utils2D.interpolateColor(RED, Color.ORANGE, 1f));
        
        assertEquals(new Color(255, 255, 255), Utils2D.interpolateColor(Color.WHITE, Color.BLACK, 0f));
        assertEquals(new Color(128, 128, 128), Utils2D.interpolateColor(Color.WHITE, Color.BLACK, 0.5f));
        assertEquals(new Color(0, 0, 0), Utils2D.interpolateColor(Color.WHITE, Color.BLACK, 1f));
    }

    @Test
    void toDataURL() {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, RED.getRGB());
        image.setRGB(1, 0, Color.BLUE.getRGB());

        assertEquals("data:image/png;base64," +
            "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0k" +
            "AAAAEElEQVR4XmP4z8AAQkAMBQAz3gP9NSox0gAAAABJRU5ErkJggg==",
            Utils2D.toDataURL(image));
    }

    @Test
    void fromDataURL() throws IOException {
        String base64 = "data:image/png;base64," +
            "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0k" +
            "AAAAEElEQVR4XmP4z8AAQkAMBQAz3gP9NSox0gAAAABJRU5ErkJggg==";

        BufferedImage image = Utils2D.fromDataURL(base64);

        assertEquals(RED.getRGB(), image.getRGB(0, 0));
        assertEquals(Color.BLUE.getRGB(), image.getRGB(1, 0));
        assertEquals(0, image.getRGB(0, 1));
        assertEquals(0, image.getRGB(1, 1));
    }

    @Test
    void addPadding() {
        BufferedImage original = Utils2D.createTestImage(100, 100);
        BufferedImage padded = Utils2D.addPadding(original, 50);

        assertEquals(200, padded.getWidth());
        assertEquals(200, padded.getHeight());
        assertEquals(EMPTY.getRGB(), padded.getRGB(0, 100));
        assertEquals(RED.getRGB(), padded.getRGB(100, 100));
    }

    @Test
    void scaleImage() {
        BufferedImage original = Utils2D.createTestImage(100, 100);
        BufferedImage scaled = Utils2D.scaleImage(original, 20, 20, false);

        assertEquals(20, scaled.getWidth());
        assertEquals(20, scaled.getHeight());
        assertEquals(EMPTY.getRGB(), scaled.getRGB(0, 0));
        assertEquals(RED.getRGB(), scaled.getRGB(10, 10));
    }

    @Test
    void scaleImageHighQuality() {
        BufferedImage original = Utils2D.createTestImage(100, 100);
        BufferedImage scaled = Utils2D.scaleImage(original, 20, 20, true);

        assertEquals(20, scaled.getWidth());
        assertEquals(20, scaled.getHeight());
        assertEquals(EMPTY.getRGB(), scaled.getRGB(0, 0));
        assertEquals(RED.getRGB(), scaled.getRGB(10, 10));
    }

    @Test
    void gaussianBlur() {
        BufferedImage original = Utils2D.createTestImage(100, 100);
        BufferedImage blur = Utils2D.applyGaussianBlur(original, 10);

        assertEquals(100, blur.getWidth());
        assertEquals(100, blur.getHeight());
    }

    @Test
    void dropShadow() {
        BufferedImage original = Utils2D.createTestImage(100, 100);
        BufferedImage shadow = Utils2D.applyDropShadow(original, Color.BLACK, 2, 4);

        assertEquals(100, shadow.getWidth());
        assertEquals(100, shadow.getHeight());
    }

    private BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
}
