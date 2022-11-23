//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

/**
 * Indicates a problem while accessing one of the application resource files,
 * either because the file cannot be found or because it cannot be read.
 * <p>
 * The purpose of this exception is similar to how {@link java.io.IOException}
 * is used when reading files from the local file system. However, this exception
 * focuses on the application's own files, which are assumed to be available.
 * This means the application cannot provide meaningful error handling, since the
 * exception occurring means the application has somehow entered an impossible
 * state.
 */
public class ResourceFileException extends RuntimeException {

    public ResourceFileException(String message) {
        super(message);
    }

    public ResourceFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceFileException(ResourceFile file, String message) {
        super("Error while accessing resource file '" + file + "': " + message);
    }

    public ResourceFileException(ResourceFile file, Throwable cause) {
        super("Error while accessing resource file '" + file + "'", cause);
    }
}
