//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Files;

import nl.colorize.util.DirectoryWalker;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.swing.Utils2D;

/**
 * Creates a CSS sprite sheet from images in a directory. The names of the
 * generated CSS classes are based on the names of the images files (without
 * the file extension). 
 */
public class CSSSpriteSheetTool {
	
	private static final String DEFAULT_IMAGE_PATH = "../images/";
	private static final int MARGIN = 2;
	private static final Logger LOGGER = LogHelper.getLogger(CSSSpriteSheetTool.class);

	public static void main(String[] args) {
		if (args.length != 4) {
			LOGGER.info("Usage: CSSSpriteSheetTool <image dir> <output image> <output css> <width>");
			System.exit(1);
		}
		
		File imageDir = new File(args[0]);
		File outputImage = new File(args[1]);
		File outputCSS = new File(args[2]);
		int width = Integer.parseInt(args[3]);
		
		CSSSpriteSheetTool tool = new CSSSpriteSheetTool();
		List<Image> images = tool.gatherImages(imageDir);
		LOGGER.info("Generating sprite sheet from " + images.size() + " images");
		tool.createSpriteSheet(images, outputImage, outputCSS, width);
		LOGGER.info("Done");
	}

	private List<Image> gatherImages(File imageDir) {
		DirectoryWalker dirWalker = new DirectoryWalker();
		dirWalker.setRecursive(true);
		dirWalker.setVisitHiddenFiles(false);
		dirWalker.setIncludeSubdirectories(false);
		dirWalker.setFileFilter(LoadUtils.getFileExtensionFilter("jpg", "png"));
		
		List<Image> images = new ArrayList<>();
		for (File imageFile : dirWalker.walk(imageDir)) {
			Optional<Image> image = parseImage(imageFile);
			if (image.isPresent()) {
				images.add(image.get());
			}
		}
		return images;
	}
	
	private Optional<Image> parseImage(File imageFile) {
		try {
			BufferedImage image = Utils2D.loadImage(imageFile);
			return Optional.of(new Image(imageFile, image));
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unusable image: " + imageFile.getName(), e);
			return Optional.absent();
		}
	}
	
	private void createSpriteSheet(List<Image> images, File outputImage, File outputCSS, int width) {
		Map<Image, Rectangle> imageBounds = calculateImageBounds(images, width);
		int height = calculateHeight(imageBounds);
		
		try {
			BufferedImage spriteSheetImage = createSpriteSheetImage(imageBounds, width, height);
			Utils2D.savePNG(spriteSheetImage, outputImage);
			
			String css = generateCSS(imageBounds, DEFAULT_IMAGE_PATH + outputImage.getName());
			Files.write(css, outputCSS, Charsets.UTF_8);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Cannot create CSS sprite sheet", e);
		}
	}

	private Map<Image, Rectangle> calculateImageBounds(List<Image> images, int width) {
		Map<Image, Rectangle> imageBounds = new LinkedHashMap<>();
		int x = MARGIN;
		int y = MARGIN;
		int rowHeight = 0;
		
		for (Image image : images) {
			if (x + image.getWidth() >= width) {
				x = MARGIN;
				y += rowHeight + MARGIN;
				rowHeight = 0;
			}
			
			imageBounds.put(image, new Rectangle(x, y, image.getWidth(), image.getHeight()));
			rowHeight = Math.max(rowHeight, image.getHeight());
			x += image.getWidth() + MARGIN;
		}
		
		return imageBounds;
	}
	
	private int calculateHeight(Map<Image, Rectangle> imageBounds) {
		int height = 0;
		for (Rectangle bounds : imageBounds.values()) {
			height = Math.max(height, bounds.y + bounds.height);
		}
		return height;
	}
	
	private BufferedImage createSpriteSheetImage(Map<Image, Rectangle> imageBounds, int width, int height) {
		BufferedImage spriteSheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = Utils2D.createGraphics(spriteSheet, true, true);
		for (Map.Entry<Image, Rectangle> entry : imageBounds.entrySet()) {
			Rectangle bounds = entry.getValue();
			g2.drawImage(entry.getKey().image, bounds.x, bounds.y, bounds.width, bounds.height, null);
		}
		g2.dispose();
		return spriteSheet;
	}
	
	private String generateCSS(Map<Image, Rectangle> imageBounds, String spriteSheetImagePath) {
		StringBuilder css = new StringBuilder();
		css.append("# Generated CSS sprite sheet\n\n");
		for (Map.Entry<Image, Rectangle> entry : imageBounds.entrySet()) {
			Rectangle bounds = entry.getValue();
			css.append(entry.getKey().getCssSelectorName() + " {\n");
			css.append("\twidth: " + bounds.width + "px;\n");
			css.append("\theight: " + bounds.height + "px;\n");
			css.append("\tbackground-image: url('" + spriteSheetImagePath + "');\n");
			css.append("\tbackground-position: " + bounds.x + "px " + bounds.y + "px;\n");
			css.append("}\n\n");
		}
		return css.toString();
	}

	/**
	 * Image that will be added to the CSS sprite sheet.
	 */
	private static class Image {
		
		private File sourceFile;
		private BufferedImage image;
		
		public Image(File sourceFile, BufferedImage image) {
			this.sourceFile = sourceFile;
			this.image = image;
		}
		
		public int getWidth() {
			return image.getWidth();
		}
		
		public int getHeight() {
			return image.getHeight();
		}
		
		public String getCssSelectorName() {
			String cssClassName = Files.getNameWithoutExtension(sourceFile.getName());
			cssClassName = cssClassName.toLowerCase().replace("-", "").replace("_", "");
			return "." + cssClassName;
		}
	}
}
