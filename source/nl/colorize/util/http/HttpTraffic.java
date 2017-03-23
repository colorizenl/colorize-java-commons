//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import nl.colorize.util.LogHelper;

/**
 * Shared parent class for HTTP requests and responses.
 */
public abstract class HttpTraffic {

	private String protocol;
	private Map<String, String> headers;
	private byte[] body;
	
	public static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
	private static final Logger LOGGER = LogHelper.getLogger(HttpTraffic.class);
	
	protected HttpTraffic(String protocol, Map<String, String> headers, byte[] body) {
		this.protocol = protocol;
		this.headers = ImmutableMap.copyOf(headers);
		this.body = (body != null) ? body : new byte[0];
	}
	
	protected HttpTraffic(Map<String, String> headers, byte[] body) {
		this("HTTP/1.1", headers, body);
	}
	
	/**
	 * Returns the version of the HTTP protocol that was used for this 
	 * request/response communication, typically HTTP/1.1.
	 */
	public String getProtocol() {
		return protocol;
	}
	
	/**
	 * Returns the value of the response header with the specified name. Header
	 * names are not case-sensitive, as stated by RFC 2616. If no header with
	 * the requested name exists {@code null} will be returned. 
	 */
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
	
	/**
	 * Returns the character encoding used for both the headers and the body, as
	 * specified in the {@code Content-Type} header. Returns the default character
	 * encoding of UTF-8 if no character encoding is specified.
	 */
	public Charset getCharset() {
		MediaType contentType = getContentType(null);
		if (contentType != null && contentType.charset().isPresent()) {
			try {
				return contentType.charset().get();
			} catch (Exception e) {
				LOGGER.warning("Invalid character encoding in Content-Type header: " + contentType);
			}
		}
		return DEFAULT_CHARSET;
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
	 * Opens a reader to the request/response body. The body is converted to 
	 * text based on the character encoding specified in the {@code Content-Type} 
	 * header.
	 * @throws IllegalStateException if no body exists.
	 */
	public Reader openReader() {
		return new InputStreamReader(openStream(), getCharset());
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
}
