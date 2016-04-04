//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import nl.colorize.util.Escape;
import nl.colorize.util.http.Method;

/**
 * Describes a request made to a REST API. This class contains information about
 * the service to which the request was made, as well as providing access to
 * the underlying {@link javax.servlet.http.HttpServletRequest}.
 */
public class RestRequest {
	
	private HttpServletRequest httpRequest;
	private List<String> pathComponents;
	private Map<String, String> pathParameters;
	private Map<String, String> parameters;
	
	/**
	 * Creates a {@code RestRequest} instance that wraps around an incoming
	 * HTTP request.
	 * <p>
	 * Note that request parameters and path parameters are <em>not</em> directly
	 * obtained from the HTTP request. Instead, they are passed in separately
	 * using {@link #bindParameters(Map)} and {@link #bindPathParameters()}. This
	 * allows REST APIs to support multiple parameter formats. 
	 */
	protected RestRequest(HttpServletRequest httpRequest, List<String> pathComponents) {
		this.httpRequest = httpRequest;
		this.pathComponents = ImmutableList.copyOf(pathComponents);
		pathParameters = Collections.emptyMap();
		parameters = Collections.emptyMap();
	}
	
	public HttpServletRequest getHttpRequest() {
		return httpRequest;
	}
	
	public Method getMethod() {
		return Method.parse(httpRequest.getMethod());
	}
	
	/**
	 * Returns the path to which this request was made. The path is relative to
	 * the REST API, and does not include the host name, servlet path, or query
	 * string. The path always starts with a leading slash, requests to the "root"
	 * of the REST API will have a path of "/".
	 */
	public String getPath() {
		return "/" + Joiner.on('/').join(getPathComponents());
	}
	
	/**
	 * Returns the path components for the path to which this request was made.
	 * The path is relative to the REST API, and does not include the host name,
	 * servlet path, or query string.
	 */
	protected List<String> getPathComponents() {
		return pathComponents;
	}
	
	protected void bindPathParameters(Map<String, String> pathParameters) {
		this.pathParameters = ImmutableMap.copyOf(pathParameters);
	}
	
	/**
	 * Returns the (URL-decoded) path component at the specified index. For
	 * example, a request to /a/b/c returns "b" for the path component at 
	 * index 1. 
	 * @throws BadRequestException if no path component exists at that index.
	 */
	public String getPathParameter(int index) {
		List<String> pathComponents = getPathComponents();
		if (index < 0 || index >= pathComponents.size()) {
			throw new BadRequestException("Invalid path parameter index: " + index);
		}
		String value = pathComponents.get(index);
		return Escape.urlDecode(value, RestServlet.REQUEST_CHARSET);
	}
	
	/**
	 * Returns the (URL-decoded) path component with the specified name. 
	 * @throws BadRequestException if no path parameter with that name exists.
	 */
	public String getPathParameter(String name) {
		String value = pathParameters.get(name);
		if (value == null || value.isEmpty()) {
			throw new BadRequestException("Unknown path parameter: " + name);
		}
		return value;
	}
	
	/**
	 * Returns all parameters sent with the request.
	 * @deprecated Use {@link #getRequiredParameter(String)} and/or
	 *             {@link #getOptionalParameter(String, String)} instead.
	 */
	@Deprecated
	@VisibleForTesting
	protected Map<String, String> getParameters() {
		return parameters;
	}
	
	protected void bindParameters(Map<String, String> parameters) {
		// Filter out parameters with null values, which according to the
		// HttpServletRequest documentation should be considered as if the
		// parameter is not present. Parameters will null *keys* are invalid
		// though, and will throw an exception when encountered.
		Predicate<String> parameterFiler = Predicates.notNull();
		this.parameters = ImmutableMap.copyOf(Maps.filterValues(parameters, parameterFiler));
	}
	
	/**
	 * Returns the value of the request parameter with the specified name.
	 * @throws BadRequestException if no parameter with that name exists.
	 */
	public String getRequiredParameter(String name) {
		String value = parameters.get(name);
		if (value == null || value.isEmpty()) {
			throw new BadRequestException("Missing required parameter: " + name);
		}
		return value;
	}
	
	/**
	 * Returns either the value of the request parameter with the specified
	 * name, or {@code defaultValue} if no parameter with that name exists.
	 */
	public String getOptionalParameter(String name, String defaultValue) {
		String value = parameters.get(name);
		if (value == null || value.isEmpty()) {
			return defaultValue;
		}
		return value;
	}
	
	/**
	 * Reads in the request body, and returns it as a string. If the request
	 * contains no body this will return an empty string.
	 */
	public String getRequestBody() {
		return ServletUtils.getRequestBody(httpRequest);
	}
	
	@Override
	public String toString() {
		return getMethod() + " " + getPath();
	}
}
