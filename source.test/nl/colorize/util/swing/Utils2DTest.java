//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

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

import nl.colorize.util.LoadUtils;
import nl.colorize.util.ResourceFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code Utils2D} class.
 */
@RunWith(HeadlessTestRunner.class)
public class Utils2DTest {
    
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
    public void testSaveImagePNG() throws IOException {
        File tempFile = LoadUtils.getTempFile(".png");
        BufferedImage image = image(200, 200);
        Utils2D.savePNG(image, new FileOutputStream(tempFile));
        BufferedImage loaded = Utils2D.loadImage(new ResourceFile(tempFile));
        assertEquals(image.getWidth(), loaded.getWidth());
        assertEquals(image.getHeight(), loaded.getHeight());
    }
    
    @Test
    public void testSaveImageJPEG() throws IOException {
        File tempFile = LoadUtils.getTempFile(".jpg");
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_BGR);
        Utils2D.saveJPEG(image, new FileOutputStream(tempFile), 1f);
        BufferedImage loaded = Utils2D.loadImage(new ResourceFile(tempFile));
        assertEquals(image.getWidth(), loaded.getWidth());
        assertEquals(image.getHeight(), loaded.getHeight());
    }
    
    @Test
    public void testMakeImageCompatible() {
        BufferedImage image = new BufferedImage(200, 300, BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImage compatible = Utils2D.makeImageCompatible(image);
        assertEquals(200, compatible.getWidth());
        assertEquals(300, compatible.getHeight());
        
        Utils2D.makeImageCompatible(new BufferedImage(200, 300, BufferedImage.TYPE_3BYTE_BGR));
    }
    
    @Test
    public void testImageTransparency() throws IOException {
        BufferedImage original = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = original.createGraphics();
        g2.setColor(Color.RED);
        g2.fillOval(0, 0, 200, 200);
        g2.dispose();
        
        File pngFile = LoadUtils.getTempFile(".png");
        Utils2D.savePNG(original, pngFile);
        
        BufferedImage png = Utils2D.loadImage(new FileInputStream(pngFile));
        assertEquals(Transparency.TRANSLUCENT, png.getTransparency());
        assertEquals(Transparency.TRANSLUCENT, Utils2D.makeImageCompatible(png).getTransparency());
        
        original = new BufferedImage(200, 200, BufferedImage.TYPE_INT_BGR);
        g2 = original.createGraphics();
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 200, 200);
        g2.dispose();
        
        File jpgFile = LoadUtils.getTempFile(".jpg");
        Utils2D.saveJPEG(original, jpgFile, 1f);
        
        BufferedImage jpg = Utils2D.loadImage(new FileInputStream(jpgFile));
        assertEquals(Transparency.OPAQUE, jpg.getTransparency());
        assertEquals(Transparency.OPAQUE, Utils2D.makeImageCompatible(jpg).getTransparency());
    }
    
    @Test
    public void testScaleImage() {
        BufferedImage result = Utils2D.scaleImage(image(100, 100), 60, 40, true);
        assertEquals(60, result.getWidth());
        assertEquals(40, result.getHeight());
    }
    
    @Test
    public void testWithAlpha() {
        assertEquals(new Color(255, 0, 0, 128), Utils2D.withAlpha(Color.RED, 128));
        assertEquals(new Color(255, 0, 0, 0), Utils2D.withAlpha(new Color(255, 0, 0, 128), 0));
    }
    
    @Test
    public void testHexColor() {
        assertEquals("#FF0000", Utils2D.toHexColor(Color.RED));
        assertEquals("#000000", Utils2D.toHexColor(new Color(0, 0, 0, 128)));
        
        assertEquals(Color.RED, Utils2D.parseHexColor("#FF0000"));
        assertEquals(Color.GREEN, Utils2D.parseHexColor("00FF00"));
    }
    
    @Test
    public void testClearGraphics() {
        BufferedImage image = Utils2D.createTestImage(100, 100, Color.RED);
        assertEquals(Color.RED.getRGB(), image.getRGB(50, 50));
        
        Graphics2D g2 = image.createGraphics();
        Utils2D.clearGraphics(g2, image);
        g2.dispose();
        assertEquals(new Color(0, 0, 0, 0).getRGB(), image.getRGB(50, 50));
    }
    
    @Test
    public void testScaleImageProportional() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(image, false, false);
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 100, 100);
        g2.dispose();
        
        BufferedImage scaled = Utils2D.scaleImageProportional(image, 100, 150, true);
        
        assertEquals(new Color(0, 0, 0, 0).getRGB(), scaled.getRGB(50, 3));
        assertEquals(Color.RED.getRGB(), scaled.getRGB(50, 28));
        assertEquals(Color.RED.getRGB(), scaled.getRGB(50, 122));
        assertEquals(new Color(0, 0, 0, 0).getRGB(), scaled.getRGB(50, 147));
    }
    
    @Test
    public void testInterpolateColor() {
        assertEquals(new Color(255, 0, 0), Utils2D.interpolateColor(Color.RED, Color.ORANGE, 0f));
        assertEquals(new Color(255, 100, 0), Utils2D.interpolateColor(Color.RED, Color.ORANGE, 0.5f));
        assertEquals(new Color(255, 150, 0), Utils2D.interpolateColor(Color.RED, Color.ORANGE, 0.75f));
        assertEquals(new Color(255, 200, 0), Utils2D.interpolateColor(Color.RED, Color.ORANGE, 1f));
        
        assertEquals(new Color(255, 255, 255), Utils2D.interpolateColor(Color.WHITE, Color.BLACK, 0f));
        assertEquals(new Color(128, 128, 128), Utils2D.interpolateColor(Color.WHITE, Color.BLACK, 0.5f));
        assertEquals(new Color(0, 0, 0), Utils2D.interpolateColor(Color.WHITE, Color.BLACK, 1f));
    }
    
    private BufferedImage image(int width, int height) {
        return Utils2D.createImage(width, height, true);
    }
}
