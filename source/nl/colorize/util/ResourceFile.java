//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
public record ResourceFile(String path) implements Resource {

    private static final Splitter PATH_SPLITTER = Splitter.on("/").omitEmptyStrings();
    
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
        List<String> pathComponents = PATH_SPLITTER.splitToList(path);
        return pathComponents.getLast();
    }

    @Override
    public InputStream openStream() {
        if (Platform.isTeaVM()) {
            throw new UnsupportedOperationException("Resource files are not supported on TeaVM");
        }

        // Attempt 1: Locate file in classpath.

        ClassLoader classLoader = ResourceFile.class.getClassLoader();
        InputStream inClassPath = classLoader.getResourceAsStream(path);

        if (inClassPath != null) {
            return inClassPath;
        }

        // Attempt 2: Locate file in local file system.

        File inFileSystem = new File(path);

        if (inFileSystem.exists() && !inFileSystem.isDirectory()) {
            try {
                return new FileInputStream(inFileSystem);
            } catch (IOException e) {
                throw new ResourceException("Error while accessing " + this, e);
            }
        }

        throw new ResourceException("Resource file not found: " + path);
    }

    @Override
    public BufferedReader openReader(Charset charset) {
        InputStreamReader reader = new InputStreamReader(openStream(), charset);
        return new BufferedReader(reader);
    }

    /**
     * Returns true if the resource file exists in one of the searched locations.
     */
    public boolean exists() {
        try (InputStream ignored = openStream()) {
            return true;
        } catch (IOException | ResourceException e) {
            return false;
        }
    }

    /**
     * Returns a {@link ResourceFile} that points to a different file located
     * in the same parent directory as this resource file.
     *
     * @throws IllegalArgumentException if trying to provide an absolute file
     *         path to the {@code name} argument.
     */
    public ResourceFile sibling(String name) {
        Preconditions.checkArgument(!name.startsWith("//"), "Cannot provide absolute path");

        List<String> pathComponents = PATH_SPLITTER.splitToList(path);

        List<String> siblingPath = new ArrayList<>();
        siblingPath.addAll(pathComponents.subList(0, pathComponents.size() - 1));
        siblingPath.add(name);

        return new ResourceFile(String.join("/", siblingPath));
    }

    @Override
    public String toString() {
        return path;
    }
}
