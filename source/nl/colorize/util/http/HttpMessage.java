//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import nl.colorize.util.LogHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Message sent using the HTTP protocol. This class contains an implementation
 * for accessing the message headers and body. It is extended by
 * {@link URLLoader} and {@link URLResponse} that represent HTTP requests and
 * HTTP responses respectively.
 */
public abstract class HttpMessage {

    private Headers headers;
    private byte[] body;
    private Charset encoding;

    public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
    private static final Logger LOGGER = LogHelper.getLogger(HttpMessage.class);

    protected HttpMessage(Charset encoding) {
        this.headers = new Headers();
        this.body = new byte[0];
        this.encoding = encoding;
    }

    protected HttpMessage() {
        this(DEFAULT_CHARSET);
    }

    public Headers getHeaders() {
        return headers;
    }

    public void addHeader(String name, String value) {
        headers.add(name, value);
    }

    public void addHeaders(Headers headers) {
        this.headers.merge(headers);
    }

    public void addHeaders(Map<String, String> entries) {
        headers.add(entries);
    }

    /**
     * Returns the (parsed) value of the {@code Content-Type} header. Falls back
     * to the specified default value if the header was not specified or its
     * contents are malformed.
     */
    public MediaType getContentType(MediaType defaultValue) {
        String contentType = headers.getValue(HttpHeaders.CONTENT_TYPE, null);

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
        headers.replace(HttpHeaders.CONTENT_TYPE, contentType);
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

    public void setBody(String body) {
        this.body = body.getBytes(encoding);
    }

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
}
