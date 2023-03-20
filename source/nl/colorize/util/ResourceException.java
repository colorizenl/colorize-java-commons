//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

/**
 * Indicates a problem while accessing one of the application's resources,
 * either because the resource cannot be found or because it cannot be read.
 * <p>
 * The purpose of this exception is similar to how {@link java.io.IOException}
 * is used when reading files from the local file system. However, this
 * exception focuses on the application's own resources, which are assumed to
 * be available.
 */
public class ResourceException extends RuntimeException {

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
