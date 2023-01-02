//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.io.CharStreams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Common interface for resource data. The interface defines a number of
 * methods for reading its contents as either binary data or as text.
 * <p>
 * Since this interface represents a resource that is known to be available,
 * the methods in this class do *not* throw {@code IOException}.
 */
public interface Resource {

    public InputStream openStream();

    public BufferedReader openReader(Charset charset);

    default byte[] readBytes() {
        try (InputStream stream = openStream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Resource access failed", e);
        }
    }

    default String read(Charset charset) {
        try (BufferedReader reader = openReader(charset)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException("Resource access failed", e);
        }
    }

    default List<String> readLines(Charset charset) {
        try (BufferedReader reader = openReader(charset)) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new RuntimeException("Resource access failed", e);
        }
    }
}
