//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

import nl.colorize.util.LogHelper;
import nl.colorize.util.Tuple;
import nl.colorize.util.swing.Utils2D;

/**
 * Resizes an image to a number of different output formats in one step.
 */
public class ResizeImageTool {
    
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)");
    private static final Logger LOGGER = LogHelper.getLogger(ResizeImageTool.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            LOGGER.info("Usage: ResizeImageTool <image> <outputSizes> <background | none> <outputDir>");
            System.exit(1);
        }
        
        ResizeImageTool tool = new ResizeImageTool();
        tool.run(new File(args[0]), args[1], args[2], new File(args[3]));
    }
    
    private void run(File sourceFile, String sizes, String background, File outputDir) throws IOException {
        BufferedImage sourceImage = Utils2D.loadImage(sourceFile);
        Color backgroundColor = parseColor(background);
        
        for (Tuple<Integer, Integer> size : parseSizes(sizes)) {
            File outputFile = new File(outputDir, sourceFile.getName().replace(".png", "") + 
                    "-" + size.getLeft() + "x" + size.getRight() + ".png");
            
            resizeImage(sourceImage, size, backgroundColor, outputFile);
        }
    }

    private void resizeImage(BufferedImage sourceImage, Tuple<Integer, Integer> size,
            Color backgroundColor, File outputFile) throws IOException {
        BufferedImage scaled = Utils2D.scaleImage(sourceImage, size.getLeft(), size.getRight(), true);
            
        BufferedImage result = new BufferedImage(scaled.getWidth(), scaled.getHeight(), 
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(result, true, true);
        if (backgroundColor != null) {
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, result.getWidth(), result.getHeight());
        }
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        
        Utils2D.savePNG(result, outputFile);
    }

    private List<Tuple<Integer, Integer>> parseSizes(String sizes) {
        List<Tuple<Integer, Integer>> parsed = new ArrayList<>();
        for (String size : Splitter.on(",").split(sizes)) {
            Matcher matcher = SIZE_PATTERN.matcher(size);
            if (matcher.matches()) {
                int width = Integer.parseInt(matcher.group(1));
                int height = Integer.parseInt(matcher.group(2));
                parsed.add(Tuple.of(width, height));
            } else {
                throw new IllegalArgumentException("Malformed size: " + size);
            }
        }
        return parsed;
    }
    
    private Color parseColor(String color) {
        if (color == null || color.isEmpty() || color.equals("none")) {
            return null;
        }
        return Utils2D.parseHexColor(color);
    }
}
