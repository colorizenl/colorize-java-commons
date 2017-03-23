//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import nl.colorize.util.DirectoryWalker;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Relation;
import nl.colorize.util.Tuple;

/**
 * Command line tool that updates the copyright statement at the top of the 
 * file, for all files in a directory. This tool assumes all files use the 
 * UTF-8 character encoding and the Unix line separator (\n). Different 
 * character encodings or line endings will be replaced.
 */
public class CopyrightUpdateTool {
	
	private String newCopyrightYear;
	private boolean dryRun;

	private static final Pattern COPYRIGHT_PATTERN = Pattern.compile(
			"Copyright\\s+(20\\d+)(\\s*-\\s*20\\d+)?");
	private static final List<String> SUPPORTED_FILE_EXTENSIONS = ImmutableList.of(
			".java", ".js", ".ts", ".php", ".gradle", ".md");
	private static final List<String> EXCLUDE_DIRS = ImmutableList.of(
			"/build/", "/lib/", "/node_modules/", "/.git/", "/.gradle/");
	private static final Charset FILE_CHARSET = Charsets.UTF_8;
	private static final Logger LOGGER = LogHelper.getLogger(CopyrightUpdateTool.class);

	public static void main(String[] args) {
		if (args.length < 2) {
			LOGGER.info("Usage: CopyrightUpdateTool <dir> <newCopyrightYear> [dryRun]");
			System.exit(1);
		}
		
		File dir = new File(args[0]);
		String newCopyrightYear = args[1];
		boolean dryRun = args[2].equals("dryRun") || args[2].equals("true");
		
		CopyrightUpdateTool tool = new CopyrightUpdateTool(newCopyrightYear, dryRun);
		tool.updateDirectory(dir);
	}
	
	public CopyrightUpdateTool(String newCopyrightYear, boolean dryRun) {
		this.newCopyrightYear = newCopyrightYear;
		this.dryRun = dryRun;
	}

	private void updateDirectory(File dir) {
		List<File> files = findFiles(dir);
		LOGGER.info("Updating copyright statements");
		LOGGER.info("Found " + files.size() + " files");
		
		for (File file : files) {
			LOGGER.info("Updating " + LoadUtils.getRelativePath(file, dir));
			try {
				updateCopyrightStatement(file);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Cannot update file " + file.getName(), e);
			}
		}
		
		if (!dryRun) {
			LOGGER.info("Done, updated " + files.size() + " files");
		}
	}

	private List<File> findFiles(File dir) {
		DirectoryWalker dirWalker = new DirectoryWalker();
		dirWalker.setIncludeSubdirectories(false);
		dirWalker.setRecursive(true);
		dirWalker.setVisitHiddenFiles(false);
		dirWalker.setFileFilter(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return shouldUpdateCopyright(new File(dir, name));
			}
		});
		return dirWalker.walk(dir);
	}
	
	private boolean shouldUpdateCopyright(File file) {
		String ext = "." + Files.getFileExtension(file.getName());
		return SUPPORTED_FILE_EXTENSIONS.contains(ext) && !isExcludedDir(file);
	}
	
	private boolean isExcludedDir(File file) {
		for (String excludedDir : EXCLUDE_DIRS) {
			if (file.getAbsolutePath().contains(excludedDir)) {
				return true;
			}
		}
		return false;
	}

	private void updateCopyrightStatement(File file) throws IOException {
		List<String> originalLines = Files.readLines(file, FILE_CHARSET);
		List<String> processedLines = new ArrayList<String>();
		
		for (String line : originalLines) {
			processedLines.add(processLine(line));
		}
		
		for (Tuple<String, String> diff : calculateDiff(originalLines, processedLines)) {
			LOGGER.info("    <<< " + diff.getLeft().trim());
			LOGGER.info("    >>> " + diff.getRight().trim());
		}
			
		if (!dryRun) {
			PrintWriter writer = new PrintWriter(file, FILE_CHARSET.displayName());
			for (String line : processedLines) {
				writer.println(line);
			}
			writer.close();
		}
	}

	private String processLine(String line) {
		Matcher matcher = COPYRIGHT_PATTERN.matcher(line);
		if (matcher.find()) {
			if (matcher.group(2) == null || newCopyrightYear.indexOf('-') != -1) {
				return matcher.replaceFirst("Copyright " + newCopyrightYear);
			} else {
				return matcher.replaceFirst("Copyright $1-" + newCopyrightYear);
			}
		} else {
			return line;
		}
	}

	private Relation<String, String> calculateDiff(List<String> first, List<String> second) {
		if (first.size() != second.size()) {
			throw new IllegalArgumentException("File should have the same length before/after");
		}
		
		Relation<String, String> diff = Relation.of();
		for (int i = 0; i < first.size(); i++) {
			if (!first.get(i).equals(second.get(i))) {
				diff.add(Tuple.of(first.get(i), second.get(i)));
			}
		}
		return diff;
	}
}
