//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import nl.colorize.util.Escape;
import nl.colorize.util.http.HttpRequest;
import nl.colorize.util.http.Method;

/**
 * Describes a request made to a REST API. This class contains information about
 * the service to which the request was made, as well as providing access to
 * the underlying {@link javax.servlet.http.HttpServletRequest}.
 */
public class RestRequest extends HttpRequest {
	
	private List<String> pathComponents;
	
	private Map<String, String> pathParameters;
	private Map<String, String> urlParameters;
	private Map<String, String> parameters;
	private Map<String, Object> infoMap;
	
	private static final Joiner PATH_JOINER = Joiner.on("/");
	
	protected RestRequest(Method method, List<String> pathComponents, Map<String, String> headers, 
			String body) {
		super(method, "/" + PATH_JOINER.join(pathComponents), headers, body);
		
		this.pathComponents = ImmutableList.copyOf(pathComponents);
		
		pathParameters = Collections.emptyMap();
		urlParameters = Collections.emptyMap();
		parameters = Collections.emptyMap();
		infoMap = Collections.emptyMap();
	}
	
	/**
	 * Returns the path components for the path to which this request was
	 * made. The path is relative to the REST API, and does not include the
	 * host name, servlet path, or query string.
	 * <p>
	 * In most cases it is more common to use the path in textual form, as
	 * returned by {@link #getPath()}, instead of using the path components
	 * directly.
	 */
	public List<String> getPathComponents() {
		return pathComponents;
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
	 * Returns the (URL-decoded) parameter from the request's query string with 
	 * the specified name. 
	 * @throws BadRequestException if no path parameter with that name exists.
	 */
	public String getRequiredUrlParameter(String name) {
		String value = urlParameters.get(name);
		if (value == null || value.isEmpty()) {
			throw new BadRequestException("Missing required URL parameter: " + name);
		}
		return value;
	}
	
	/**
	 * Returns the (URL-decoded) parameter from the request's query string with 
	 * the specified name, or {@code defaultValue} if no parameter with that name 
	 * exists. 
	 */
	public String getOptionalUrlParameter(String name, String defaultValue) {
		String value = urlParameters.get(name);
		if (value == null || value.isEmpty()) {
			return defaultValue;
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
	public Map<String, String> getParameters() {
		return parameters;
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
	 * Returns other information associated with this request. The contents
	 * of the info map will depend on the current web app environment, which
	 * means some of the information may not be available when the application
	 * is used in another environment.
	 */
	public Map<String, Object> getInfoMap() {
		return infoMap;
	}
	
	protected void bindPathParameters(Map<String, String> pathParameters) {
		this.pathParameters = ImmutableMap.copyOf(pathParameters);
	}
	
	protected void bindUrlParameters(Map<String, String> urlParameters) {
		this.urlParameters = ImmutableMap.copyOf(urlParameters);
	}
	
	protected void bindParameters(Map<String, String> parameters) {
		// Filter out parameters with null values, which according to the
		// HttpServletRequest documentation should be considered as if the
		// parameter is not present. Parameters will null *keys* are invalid
		// though, and will throw an exception when encountered.
		Predicate<String> parameterFiler = Predicates.notNull();
		this.parameters = ImmutableMap.copyOf(Maps.filterValues(parameters, parameterFiler));
	}
	
	protected void bindInfoMap(Map<String, Object> infoMap) {
		this.infoMap = ImmutableMap.copyOf(infoMap);
	}
}
