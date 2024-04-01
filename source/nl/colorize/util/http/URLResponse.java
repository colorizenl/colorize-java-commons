//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.net.HttpHeaders;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.colorize.util.Resource;

import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.PROTECTED;

/**
 * Represents an HTTP response that is returned after sending an HTTP request
 * to a URL. Used in conjunction with {@link URLLoader}, which supports/uses
 * a variety of HTTP clients depending on the platform.
 */
@Getter
public class URLResponse implements Resource {

    private int status;
    private Headers headers;
    private byte[] body;
    @Setter(PROTECTED) private SSLSession sslSession;

    public URLResponse(int status, Headers headers, byte[] body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public URLResponse(int status, Headers headers, String body) {
        this(status, headers, body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the value of the HTTP header with the specified name. Using
     * this method is equivalent to {@code getHeaders().get(name)}.
     */
    public Optional<String> getHeader(String name) {
        return headers.get(name);
    }

    /**
     * Returns all values for the HTTP header with the specified name. Using
     * this method is equivalent to {@code getHeaders().getAll(name)}.
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

    public String readBody() {
        return read(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(headers);
        buffer.append("\n\n");
        buffer.append(readBody());
        return buffer.toString();
    }
}
