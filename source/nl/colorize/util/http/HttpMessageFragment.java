//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import nl.colorize.util.LogHelper;

import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * Common functionality for messages (requests and responses) sent using the
 * HTTP protocol.
 */
public interface HttpMessageFragment {

    public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
    public static final Logger LOGGER = LogHelper.getLogger(HttpMessageFragment.class);

    public String getHeader(String name);

    /**
     * Returns the (parsed) value of the {@code Content-Type} header. Falls back
     * to the specified default value if the header was not specified or its
     * contents are malformed.
     */
    default MediaType getContentType(MediaType defaultValue) {
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

    /**
     * Returns the character encoding used for both the headers and the body, as
     * specified in the {@code Content-Type} header. Returns the default character
     * encoding of UTF-8 if no character encoding is specified.
     */
    default Charset getCharset() {
        MediaType contentType = getContentType(null);
        try {
            if (contentType != null && contentType.charset().isPresent()) {
                return contentType.charset().get();
            }
        } catch (Exception e) {
            LOGGER.warning("Invalid character encoding in Content-Type header: " + contentType);
        }
        return DEFAULT_CHARSET;
    }
}
