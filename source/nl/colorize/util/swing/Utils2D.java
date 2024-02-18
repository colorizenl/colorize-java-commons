//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import nl.colorize.util.ResourceException;
import nl.colorize.util.ResourceFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
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

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;

/**
 * Utility methods for Java 2D, mainly focused on image manipulation. Some of
 * the graphical effects provided by this class are based on the (brilliantly
 * titled) book Filthy Rich Clients.
 */
public final class Utils2D {
    
    private static final Splitter NEWLINE_SPLITTER = Splitter.on(CharMatcher.is('\n')).trimResults();
    private static final float LINE_SPACING_FACTOR = 1.8f;

    private Utils2D() {
    }
    
    public static BufferedImage loadImage(InputStream input) throws IOException {
        try (input) {
            return ImageIO.read(input);
        }
    }
    
    public static BufferedImage loadImage(File file) throws IOException {
        return loadImage(new FileInputStream(file));
    }
    
    public static BufferedImage loadImage(ResourceFile file) {
        try {
            return loadImage(file.openStream());
        } catch (IOException e) {
            throw new ResourceException("Cannot load image from " + file, e);
        }
    }

    public static void savePNG(BufferedImage image, OutputStream output) throws IOException {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            image = convertImage(image, BufferedImage.TYPE_INT_ARGB);
        }
        
        try (output) {
            ImageIO.write(image, "png", output);
        }
    }
    
    public static void savePNG(BufferedImage image, File dest) throws IOException {
        FileOutputStream stream = new FileOutputStream(dest);
        savePNG(image, stream);
    }
    
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
    
    public static void saveJPEG(BufferedImage image, File dest) throws IOException {
        FileOutputStream stream = new FileOutputStream(dest);
        saveJPEG(image, stream);
    }

    /**
     * Returns a <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URLs">
     * data URL</a> that represents the specified image's contents in PNG format.
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
     * Parses a <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URLs">
     * data URL</a> and returns the resulting image.
     *
     * @throws IOException if the data URL is invalid or uses an image format
     *         that is not supported by Java2D.
     */
    public static BufferedImage fromDataURL(String dataURL) throws IOException {
        byte[] imageData = Base64.getDecoder().decode(dataURL.substring(dataURL.indexOf(",") + 1));

        try (InputStream stream = new ByteArrayInputStream(imageData)) {
            return loadImage(stream);
        }
    }

    private static BufferedImage convertImage(BufferedImage image, int type) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), type);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return result;
    }

    /**
     * Returns the {@link GraphicsConfiguration} used by the default screen.
     * Note this may not be the screen that is displaying the application.
     */
    private static GraphicsConfiguration getDefaultGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration();
    }

    /**
     * Converts a {@code BufferedImage} to the pixel format and color model
     * preferred by the graphics environment.
     */
    public static BufferedImage makeImageCompatible(BufferedImage original) {
        BufferedImage compatibleImage = getDefaultGraphicsConfiguration().createCompatibleImage(
            original.getWidth(), original.getHeight(), Transparency.TRANSLUCENT);
        Graphics2D g2 = compatibleImage.createGraphics();
        g2.drawImage(original, 0, 0, null);
        g2.dispose();
        return compatibleImage;
    }
    
    /**
     * Returns a new image that contains the same contents as the original,
     * but is scaled to the specified width and height.
     * <p>
     * If the {@code highQuality} parameter is set to true, this method will
     * use a scaling algorithm that improves visual quality at the cost of
     * performance. The difference in visual quality will be more pronounced
     * if the original image is significantly larger or smaller than the
     * target size.
     */
    public static BufferedImage scaleImage(Image original, int width, int height, boolean highQuality) {
        Image current = original;
        int currentWidth = current.getWidth(null);
        int currentHeight = current.getHeight(null);

        while (highQuality && (currentWidth >= width * 2 || currentHeight >= height * 2)) {
            currentWidth = currentWidth / 2;
            currentHeight = currentHeight / 2;
            current = scaleImage(current, currentWidth, currentHeight);
        }

        return scaleImage(current, width, height);
    }

    private static BufferedImage scaleImage(Image original, int width, int height) {
        Preconditions.checkArgument(width >= 1, "Invalid width: " + width);
        Preconditions.checkArgument(height >= 1, "Invalid height: " + height);

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(result, true, true);
        g2.drawImage(original, 0, 0, width, height, null);
        g2.dispose();
        return result;
    }

    /**
     * Returns a new image that contains the original in the center, but is
     * surrounded with empty padding until the target width and height are
     * reached.
     */
    private static BufferedImage addPadding(BufferedImage original, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(result, true, false);
        g2.drawImage(original, width / 2 - original.getWidth() / 2,
            height / 2 - original.getHeight() / 2, null);
        g2.dispose();
        return result;
    }

    /**
     * Returns a new image that contains the original in the center, but is
     * surrounded by the specified amount of empty padding.
     */
    public static BufferedImage addPadding(BufferedImage original, int padding) {
        int targetWidth = original.getWidth() + padding * 2;
        int targetHeight = original.getHeight() + padding * 2;
        return addPadding(original, targetWidth, targetHeight);
    }

    /**
     * Convenience method that casts an AWT {@link Graphics} instance to
     * {@link Graphics2D}, then configures its rendering hints based on
     * the provided parameters.
     *
     * @param antialias Enables anti-aliasing for both graphics and text
     *                  rendering.
     * @param bilinear Enables bilinear filtering when scaling graphics. When
     *                 false, nearest neighbor scaling will be used.
     */
    public static Graphics2D createGraphics(Graphics g, boolean antialias, boolean bilinear) {
        Graphics2D g2 = (Graphics2D) g;
        if (antialias) {
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
        }
        if (bilinear) {
            g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        }
        return g2;
    }

    /**
     * Returns a {@link Graphics2D} instance for the specified image's graphics,
     * configuring its rendering hints based on the provided parameters.
     *
     * @param antialias Enables anti-aliasing for both graphics and text
     *                  rendering.
     * @param bilinear Enables bilinear filtering when scaling graphics. When
     *                 false, nearest neighbor scaling will be used.
     */
    public static Graphics2D createGraphics(BufferedImage image, boolean antialias, boolean bilinear) {
        return createGraphics(image.createGraphics(), antialias, bilinear);
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
     *
     * @param width The maximum width of the text rectangle.
     *
     * @return The height of the text rectangle.
     */
    public static int drawMultiLineString(Graphics2D g2, String text, int x, int startY, int width) {
        FontRenderContext renderContext = g2.getFontRenderContext();
        float currentY = startY;
        
        for (String line : NEWLINE_SPLITTER.split(text)) {
            // Simulate paragraphs for empty lines.
            if (line.isEmpty()) {
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
     * Returns a color with the same RGB values as {@code color}, but with a
     * different alpha value. The provided alpha value is expected to be in
     * the range 0 - 255.
     */
    public static Color withAlpha(Color color, int alpha) {
        Preconditions.checkArgument(alpha >= 0 && alpha <= 255, "Invalid alpha: " + alpha);

        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Returns a color with the same RGB values as {@code color}, but with a
     * different alpha value. The provided alpha value is expected to be in
     * the range 0.0 - 1.0.
     */
    public static Color withAlpha(Color color, float alpha) {
        return withAlpha(color, Math.round(alpha * 255f));
    }

    /**
     * Returns a color with the same RGB values as {@code hexColor}, but with a
     * different alpha value. The provided alpha value is expected to be in
     * the range 0 - 255.
     */
    public static Color withAlpha(String hexColor, int alpha) {
        return withAlpha(parseHexColor(hexColor), alpha);
    }

    /**
     * Returns a color with the same RGB values as {@code hexColor}, but with a
     * different alpha value. The provided alpha value is expected to be in
     * the range 0.0 - 1.0.
     */
    public static Color withAlpha(String hexColor, float alpha) {
        return withAlpha(hexColor, Math.round(alpha * 255f));
    }

    /**
     * Converts a {@link java.awt.Color} object to a hex string. For example, the
     * color red will produce "#FF0000".
     */
    public static String toHexColor(Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2, 8).toUpperCase();
    }

    /**
     * Converts a hex string to a {@link Color}. For example, the string
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
     *
     * @param delta Number between 0 and 1, where 0.0 indicates the "from" color
     *              and 1.0 indicates the "to" color.
     */
    public static Color interpolateColor(Color from, Color to, float delta) {
        delta = Math.clamp(delta, 0f, 1f);

        int red = Math.round(from.getRed() + delta * (to.getRed() - from.getRed()));
        int green = Math.round(from.getGreen() + delta * (to.getGreen() - from.getGreen()));
        int blue = Math.round(from.getBlue() + delta * (to.getBlue() - from.getBlue()));
        int alpha = Math.round(from.getAlpha() + delta * (to.getAlpha() - from.getAlpha()));
        
        return new Color(red, green, blue, alpha);
    }

    /**
     * Applies a Gaussian blur filter to the specified image, and returns the
     * result as a new image.
     *
     * @param amount The amount of blur, in pixels.
     */
    public static BufferedImage applyGaussianBlur(BufferedImage original, int amount) {
        Preconditions.checkArgument(amount >= 1, "Invalid amount of blur: " + amount);

        int size = amount * 2 + 1;
        float[] data = calculateGaussianBlurData(amount, size);
        ConvolveOp horizontal = new ConvolveOp(new Kernel(size, 1, data), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, size, data), ConvolveOp.EDGE_NO_OP, null);
        
        BufferedImage blurredImage = new BufferedImage(original.getWidth(), original.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        blurredImage = horizontal.filter(original, blurredImage);
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
     * Applies a drop shadow filter to the specified image, and returns the
     * result as a new image. The shadow will be cast from the non-transparent
     * contents from the orginal image. Because the shadow will also take up
     * some space, the image should ideally contain some empty padding to
     * accommodate for the shadow. {@link #addPadding(BufferedImage, int)} can
     * be used to add additional padding, if necessary.
     *
     * @param color Drop shadow color.
     * @param size The offset between the shadow and the original image.
     * @param blur Amount of blur to apply to the shadow.
     */
    public static BufferedImage applyDropShadow(BufferedImage image, Color color, int size, int blur) {
        BufferedImage shadow = new BufferedImage(image.getWidth(), image.getHeight(), 
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D shadowG2 = createGraphics(shadow, true, true);
        shadowG2.drawImage(image, size, size, null);
        shadowG2.setComposite(AlphaComposite.SrcIn);
        shadowG2.setColor(color);
        shadowG2.fillRect(0, 0, shadow.getWidth(), shadow.getHeight());
        shadowG2.dispose();
        
        BufferedImage combined = applyGaussianBlur(shadow, blur);
        Graphics2D combinedG2 = createGraphics(combined, true, true);
        combinedG2.drawImage(image, 0, 0, null);
        combinedG2.dispose();
        return combined;
    }

    /**
     * Returns an image that consists of a circle using the specified color.
     * This image is intended to be used for testing purposes.
     */
    @VisibleForTesting
    public static BufferedImage createTestImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createGraphics(image, true, true);
        g2.setColor(color);
        g2.fillOval(0, 0, width - 1, height - 1);
        g2.dispose();
        return image;
    }

    /**
     * Returns an image that consists of a red circle with the specified width
     * and height. This image is intended to be used for testing purposes.
     */
    @VisibleForTesting
    public static BufferedImage createTestImage(int width, int height) {
        return createTestImage(width, height, Color.RED);
    }
}
