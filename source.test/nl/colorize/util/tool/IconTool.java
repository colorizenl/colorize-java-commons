//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

import nl.colorize.util.CommandRunner;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.swing.Utils2D;

/**
 * Command line tool that creates an ICNS icon file from a single image. The
 * image is automatically rescaled to all required size variants. 
 */
public class IconTool {

	private static final List<Integer> SIZE_VARIANTS = ImmutableList.of(16, 32, 128, 256, 512); 
	private static final Logger LOGGER = LogHelper.getLogger(IconTool.class);
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			LOGGER.info("Usage: IconTool [image] [outputFile]");
			System.exit(1);
		}
		
		File sourceImageFile = new File(args[0]);
		File outputFile = new File(args[1]);
		
		if (outputFile.exists()) {
			throw new IOException("Output file already exists: " + outputFile.getAbsolutePath());
		}
		
		IconTool tool = new IconTool();
		tool.createICNS(sourceImageFile, outputFile);
	}

	private void createICNS(File sourceImageFile, File outputFile) throws IOException {
		BufferedImage sourceImage = loadImage(sourceImageFile);
		Map<String, BufferedImage> iconSet = createIconSet(sourceImage);
		File iconSetDir = new File(LoadUtils.createTempDir(), "icon.iconset");
		saveIconSet(iconSet, iconSetDir);
		LOGGER.info("Created icon set at " + iconSetDir.getAbsolutePath());
		convertIconSetToICNS(iconSetDir, outputFile);
		LOGGER.info("Done, wrote ICNS icon to " + outputFile.getAbsolutePath());
	}

	private BufferedImage loadImage(File sourceImageFile) {
		try {
			BufferedImage image = Utils2D.loadImage(sourceImageFile);
			if (image.getWidth() != image.getHeight()) {
				throw new RuntimeException("Image must be square to be used as icon");
			}
			return image;
		} catch (IOException e) {
			throw new RuntimeException("Cannot load image: " + sourceImageFile.getAbsolutePath());
		}
	}
	
	private Map<String, BufferedImage> createIconSet(BufferedImage sourceImage) {
		Map<String, BufferedImage> iconSet = new LinkedHashMap<String, BufferedImage>();
		for (int variant : SIZE_VARIANTS) {
			iconSet.put("icon_" + variant + "x" + variant + ".png", 
					scaleIconImage(sourceImage, variant));
			iconSet.put("icon_" + variant + "x" + variant + "@2x.png", 
					scaleIconImage(sourceImage, 2 * variant));
		}
		return iconSet;
	}
	
	private BufferedImage scaleIconImage(BufferedImage sourceImage, int size) {
		if (sourceImage.getWidth() == size && sourceImage.getHeight() == size) {
			return sourceImage;
		} else {
			return Utils2D.scaleImage(sourceImage, size, size);
		}
	}

	private void saveIconSet(Map<String, BufferedImage> iconSet, File outputDir) throws IOException {
		if (!outputDir.exists()) {
			outputDir.mkdir();
		}
		
		for (Map.Entry<String, BufferedImage> entry : iconSet.entrySet()) {
			File imageFile = new File(outputDir, entry.getKey());
			LOGGER.info("Creating icon " + imageFile.getAbsolutePath());
			Utils2D.savePNG(entry.getValue(), imageFile);
		}
	}
	
	private void convertIconSetToICNS(File iconSetDir, File icnsFile) throws IOException {
		CommandRunner commandRunner = new CommandRunner("iconutil", "-c", "icns", 
				iconSetDir.getAbsolutePath(), "-o", icnsFile.getAbsolutePath());
		commandRunner.setShellMode(true);
		commandRunner.setLoggingEnabled(true);
		try {
			commandRunner.execute();
		} catch (TimeoutException e) {
			throw new IOException("iconutil timeout");
		}
	}
}
