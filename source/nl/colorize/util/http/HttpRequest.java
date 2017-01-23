//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import nl.colorize.util.Escape;

/**
 * Represents a HTTP request that is sent from a client to a server. The purpose 
 * of this class is similar to {@link javax.servlet.http.HttpServletRequest},
 * but can (also) be used in environments where the Servlet API is not available
 * or supported. 
 */
public class HttpRequest extends HttpTraffic {

	private Method method;
	private String path;
	
	/**
	 * Creates a HTTP request with a textual request body. The character encoding
	 * is derived from the {@code Content-Type} header, with a default of UTF-8
	 * if no character encoding is specified.
	 * @throws NullPointerException if the body is {@code null}. Use an empty
	 *         string to indicate the request has no body.
	 */
	public HttpRequest(Method method, String path, Map<String, String> headers, String body) {
		super(headers, body.getBytes(DEFAULT_CHARSET));
		
		Preconditions.checkArgument(method.hasRequestBody() || body.isEmpty(),
				"Request method " + method + " does not allow a request body");
		Preconditions.checkArgument(path.startsWith("/"), "Invalid path: " + path);
		
		this.method = method;
		this.path = path;
	}
	
	/**
	 * Creates a HTTP request without a request body.
	 */
	public HttpRequest(Method method, String path, Map<String, String> headers) {
		this(method, path, headers, "");
	}
	
	/**
	 * Creates a HTTP request without a request body.
	 */
	public HttpRequest(Method method, String path) {
		this(method, path, ImmutableMap.<String, String>of());
	}

	public Method getMethod() {
		return method;
	}

	/**
	 * Returns the request path to which this request was made. The returned path
	 * is relative, starts with a leading slash, and does not include the query
	 * string. Requests made to the "root" will have a path of "/".  
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * Parses a request body that contains key/value pairs. The request body is
	 * assumed to use the common encoding for the "application/x-www-form-urlencoded"
	 * content type.
	 */
	public Map<String, String> parsePostData() {
		return Escape.formDecode(getBody(), getCharset());
	}
	
	@Override
	public String toString() {
		return method + " " + path;
	}
}
