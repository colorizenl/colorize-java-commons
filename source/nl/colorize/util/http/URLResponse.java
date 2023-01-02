//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.net.HttpHeaders;
import nl.colorize.util.Resource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an HTTP response that is returned after sending an HTTP request
 * to a URL. Used in conjunction with {@link URLLoader}, which supports/uses a
 * variety of HTTP clients depending on the platform.
 * <p>
 * <strong>Note:</strong> The {@code connectionProperties} field is populated
 * depending on the HTTP client. Since {@link URLLoader} supports multiple HTTP
 * clients depending on the platform, these properties can have different types
 * depending on the HTTP client, and some properties might not be available for
 * certain HTTP clients.
 */
public record URLResponse(
    int status,
    Headers headers,
    byte[] body,
    Charset encoding,
    Map<String, Object> connectionProperties
) implements Resource {

    /**
     * Returns the value of the HTTP header with the specified name. Using this
     * method is equivalent to {@code headers().get(name)}.
     */
    public Optional<String> getHeader(String name) {
        return headers.get(name);
    }

    /**
     * Returns all values for the HTTP header with the specified name. Using
     * this method is equivalent to {@code headers().getAll(name)}.
     */
    public List<String> getHeaderValues(String name) {
        return headers.getAll(name);
    }

    /**
     * Returns the value of the HTTP {@code Content-Type} header.
     */
    public Optional<String> getContentType() {
        return getHeader(HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public BufferedReader openReader(Charset charset) {
        return new BufferedReader(new StringReader(read(charset)));
    }

    @Override
    public String read(Charset charset) {
        return new String(body, charset);
    }

    /**
     * Parses the response body to text, using the same character encoding that
     * was used to send the request.
     */
    public String getBody() {
        return read(encoding);
    }

    public Optional<Object> getConnectionProperty(String name) {
        return Optional.ofNullable(connectionProperties.get(name));
    }
}
