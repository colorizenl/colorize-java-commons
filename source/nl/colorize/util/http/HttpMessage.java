//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import nl.colorize.util.LogHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Message sent using the HTTP protocol. This class contains an implementation
 * for accessing the message headers and body. It is extended by
 * {@link URLLoader} and {@link URLResponse} that represent HTTP requests and
 * HTTP responses respectively.
 */
public abstract class HttpMessage {

    private Multimap<HeaderName, String> headers;
    private byte[] body;
    private Charset encoding;

    public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
    private static final Logger LOGGER = LogHelper.getLogger(HttpMessage.class);

    protected HttpMessage(Charset encoding) {
        this.headers = ArrayListMultimap.create();
        this.body = new byte[0];
        this.encoding = encoding;
    }

    protected HttpMessage() {
        this(DEFAULT_CHARSET);
    }

    /**
     * Adds the specified header to the request. Note this will not replace any
     * existing headers with the same name. The header name is considered
     * case-insensitive.
     */
    public void addHeader(String name, String value) {
        Preconditions.checkArgument(name != null && !name.isEmpty(),
            "Invalid HTTP header name: " + name);
        Preconditions.checkArgument(value != null, "Invalid HTTP header value: " + value);

        headers.put(new HeaderName(name), value);
    }

    /**
     * Convenience method for setting multiple headers at the same time. Each
     * header is added using {@link #addHeader(String, String)}.
     */
    public void addHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            addHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds the specified header to the request, replacing any headers with the
     * same name already added. The header name is considered case-insensitive.
     */
    public void replaceHeader(String name, String value) {
        headers.removeAll(new HeaderName(name));
        addHeader(name, value);
    }

    /**
     * Returns the value of the HTTP header with the specified name. If the
     * message contains multiple headers with this name, the first value is
     * returned. The header name is considered case-insensitive.
     * @throws IllegalArgumentException if the message contains no such header.
     */
    public String getHeader(String name) {
        Collection<String> values = headers.get(new HeaderName(name));
        Preconditions.checkArgument(!values.isEmpty(),
            "HTTP message contains no such header: " + name);
        return values.iterator().next();
    }

    /**
     * Returns the value of the HTTP header with the specified name, or the
     * provided default value if no such header exists. If the message contains
     * multiple headers with this name, the first value is returned. The header
     * name is considered case-insensitive.
     */
    public String getHeader(String name, String defaultValue) {
        Collection<String> values = headers.get(new HeaderName(name));
        if (values.isEmpty()) {
            return defaultValue;
        }
        return values.iterator().next();
    }

    /**
     * Returns the values of all headers that match the specified name. If
     * the message contains no such headers this will return an empty list.
     * The header name is considered case-insensitive.
     */
    public List<String> getHeaderValues(String name) {
        return (List<String>) headers.get(new HeaderName(name));
    }

    /**
     * Returns true if the message contains at least one header with the
     * specified name. The header name is considered case-insensitive.
     */
    public boolean hasHeader(String name) {
        return headers.containsKey(new HeaderName(name));
    }

    public Set<String> getHeaderNames() {
        return headers.keySet().stream()
            .map(header -> header.header)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns the (parsed) value of the {@code Content-Type} header. Falls back
     * to the specified default value if the header was not specified or its
     * contents are malformed.
     */
    public MediaType getContentType(MediaType defaultValue) {
        String contentType = getHeader(HttpHeaders.CONTENT_TYPE);

        if (contentType != null && !contentType.isEmpty()) {
            try {
                return MediaType.parse(contentType);
            } catch (Exception e) {
                LOGGER.warning("Invalid Content-Type header: " + contentType);
            }
        }

        return defaultValue;
    }

    public void setBody(String contentType, byte[] body) {
        replaceHeader(HttpHeaders.CONTENT_TYPE, contentType);
        this.body = body;
    }

    public void setBody(MediaType contentType, byte[] body) {
        setBody(contentType.toString(), body);
    }

    public void setBody(String contentType, String body) {
        setBody(contentType, body.getBytes(encoding));
    }

    public void setBody(MediaType contentType, String body) {
        setBody(contentType.toString(), body);
    }

    public void setBody(PostData body) {
        String contentType = "application/x-www-form-urlencoded;charset=" + encoding.displayName();
        setBody(contentType, body.encode(encoding));
    }

    public void setJsonBody(String json) {
        setBody(MediaType.JSON_UTF_8.toString(), body);
    }

    /**
     * Sets the message body without specifying the content type.
     * @deprecated Use {@link #setBody(String, String)} instead.
     */
    @Deprecated
    public void setBody(String body) {
        this.body = body.getBytes(encoding);
    }

    /**
     * Sets the message body without specifying the content type.
     * @deprecated Use {@link #setBody(String, byte[])} instead.
     */
    @Deprecated
    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getBody() {
        return new String(body, encoding);
    }

    public InputStream openBodyStream() {
        return new ByteArrayInputStream(body);
    }

    public Reader openBodyReader() {
        return new InputStreamReader(new ByteArrayInputStream(body), encoding);
    }

    public boolean hasBody() {
        return body.length > 0;
    }

    public Charset getEncoding() {
        return encoding;
    }

    /**
     * Used to represent a case-insensitive HTTP header name. The header names
     * do preserve their original case, so {@code Content-Type} will be considered
     * equal to {@code content-type}, but still keeps the uppercase letters in
     * its textual representation.
     */
    private static class HeaderName {

        private String header;
        private int hash;

        public HeaderName(String header) {
            this.header = header;
            this.hash = header.toLowerCase().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof HeaderName) {
                HeaderName other = (HeaderName) o;
                return other.header.equalsIgnoreCase(header);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return header;
        }
    }
}
