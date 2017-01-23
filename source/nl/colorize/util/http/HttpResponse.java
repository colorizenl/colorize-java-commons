//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Represents a HTTP response that is sent from a client to a server in response
 * to a request. The purpose of this class is similar to 
 * {@link javax.servlet.http.HttpServletResponse}, but can (also) be used in
 * environments where the Servlet API is not available or supported. 
 */
public class HttpResponse extends HttpTraffic {

	private HttpStatus status;
	
	/**
	 * Creates a HTTP response with a binary response body.
	 * @throws NullPointerException if the body is {@code null}. Use an empty
	 *         array to indicate the response has no body.
	 */
	public HttpResponse(HttpStatus status, Map<String, String> headers, byte[] body) {
		super(headers, body);
		this.status = status;
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
		this(status, ImmutableMap.<String, String>of(), "");
	}
	
	public HttpStatus getStatus() {
		return status;
	}
	
	@Override
	public String toString() {
		return status.getCode() + " " + status.getDescription();
	}
}
