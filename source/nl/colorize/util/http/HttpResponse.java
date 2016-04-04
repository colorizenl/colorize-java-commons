//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import nl.colorize.util.LogHelper;

/**
 * Represents the response of a HTTP request. This includes the HTTP status, the
 * response headers, and the response body.
 */
public class HttpResponse {

	private HttpStatus status;
	private Map<String, String> headers;
	private MediaType contentType;
	private Charset charset;
	private byte[] body;
	
	private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
	private static final Logger LOGGER = LogHelper.getLogger(HttpResponse.class);
	
	/**
	 * Creates a HTTP response with the specified contents.
	 */
	public HttpResponse(HttpStatus status, Map<String, String> headers, String body) {
		this.status = status;
		this.headers = ImmutableMap.copyOf(headers);
		this.contentType = parseContentType(headers);
		this.charset = parseCharset(contentType);
		this.body = body.getBytes(getCharset(DEFAULT_CHARSET));
	}
	
	/**
	 * Creates a binary HTTP response with the specified contents.
	 */
	public HttpResponse(HttpStatus status, Map<String, String> headers, byte[] body) {
		this.status = status;
		this.headers = ImmutableMap.copyOf(headers);
		this.contentType = parseContentType(headers);
		this.charset = parseCharset(contentType);
		this.body = body;
	}
	
	/**
	 * Creates an empty response.
	 */
	public HttpResponse(HttpStatus status, Map<String, String> headers) {
		this(status, headers, "");
	}
	
	/**
	 * Creates an empty response.
	 */
	public HttpResponse(HttpStatus status) {
		this(status, ImmutableMap.<String, String>of(), "");
	}
	
	private MediaType parseContentType(Map<String, String> headers) {
		String header = getHeader(HttpHeaders.CONTENT_TYPE);
		if (header != null && !header.isEmpty()) {
			try {
				return MediaType.parse(header);
			} catch (IllegalArgumentException e) {
				LOGGER.warning("Response contains invalid Content-Type: " + header);
			}
		}
		
		return null;
	}

	private Charset parseCharset(MediaType contentType) {
		if (contentType != null) {
			try {
				return contentType.charset().orNull();
			} catch (UnsupportedCharsetException e) {
				LOGGER.warning("Response uses unsupported character encoding: " + contentType);
			} catch (IllegalCharsetNameException e) {
				LOGGER.warning("Response uses invalid character encoding: " + contentType);
			}
		}
		
		return null;
	}
	
	public HttpStatus getStatus() {
		return status;
	}
	
	public Set<String> getHeaderNames() {
		return headers.keySet();
	}
	
	/**
	 * Returns the value of the response header with the specified name. Header
	 * names are not case-sensitive, as stated by RFC 2616. If no header with
	 * the requested name exists {@code null} will be returned. If the response
	 * contained the same header multiple times the returned string will contain
	 * all values, separated by newlines.  
	 */
	public String getHeader(String name) {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if (name.equalsIgnoreCase(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	/**
	 * Returns a map containing all response headers.
	 * @deprecated This method may be used to obtain the value of a specific
	 *             header, in the form {@code getHeaders().get()}. This approach
	 *             is mistake-prone since header names are case-insensitive. Use
	 *             {@link #getHeaderNames()} and {@link #getHeader(String)} instead. 
	 */
	@Deprecated
	public Map<String, String> getHeaders() {
		return headers;
	}
	
	/**
	 * Returns the value of the Content-Type header. If the header is not set or
	 * if it contains an invalid value the provided default value will be returned
	 * instead.
	 */
	public MediaType getContentType(MediaType defaultValue) {
		if (contentType == null) {
			return defaultValue;
		}
		return contentType;
	}
	
	/**
	 * Returns the response's character encoding included in the Content-Type
	 * header. If the Content-Type is not set, contains no character encoding,
	 * has an invalid value, or if the character encoding is not supported the
	 * specified default character encoding will be used. 
	 */
	public Charset getCharset(Charset defaultCharset) {
		if (charset == null) {
			return defaultCharset;
		}
		return charset;
	}
	
	public InputStream openStream() {
		return new ByteArrayInputStream(body);
	}
	
	/**
	 * Reads the response body using the character encoding defined in the
	 * Content-Type header.
	 */
	public Reader openReader() {
		return new InputStreamReader(openStream(), getCharset(DEFAULT_CHARSET));
	}
	
	/**
	 * Reads the response body using the specified character encoding.
	 */
	public Reader openReader(Charset charset) {
		return new InputStreamReader(openStream(), charset);
	}
	
	public byte[] getBodyBytes() {
		return body;
	}
	
	public String getBodyText() {
		return new String(body, getCharset(DEFAULT_CHARSET));
	}
	
	public boolean hasBody() {
		return !getBodyText().isEmpty();
	}
	
	@Override
	public String toString() {
		return getBodyText();
	}
}
