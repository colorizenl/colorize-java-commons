//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
 * <p>
 * Resource file paths will always use forward slashes as delimiters, regardless
 * of the platform's file separator.
 */
public record ResourceFile(String path) {
    
    public ResourceFile(String path) {
        Preconditions.checkArgument(!path.trim().isEmpty(), "Empty path");
        this.path = normalizePath(path);
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
     * Opens a stream to the resource file represented by this file handle.
     *
     * @throws FileNotFoundException if an I/O error occurs while reading the file.
     */
    public InputStream openStream() throws FileNotFoundException {
        // Attempt 1: Locate file in classpath.
        ClassLoader classLoader = ResourceFile.class.getClassLoader();
        InputStream inClassPath = classLoader.getResourceAsStream(path);

        if (inClassPath != null) {
            return inClassPath;
        }

        // Attempt 2: Locate file in local file system.
        File inFileSystem = new File(path);

        if (inFileSystem.exists() && !inFileSystem.isDirectory()) {
            return new FileInputStream(inFileSystem);
        }

        throw new FileNotFoundException("Resource file not found: " + path);
    }

    /**
     * Convenience method that opens a reader to the referenced file.
     *
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public BufferedReader openReader(Charset charset) throws IOException {
        InputStreamReader reader = new InputStreamReader(openStream(), charset);
        return new BufferedReader(reader);
    }
    
    /**
     * Convenience method that reads in the binary contents of this resource file.
     *
     * @throws ResourceFileException if the resource file does not exist.
     */
    public byte[] readBytes() {
        try (InputStream stream = openStream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new ResourceFileException(this, "Cannot read resource file");
        }
    }
    
    /**
     * Convenience method that reads in the textual contents of this resource file.
     *
     * @throws ResourceFileException if the resource file does not exist.
     */
    public String read(Charset charset) {
        try (BufferedReader reader = openReader(charset)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new ResourceFileException(this, "Cannot read resource file");
        }
    }
    
    /**
     * Convenience method that reads in the textual contents of this resource file
     * and returns the contents as a list of lines.
     *
     * @throws ResourceFileException if the resource file does not exist.
     */
    public List<String> readLines(Charset charset) {
        try (BufferedReader reader = openReader(charset)) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new ResourceFileException(this, "Cannot read resource file");
        }
    }
    
    /**
     * Returns true if the resource file exists in one of the searched locations.
     */
    public boolean exists() {
        try (InputStream ignored = openStream()) {
            return true;
        } catch (IOException | ResourceFileException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return path;
    }
}
