//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import nl.colorize.util.Escape;
import nl.colorize.util.ReflectionUtils;
import nl.colorize.util.http.HttpResponse;
import nl.colorize.util.http.Method;

/**
 * Exposes a REST API through a servlet. Mapping requests to services is handled
 * by a {@link RestRequestDispatcher}. This class is abstract, it needs to be 
 * configured by extending it and implementing the abstract methods. 
 */
public abstract class RestServlet extends HttpServlet implements AuthorizationCheck {
	
	private RestRequestDispatcher requestDispatcher;
	
	public static final Charset REQUEST_CHARSET = Charsets.UTF_8;
	public static final String INFO_REQUEST = "REQUEST";
	public static final String INFO_REMOTE_ADDRESS = "REMOTE_ADDRESS";
	
	/**
	 * Registers all methods from {@link #getServiceObjects()} annotated with 
	 * {@link Rest} as services.
	 * @throws IllegalArgumentException if one of the annotated methods cannot
	 *         be used as service.
	 * @throws IllegalStateException if multiple services are attempting to use
	 *         the same path and method, or when no services were registered.
	 */
	@Override
	public void init() throws ServletException {
		requestDispatcher = new RestRequestDispatcher(this, getDefaultResponseHeaders());
		for (Object serviceObject : getServiceObjects()) {
			requestDispatcher.registerServices(serviceObject);
		}
	}

	/**
	 * Returns all objects that should be registered as REST API services.
	 * Incoming requests can be dispatched to any matching methods that have
	 * been annotated with {@link Rest}.
	 * <p>
	 * Note that the provided instances might be used for concurrent requests,
	 * and are assumed to be stateless and thread-safe.
	 */
	protected abstract List<?> getServiceObjects();
	
	@VisibleForTesting
	protected void registerServiceMethod(Object subject, java.lang.reflect.Method service, Rest config) {
		requestDispatcher.registerService(ReflectionUtils.toMethodCallback(
				subject, service, RestRequest.class, HttpResponse.class), config);
	}

	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		handleRequest(request, response);
	}
	
	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		handleRequest(request, response);
	}
	
	@Override
	protected final void doPut(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		handleRequest(request, response);
	}
	
	@Override
	protected final void doDelete(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		handleRequest(request, response);
	}
	
	@Override
	protected final void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		handleRequest(request, response);
	}

	private void handleRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
			throws ServletException, IOException {
		RestRequest serviceRequest = parseRequest(httpRequest);
		HttpResponse serviceResponse = dispatchRequest(serviceRequest);
		ServletUtils.fillServletResponse(serviceResponse, httpResponse);
	}
	
	protected RestRequest parseRequest(HttpServletRequest httpRequest) {
		String requestBody = ServletUtils.getRequestBody(httpRequest);
		RestRequest restRequest = new RestRequest(Method.parse(httpRequest.getMethod()), 
				requestDispatcher.extractPathComponents(ServletUtils.getRequestPath(httpRequest)), 
				ServletUtils.getRequestHeaders(httpRequest),
				requestBody);
		restRequest.bindParameters(parseRequestBody(httpRequest, requestBody));
		restRequest.bindUrlParameters(parseUrlParameters(httpRequest));
		restRequest.bindInfoMap(createInfoMap(httpRequest));
		return restRequest;
	}
	
	/**
	 * Parses the request parameters (name/value pairs) present in the request
	 * body. The default implementation of this method supports both the "classic"
	 * POST data (with a Content-Type of "application/x-www-form-urlencoded") as
	 * well as JSON objects (with a Content-Type of "application/json"). Subclasses
	 * can override this method to support additional request parameter formats. 
	 */
	protected Map<String, String> parseRequestBody(HttpServletRequest request, String requestBody) {
		Method method = Method.parse(request.getMethod());
		if (!method.hasRequestBody()) {
			return Collections.emptyMap();
		}
		
		String contentType = request.getContentType();
		if (contentType != null && contentType.contains("application/json")) {
			return parseJsonRequestParameters(requestBody);
		} else {
			return Escape.formDecode(requestBody, REQUEST_CHARSET);
		}
	}

	private Map<String, String> parseJsonRequestParameters(String requestBody) {
		JsonParser jsonParser = new JsonParser();
		JsonElement json = jsonParser.parse(requestBody);
		Map<String, String> requestParameters = new HashMap<String, String>();
		if (json instanceof JsonObject) {
			for (Map.Entry<String, JsonElement> entry : ((JsonObject) json).entrySet()) {
				if (entry.getValue() instanceof JsonPrimitive) {
					requestParameters.put(entry.getKey(), entry.getValue().getAsString());
				} else {
					requestParameters.put(entry.getKey(), entry.getValue().toString());
				}
			}
		}
		return requestParameters;
	}
	
	private Map<String, String> parseUrlParameters(HttpServletRequest request) {
		if (request.getQueryString() == null) {
			return Collections.emptyMap();
		}
		return Escape.formDecode(request.getQueryString(), REQUEST_CHARSET);
	}
	
	private Map<String, Object> createInfoMap(HttpServletRequest httpRequest) {
		return ImmutableMap.of(
				INFO_REQUEST, httpRequest,
				INFO_REMOTE_ADDRESS, httpRequest.getRemoteAddr());
	}

	/**
	 * Attempts to dispatch a request to one of the registered services. This
	 * method delegates to {@link RestRequestDispatcher#dispatch(RestRequest)}. 
	 */
	protected HttpResponse dispatchRequest(RestRequest request) {
		return requestDispatcher.dispatch(request);
	}
	
	public boolean isRequestAuthorized(RestRequest request, Rest serviceConfig) {
		return isRequestAuthorized(request, serviceConfig.authorized());
	}
	
	/**
	 * Returns whether a request is authorized to call a service. Subclasses
	 * should override this method based on whatever authentication/authorization
	 * mechanism is used by the REST API.
	 * @param authorizedRoles The roles authorized to access the service, as
	 *        indicated by {@link Rest#authorized()}.
	 */
	protected abstract boolean isRequestAuthorized(RestRequest request, String authorizedRoles);
	
	/**
	 * Returns the default HTTP response headers that should be added to each 
	 * request. The default implementation for this method sets the 
	 * {@code Cache-Control} and {@code Access-Control-X} headers, but this
	 * list can be extended or changed by subclasses. 
	 */
	protected Map<String, String> getDefaultResponseHeaders() {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(HttpHeaders.CACHE_CONTROL, "no-cache");
		headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
		headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		return headers;
	}
}
