//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Reference to a resource file (files included with the application). Resource
 * files can be located both in the classpath and in the local file system. This 
 * class can be used to pass around handles to resource files without first
 * having to open those files.
 */
public final class ResourceFile {
    
    private String path;
    
    public ResourceFile(String path) {    
        this.path = normalizePath(path);
        checkPath();
    }
    
    public ResourceFile(ResourceFile dir, String path) {
        this.path = normalizePath(dir.getPath(), path);
        checkPath();
    }
    
    public ResourceFile(File file) {
        this(file.getAbsolutePath());
    }
    
    private String normalizePath(String path) {
        String normalizedPath = path;
        normalizedPath = normalizedPath.trim();
        normalizedPath = normalizedPath.replace('\\', '/');        
        if (normalizedPath.endsWith("/")) { 
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        if (normalizedPath.startsWith("./")) {
            normalizedPath = normalizedPath.substring(2);
        }
        return normalizedPath;
    }
    
    private String normalizePath(String parentPath, String childPath) {
        String normalizedParentPath = normalizePath(parentPath);
        String normalizedPath = normalizePath(childPath);
        if (normalizedPath.startsWith(normalizedParentPath)) {
            return normalizedPath;
        } else {
            return normalizedParentPath + "/" + normalizedPath;
        }
    }
    
    private void checkPath() {
        if (path.trim().length() == 0) {
            throw new IllegalArgumentException("Empty path");
        }
    }
    
    /**
     * Returns the normalized path that is used to locate the file referenced 
     * by this class. The path will always use forward slashes as delimiters, 
     * regardless of the platform's conventions.
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Returns the name of this resource file. The name is the part of the path
     * after the last path separator. For example, the path "/a/b/c.txt" will
     * return a name of "c.txt".
     */
    public String getName() {
        int index = path.lastIndexOf('/');
        if (index == -1) {
            return path;
        }
        return path.substring(index + 1);
    }
    
    /**
     * Opens a stream to the resource file represented by this file handle. The
     * locations that are searched depend on the platform, and is implemented by
     * {@link Platform#openResourceFile(String)}.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public InputStream openStream() throws IOException {
        return Platform.openResourceFile(path);
    }
    
    /**
     * Convenience method that opens a reader to the referenced file.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public BufferedReader openReader(Charset charset) throws IOException {
        InputStreamReader reader = new InputStreamReader(openStream(), charset);
        return new BufferedReader(reader);
    }
    
    /**
     * Convenience method that reads in the binary contents of this resource file.
     * @throws IllegalStateException if the resource file does not exist.
     */
    public byte[] readBytes() {
        try {
            return LoadUtils.readToByteArray(openStream());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read resource file " + path, e);
        }
    }
    
    /**
     * Convenience method that reads in the textual contents of this resource file.
     * @throws IllegalStateException if the resource file does not exist.
     */
    public String read(Charset charset) {
        try {
            return LoadUtils.readToString(openReader(charset));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read resource file " + path, e);
        }
    }
    
    /**
     * Convenience method that reads in the textual contents of this resource file
     * and returns the contents as a list of lines.
     * @throws IllegalStateException if the resource file does not exist.
     */
    public List<String> readLines(Charset charset) {
        try {
            return LoadUtils.readLines(openReader(charset));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read resource file " + path, e);
        }
    }
    
    /**
     * Returns true if the resource file exists in one of the searched locations.
     */
    public boolean exists() {
        try {
            return openStream() != null;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof ResourceFile) {
            ResourceFile other = (ResourceFile) o;
            return path.equals(other.path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
    
    @Override
    public String toString() {
        return path;
    }
}
