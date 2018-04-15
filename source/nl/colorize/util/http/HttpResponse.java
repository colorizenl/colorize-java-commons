//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

/**
 * Represents the HTTP response that is returned by a server.
 */
public class HttpResponse implements HttpMessageFragment {

    private HttpStatus status;
    private Map<String, String> headers;
    private byte[] body;

    /**
     * Creates a HTTP response with a binary response body.
     * @throws NullPointerException if the body is {@code null}. Use an empty
     *         array to indicate the response has no body.
     */
    public HttpResponse(HttpStatus status, Map<String, String> headers, byte[] body) {
        this.status = status;
        this.headers = ImmutableMap.copyOf(headers);
        this.body = body == null ? new byte[0] : body;
    }
    
    /**
     * Creates a HTTP response with a textual response body. The character encoding
     * is derived from the {@code Content-Type} header, with a default of UTF-8
     * if no character encoding is specified.
     * @throws NullPointerException if the body is {@code null}. Use an empty
     *         string to indicate the response has no body.
     */
    public HttpResponse(HttpStatus status, Map<String, String> headers, String body) {
        this(status, headers, body.getBytes(DEFAULT_CHARSET));
    }
    
    /**
     * Creates a HTTP response without a response body.
     */
    public HttpResponse(HttpStatus status, Map<String, String> headers) {
        this(status, headers, "");
    }

    /**
     * Creates a HTTP response without a response body.
     */
    public HttpResponse(HttpStatus status) {
        this(status, ImmutableMap.of(), "");
    }
    
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns the value of the response header with the specified name. Header
     * names are not case-sensitive, as stated by RFC 2616. If no header with
     * the requested name exists {@code null} will be returned.
     */
    @Override
    public String getHeader(String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the request/response body in text form, or an empty string if no
     * body exists (as indicated by {@link #hasBody()}). The body is converted
     * to text based on the character encoding specified in the
     * {@code Content-Type} header.
     */
    public String getBody() {
        return new String(body, getCharset());
    }

    /**
     * Returns the request/response body in binary form. Returns an empty array
     * if no body exists (as indicated by {@link #hasBody()}).
     */
    public byte[] getBinaryBody() {
        return body;
    }

    public boolean hasBody() {
        return body.length > 0;
    }

    /**
     * Opens a stream to the binary request/response body.
     * @throws IllegalStateException if no body exists.
     */
    public InputStream openStream() {
        if (!hasBody()) {
            throw new IllegalStateException("No body exists");
        }
        return new ByteArrayInputStream(body);
    }

    /**
     * Opens a reader to the request/response body. The body is converted to
     * text based on the character encoding specified in the {@code Content-Type}
     * header.
     * @throws IllegalStateException if no body exists.
     */
    public Reader openReader() {
        return new InputStreamReader(openStream(), getCharset());
    }

    @Override
    public String toString() {
        return status.getCode() + " " + status.getDescription();
    }
}
