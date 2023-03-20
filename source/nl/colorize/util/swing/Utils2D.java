//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import nl.colorize.util.ResourceFile;
import nl.colorize.util.ResourceException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Base64;

/**
 * Utility methods for Java 2D, mainly focused on image manipulation. Some of
 * the graphical effects are based on the (brilliantly titled) book Filthy Rich
 * Clients.
 */
public final class Utils2D {
    
    private static final Splitter NEWLINE_SPLITTER = Splitter.on(CharMatcher.is('\n')).trimResults();
    private static final float LINE_SPACING_FACTOR = 1.8f;

    private Utils2D() {
    }
    
    /**
     * Loads an image from a stream. The image can be of any type supported by
     * ImageIO. The stream is closed afterwards.
     *
     * @throws IOException if the image could not be loaded from the stream.
     */
    public static BufferedImage loadImage(InputStream input) throws IOException {
        try (input) {
            return ImageIO.read(input);
        }
    }
    
    /**
     * Loads an image from a file. The image can be of any type supported by ImageIO.
     *
     * @throws IOException if an I/O error occurs while readin the file.
     */
    public static BufferedImage loadImage(File file) throws IOException {
        return loadImage(new FileInputStream(file));
    }
    
    /**
     * Loads an image from a resource file. The image can be of any type supported
     * by ImageIO.
     */
    public static BufferedImage loadImage(ResourceFile file) {
        try {
            return loadImage(file.openStream());
        } catch (IOException e) {
            throw new ResourceException("Cannot load image from " + file, e);
        }
    }
    
    /**
     * Converts an {@link Image} of an unknown type to a {@link BufferedImage}.
     */
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        } else {
            BufferedImage wrapper = new BufferedImage(image.getWidth(null), image.getHeight(null), 
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = createGraphics(wrapper, false, false);
            g2.drawImage(image, 0, 0, null);
            g2.dispose();
            return wrapper;
        }
    }
    
    /**
     * Encodes the specified image as a PNG and writes it to a stream. The stream
     * is closed afterwards,
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public static void savePNG(BufferedImage image, OutputStream output) throws IOException {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            image = convertImage(image, BufferedImage.TYPE_INT_ARGB);
        }
        
        try (output) {
            ImageIO.write(image, "png", output);
        }
    }
    
    /**
     * Encodes the specified image as a PNG and writes it to a file.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public static void savePNG(BufferedImage image, File dest) throws IOException {
        FileOutputStream stream = new FileOutputStream(dest);
        savePNG(image, stream);
    }
    
    /**
     * Encodes the specified image as a JPEG and writes it to a stream.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public static void saveJPEG(BufferedImage image, OutputStream output) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(1f);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            writer.dispose();
        }
    }
    
    /**
     * Encodes the specified image as a JPEG and writes it to a file.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public static void saveJPEG(BufferedImage image, File dest) throws IOException {
        FileOutputStream stream = new FileOutputStream(dest);
        saveJPEG(image, stream);
    }

    /**
     * Converts a {@code BufferedImage} to the specified image format. If the image
     * already has the correct type this method does nothing.
     * @param type Requested image format, for example {@code BufferedImage.TYPE_INT_ARGB}.
     */
    public static BufferedImage convertImage(BufferedImage image, int type) {
        if (image.getType() == type) {
            return image;
        } else {
            BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), type);
            Graphics2D g2 = converted.createGraphics();
            g2.drawImage(image, 0, 0, null);
            g2.dispose();
            return converted;
        }
    }
    
    /**
     * Converts a {@code BufferedImage} to the pixel format and color model preferred
     * by the graphics environment. If the image already has the correct type this
     * method does nothing.
     */
    public static BufferedImage makeImageCompatible(BufferedImage original) {
        GraphicsConfiguration graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration();
        
        if (graphicsConfig.getColorModel().equals(original.getColorModel())) {
            return original;
        } else {
            BufferedImage compatibleImage = graphicsConfig.createCompatibleImage(
                original.getWidth(), original.getHeight(), original.getTransparency());
            Graphics2D g2 = compatibleImage.createGraphics();
            g2.drawImage(original, 0, 0, null);
            g2.dispose();
            return compatibleImage;
        }
    }
    
    /**
     * Creates a new image containing the source image, padded with empty space
     * to meet the specified dimensions.
     */
    public static BufferedImage addPadding(BufferedImage sourceImage, int width, int height) {
        if (sourceImage.getWidth() == width && sourceImage.getHeight() == height) {
            return sourceImage;
        }
        
        BufferedImage padded = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(padded, true, false);
        g2.drawImage(sourceImage, width / 2 - sourceImage.getWidth() / 2, 
                height / 2 - sourceImage.getHeight() / 2, null);
        g2.dispose();
        return padded;
    }
    
    /**
     * Creates a new image containing the source image, applying the specified
     * amount of padding to the top/bottom/left/right.
     *
     * @throws IllegalArgumentException for negative amounts of padding.
     */
    public static BufferedImage addPadding(BufferedImage sourceImage, int padding) {
        if (padding < 0) {
            throw new IllegalArgumentException("Invalid padding: " + padding);
        }
        return addPadding(sourceImage, sourceImage.getWidth() + padding * 2, 
                sourceImage.getHeight() + padding * 2);
    }
    
    /**
     * Scales an image to the specified dimensions. The image's aspect ratio is
     * ignored, meaning the image will appear stretched or squashed if the
     * target width/height have a different aspect ratio.
     * @param highQuality Improves the quality of the scaled image, at the cost
     *        of performance. 
     */
    public static BufferedImage scaleImage(Image original, int targetWidth, int targetHeight,
            boolean highQuality) {
        if (highQuality) {
            original = original.getScaledInstance(targetWidth, targetHeight, BufferedImage.SCALE_SMOOTH);
        }
        
        BufferedImage rescaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(rescaled, true, true);
        g2.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2.dispose();
        return rescaled;
    }
    
    /**
     * Scales an image while maintaining its aspect ratio. This will prevent the 
     * image from looking stretched or squashed if the target width/height have a
     * different aspect ratio. 
     * @param highQuality Improves the quality of the scaled image, at the cost
     *        of performance.
     */
    public static BufferedImage scaleImageProportional(Image original, int targetWidth, 
            int targetHeight, boolean highQuality) {
        float originalAspectRatio = (float) original.getWidth(null) / (float) original.getHeight(null);
        float targetAspectRatio = (float) targetWidth / (float) targetHeight;
        
        int scaledWidth = targetWidth;
        int scaledHeight = targetHeight;
        if (originalAspectRatio < targetAspectRatio) {
            scaledWidth = Math.round(targetHeight * originalAspectRatio);
        } else {
            scaledHeight = Math.round(targetWidth * (1f / originalAspectRatio));
        }
        
        if (highQuality) {
            original = original.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_SMOOTH);
        }
        
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(scaled, true, true);
        g2.drawImage(original, targetWidth / 2 - scaledWidth / 2, targetHeight / 2 - scaledHeight / 2,
                scaledWidth, scaledHeight, null);
        g2.dispose();
        
        return scaled;
    }

    /**
     * Creates a data URL out of an existing image. The data URL will be based
     * on a PNG image.
     */
    public static String toDataURL(BufferedImage image) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Utils2D.savePNG(image, buffer);
            byte[] base64 = Base64.getEncoder().encode(buffer.toByteArray());
            return "data:image/png;base64," + new String(base64, Charsets.UTF_8);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Unable to convert image to PNG", e);
        }
    }

    /**
     * Creates an image from a data URL.
     *
     * @throws IOException if the data URL is corrupted or uses an image format
     *         that is not supported by Java2D.
     */
    public static BufferedImage fromDataURL(String dataURL) throws IOException {
        byte[] imageData = Base64.getDecoder().decode(dataURL.substring(dataURL.indexOf(",") + 1));

        try (InputStream stream = new ByteArrayInputStream(imageData)) {
            return loadImage(stream);
        }
    }

    /**
     * Saves an image that is represented by a data URL to a file.
     */
    public static void saveDataURL(String dataURL, File file) throws IOException {
        byte[] imageData = Base64.getDecoder().decode(dataURL.substring(dataURL.indexOf(",") + 1));
        Files.write(imageData, file);
    }
    
    public static Graphics2D createGraphics(Graphics g, boolean antialias, boolean bilinear) {
        Graphics2D g2 = (Graphics2D) g;
        if (antialias) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        if (bilinear) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        return g2;
    }
    
    public static Graphics2D createGraphics(BufferedImage image, boolean antialias, boolean bilinear) {
        return createGraphics(image.createGraphics(), antialias, bilinear);
    }
    
    public static void clearGraphics(Graphics2D g2, BufferedImage imageToClear) {
        Composite composite = g2.getComposite();
        Color color = g2.getColor();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 1f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, imageToClear.getWidth(), imageToClear.getHeight());
        g2.setComposite(composite);
        g2.setColor(color);
    }
    
    public static void drawStringCentered(Graphics2D g2, String text, int x, int y) {
        g2.drawString(text, x - g2.getFontMetrics().stringWidth(text) / 2, y);
    }
    
    public static void drawStringRight(Graphics2D g2, String text, int x, int y) {
        g2.drawString(text, x - g2.getFontMetrics().stringWidth(text), y);
    }
    
    /**
     * Draws a string that will word-wrap around a set width. Any line breaks
     * already in the string will be preserved.
     * @param width The maximum width of the text rectangle.
     * @return The height of the text rectangle.
     */
    public static int drawMultiLineString(Graphics2D g2, String text, int x, int startY, int width) {
        FontRenderContext renderContext = g2.getFontRenderContext();
        float currentY = startY;
        
        for (String line : NEWLINE_SPLITTER.split(text)) {
            // Simulate paragraphs for empty lines.
            if (line.length() == 0) {
                line = " ";
            }
            
            AttributedString paragraph = new AttributedString(line);
            paragraph.addAttribute(TextAttribute.FONT, g2.getFont());
            paragraph.addAttribute(TextAttribute.FOREGROUND, g2.getColor());
            
            AttributedCharacterIterator charIterator = paragraph.getIterator();
            LineBreakMeasurer lineBreakMeasurer = new LineBreakMeasurer(charIterator, renderContext);
            lineBreakMeasurer.setPosition(charIterator.getBeginIndex());
            
            while (lineBreakMeasurer.getPosition() < charIterator.getEndIndex()) {
                TextLayout layout = lineBreakMeasurer.nextLayout(width);
                currentY += layout.getAscent();
                layout.draw(g2, x, currentY);
                currentY += (layout.getDescent() + layout.getLeading()) * LINE_SPACING_FACTOR;
            }
        }
        
        return Math.round(currentY - startY);
    }

    /**
     * Convenience method for drawing a circle from its center, rather than from
     * its top left corner.
     */
    public static void drawCircle(Graphics2D g2, int centerX, int centerY, int radius) {
        g2.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
    
    /**
     * Creates a new image by applying a color tint to an existing image. All 
     * pixels in the image will retain their existing alpha value, but the 
     * pixel's RGB values will be replaced with the tint color.
     */
    public static BufferedImage applyTint(BufferedImage image, Color tint) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), 
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(result, true, true);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int alpha = (image.getRGB(x, y) >> 24) & 0xff;
                Color pixelColor = new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha);
                result.setRGB(x, y, pixelColor.getRGB());
            }
        }
        g2.dispose();
        return result;
    }
    
    /**
     * Returns a color with the same RGB value as {@code color} but different alpha.
     * @param alpha The new alpha value, between 0 and 255.
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Returns a color with the RGB value indicated by {@code hexColor} but a
     * different alpha.
     * @param alpha The new alpha value, between 0 and 255.
     */
    public static Color withAlpha(String hexColor, int alpha) {
        return withAlpha(parseHexColor(hexColor), alpha);
    }
    
    /**
     * Returns a color with the same RGB value as {@code color} but different alpha.
     * @param alpha The new alpha value, between 0 and 1.
     */
    public static Color withAlpha(Color color, float alpha) {
        return withAlpha(color, Math.round(alpha * 255f));
    }

    /**
     * Converts a {@link java.awt.Color} object to a hex string. For example, the
     * color red will produce "#FF0000".
     */
    public static String toHexColor(Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2, 8).toUpperCase();
    }

    /**
     * Converts a hex string to a {@link java.awt.Color}. For example, the string
     * "#FF0000" will produce red. 
     */
    public static Color parseHexColor(String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        return Color.decode(hex);
    }
    
    /**
     * Creates a new color that calculates its red/green/blue/alpha components by
     * interpolating between two other colors.
     * @param delta Number between 0 and 1, where 0 indicates the "from" color and
     *        1 indicates the "to" color.
     */
    public static Color interpolateColor(Color from, Color to, float delta) {
        delta = Math.max(delta, 0f);
        delta = Math.min(delta, 1f);
        
        int red = Math.round(from.getRed() + delta * (to.getRed() - from.getRed()));
        int green = Math.round(from.getGreen() + delta * (to.getGreen() - from.getGreen()));
        int blue = Math.round(from.getBlue() + delta * (to.getBlue() - from.getBlue()));
        int alpha = Math.round(from.getAlpha() + delta * (to.getAlpha() - from.getAlpha()));
        
        return new Color(red, green, blue, alpha);
    }

    public static Stroke createDashedStroke(float weight, int dashSize) {
        return new BasicStroke(weight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            0, new float[] {dashSize}, 0);
    }
    
    /**
     * Creates a new image by applying gaussian blur to an existing image.
     * @param amount The amount of blur, in pixels.
     *
     * @throws IllegalArgumentException for negative amounts of blur.
     */
    public static BufferedImage applyGaussianBlur(BufferedImage image, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Invalid amount of blur: " + amount);
        }
        
        if (amount == 0) {
            return image;
        }
        
        int size = amount * 2 + 1;
        float[] data = calculateGaussianBlurData(amount, size);
        ConvolveOp horizontal = new ConvolveOp(new Kernel(size, 1, data), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, size, data), ConvolveOp.EDGE_NO_OP, null);
        
        BufferedImage blurredImage = new BufferedImage(image.getWidth(), image.getHeight(), 
                BufferedImage.TYPE_INT_ARGB);
        blurredImage = horizontal.filter(image, blurredImage);
        blurredImage = vertical.filter(blurredImage, null);
        return blurredImage;
    }
    
    private static float[] calculateGaussianBlurData(int amount, int size) {
        float[] data = new float[size];
        float sigma = amount / 3f;
        float sigmaTwoSquared = 2f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(sigmaTwoSquared * Math.PI);
        float total = 0f;
        
        for (int i = -amount; i <= amount; i++) {
            float distance = i * i;
            int index = i + amount;
            data[index] = (float) Math.exp(-distance / (sigmaTwoSquared) / sigmaRoot);
            total += data[index];
        }
        
        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }
        
        return data;
    }
    
    /**
     * Creates a new image by applying a drop shadow to an existing image. The
     * shadow will be cast from the non-transparent contents from the orginal
     * image. Because the shadow will also take up some space the image needs 
     * to contain some empty padding, to accommodate for the shadow. 
     * @param shadowSize The offset between the shadow and the original image.
     * @param blur Amount of blur to apply to the shadow.
     */
    public static BufferedImage applyDropShadow(BufferedImage image, int shadowSize, int blur) {
        BufferedImage shadow = new BufferedImage(image.getWidth(), image.getHeight(), 
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(shadow, true, false);
        g2.drawImage(image, shadowSize, shadowSize, null);
        g2.setComposite(AlphaComposite.SrcIn);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, shadow.getWidth(), shadow.getHeight());
        g2.dispose();
        
        BufferedImage combined = applyGaussianBlur(shadow, blur);
        g2 = createGraphics(combined, true, false);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return combined;
    }

    @VisibleForTesting
    public static BufferedImage createTestImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(image, true, true);
        g2.setColor(color);
        g2.fillOval(0, 0, width - 1, height - 1);
        g2.dispose();
        return image;
    }

    @VisibleForTesting
    public static BufferedImage createTestImage(int width, int height) {
        return createTestImage(width, height, Color.RED);
    }
}
