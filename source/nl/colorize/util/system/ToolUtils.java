//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.system;

import java.io.File;

/**
 * Miscellaneous utility and convenience methods for creating (command line) tools.
 * This mainly relates to dealing with input and output files and directories.
 */
public final class ToolUtils {
	
	private ToolUtils() {
	}

	public static File parseInputFile(String path) {
		File inputFile = new File(path);
		if (!inputFile.exists() || inputFile.isDirectory()) {
			throw new IllegalArgumentException("Input file does not exist: " + path);
		}
		return inputFile;
	}
	
	public static File parseInputDir(String path) {
		File inputDir = new File(path);
		if (!inputDir.exists() || !inputDir.isDirectory()) {
			throw new IllegalArgumentException("Input directory does not exist: " + path);
		}
		return inputDir;
	}
	
	public static File parseOutputFile(String path) {
		File outputFile = new File(path);
		if (outputFile.exists()) {
			throw new IllegalArgumentException("Output file already exists: " + path);
		}
		return outputFile;
	}
	
	public static File parseOutputDir(String path, boolean createIfAbsent) {
		File outputDir = new File(path);
		if (!outputDir.exists()) {
			if (createIfAbsent) {
				outputDir.mkdir();
			} else {
				throw new IllegalArgumentException("Output directory does not exist: " + path);
			}
		}
		return outputDir;
	}
	
	public static File parseOutputDir(String path) {
		return parseOutputDir(path, true);
	}
}
