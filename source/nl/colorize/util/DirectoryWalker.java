//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

/**
 * Walks through all files in a directory, recursively. Filters can be used to
 * determine which files and/or subdirectories should be visited. Hidden files
 * and subdirectories are not visited by default, but this behavior can be
 * changed. 
 */
public class DirectoryWalker {
	
	private FilenameFilter fileFilter;
	private FilenameFilter dirFilter;
	private boolean recursive;
	private boolean visitHiddenFiles;
	private boolean includeSubdirectories;
	private int maxDepth;

	/**
	 * Creates a directory walker that is initially configured to ignore hidden
	 * files and subdirectories, but visits all other files.
	 */
	public DirectoryWalker() {
		recursive = true;
		visitHiddenFiles = false;
		includeSubdirectories = false;
		maxDepth = Integer.MAX_VALUE;
	}
	
	/**
	 * Walks through all files in {@code dir}, recursively.
	 * @return All files that were visited and that should be included in the
	 *         results according to the set filters.
	 * @throws IllegalArgumentException if the target directory does not exist.
	 */
	public List<File> walk(File dir) {
		Preconditions.checkArgument(dir.exists() && dir.isDirectory(), 
				"No such directory: " + dir.getAbsolutePath());
		
		List<File> result = new ArrayList<File>();
		Set<File> visited = new HashSet<File>();
		
		visit(dir, result, visited, 0);
		
		// Sorts the files according to the platform's convention
		Collections.sort(result);
		
		return result;
	}
	
	/**
	 * Visits the specified directory, recursively, and adds files to the result
	 * list depending on the set filters.
	 * @param visited Keeps track of visited directories to prevent infinite
	 *        recursion if the directory contains symlinks to its parent.
	 */
	private void visit(File dir, List<File> result, Set<File> visited, int atDepth) {
		visited.add(dir);
		if (atDepth > 0 && includeSubdirectories) {
			result.add(dir);
		}
		
		for (File entry : getDirectoryContents(dir)) {
			if (shouldVisit(entry, atDepth)) {
				if (entry.isDirectory()) {
					if (!visited.contains(entry)) {
						visit(entry, result, visited, atDepth + 1);
					}
				} else {
					result.add(entry);
				}
			}
		}
	}
	
	private File[] getDirectoryContents(File dir) {
		File[] contents = dir.listFiles();
		if (contents == null) {
			// This might return null in some obscure cases (the JavaDoc
			// is vague about this). Because it's so rare and the cause
			// is unknown it's not worth throwing an IOException for it.
			throw new RuntimeException("Cannot access directory " + dir.getAbsolutePath());
		}
		return contents;
	}

	private boolean shouldVisit(File entry, int atDepth) {
		if (!visitHiddenFiles && entry.isHidden()) {
			return false;
		}
		
		if (atDepth >= maxDepth) {
			return false;
		}
		
		File parent = entry.getParentFile();
		String name = entry.getName();
		if (entry.isDirectory()) {
			return shouldVisitDirectory(parent, name);
		} else {
			return shouldVisitFile(parent, name);
		}
	}
	
	private boolean shouldVisitFile(File parent, String name) {
		return (fileFilter == null) || fileFilter.accept(parent, name);
	}

	private boolean shouldVisitDirectory(File parent, String name) {
		return recursive && (dirFilter == null || dirFilter.accept(parent, name));
	}

	public void setFileFilter(FilenameFilter fileFilter) {
		this.fileFilter = fileFilter;
	}
	
	public FilenameFilter getFileFilter() {
		return fileFilter;
	}
	
	public void setDirectoryFilter(FilenameFilter dirFilter) {
		this.dirFilter = dirFilter;
	}
	
	public FilenameFilter getDirectoryFilter() {
		return dirFilter;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
	
	public boolean isRecursive() {
		return recursive;
	}

	public void setVisitHiddenFiles(boolean visitHiddenFiles) {
		this.visitHiddenFiles = visitHiddenFiles;
	}

	public boolean getVisitHiddenFiles() {
		return visitHiddenFiles;
	}
	
	public void setIncludeSubdirectories(boolean includeSubdirectories) {
		this.includeSubdirectories = includeSubdirectories;
	}

	public boolean getIncludeSubdirectories() {
		return includeSubdirectories;
	}
	
	public void setMaxDepth(int maxDepth) {
		Preconditions.checkArgument(maxDepth >= 1, "Invalid max depth: " + maxDepth);
		this.maxDepth = maxDepth;
	}
	
	public int getMaxDepth() {
		return maxDepth;
	}
}
