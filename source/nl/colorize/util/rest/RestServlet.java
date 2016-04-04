//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import nl.colorize.util.LogHelper;
import nl.colorize.util.ReflectionUtils;
import nl.colorize.util.TextUtils;
import nl.colorize.util.http.HttpResponse;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.Method;

/**
 * Exposes a number of REST services through a servlet. Incoming requests are
 * dispatched to one of the registered service's methods annotated with
 * {@link Rest}, and turns it into a response. This framework follows the REST
 * conventions as described in the book "REST API Design Rules".
 * <p>
 * Services are registered by overriding {@link #getServiceObjects()}. No
 * classpath scanning is performed, which means that this class is fully
 * compatible with Google App Engine.
 */
public abstract class RestServlet extends HttpServlet {

	private List<Mapping> serviceMappings;
	
	public static final Charset REQUEST_CHARSET = Charsets.UTF_8;
	private static final Splitter PATH_SPLITTER = Splitter.on('/').omitEmptyStrings();
	private static final Joiner HEADER_JOINER = Joiner.on(", ");
	private static final Logger LOGGER = LogHelper.getLogger(RestServlet.class);
	
	@Override
	public void init() throws ServletException {
		registerServices();
	}
	
	/**
	 * Registers all methods from {@link #getServiceObjects()} annotated with 
	 * {@link Rest} as services.
	 * @throws IllegalArgumentException if one of the annotated methods cannot
	 *         be used as service.
	 * @throws IllegalStateException if multiple services are attempting to use
	 *         the same path and method, or when no services were registered.
	 */
	private void registerServices() {
		List<?> serviceObjects = getServiceObjects();
		serviceMappings = new CopyOnWriteArrayList<Mapping>();
		
		for (Object serviceObject : serviceObjects) {
			for (java.lang.reflect.Method method : ReflectionUtils.getMethodsWithAnnotation(
					serviceObject, Rest.class)) {
				Mapping mapping = new Mapping(serviceObject, method, method.getAnnotation(Rest.class));
				registerMapping(mapping);
			}
		}
		
		if (serviceMappings.isEmpty()) {
			throw new IllegalStateException("No services were registered");
		}
	}
	
	private void registerMapping(Mapping mapping) {
		verifyMapping(mapping);
		verifyMappingCollision(mapping);
		
		serviceMappings.add(mapping);
	}
	
	/**
	 * Registers a service and maps it using the provided configuration. The
	 * preferred approach for registering services is by adding objects to
	 * {@link #getServiceObjects()}, which are automatically registered when
	 * the servlet is intialized. This method should only be used if services
	 * (for whatever reason) need to be registered dynamically. 
	 * @throws IllegalArgumentException if the service does not exist or cannot
	 *         be used.
	 * @throws IllegalStateException if another service is already mapped to the
	 *         same path and uses the same request method.
	 */
	@VisibleForTesting
	protected void registerMapping(Object serviceObject, String methodName, Rest config) {
		try {
			Mapping mapping = new Mapping(serviceObject, 
					serviceObject.getClass().getMethod(methodName, RestRequest.class), config);
			registerMapping(mapping);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(methodName + "(RestRequest) not found", e);
		}
	}
	
	/**
	 * Registers a service mapping for the specified method.
	 * @deprecated Use {@link #registerMapping(Object, String, Rest)} instead.
	 */
	@Deprecated
	@VisibleForTesting
	void registerServiceMethod(Object owner, java.lang.reflect.Method serviceMethod, 
			Rest annotation) throws ServletException {
		Mapping mapping = new Mapping(owner,serviceMethod, annotation);
		registerMapping(mapping);
	}

	private void verifyMapping(Mapping mapping) {
		Class<?>[] parameters = mapping.service.getParameterTypes();
		if (parameters.length != 1 || parameters[0] != RestRequest.class) {
			throw new IllegalArgumentException("Method is annotated with @Rest, but " + 
					"has invalid parameter type: " + mapping.service.getName());
		}
		
		if (mapping.service.getReturnType() != HttpResponse.class) {
			throw new IllegalArgumentException("Method is annotated with @Rest, but " +
					"does not have URLResponse return type: " + mapping.service.getName());
		}
		
		if (!mapping.config.path().startsWith("/")) {
			throw new IllegalArgumentException("Method is annotated with @Rest, " +
					"path must have a leading slash: " + mapping.service.getName());
		}
	}
	
	private void verifyMappingCollision(Mapping mapping) {
		if (findMappingsForPath(mapping.config.path()).containsKey(mapping.config.method())) {
			throw new IllegalStateException("Mapping already exists: " + 
					mapping.config.method() + " " + mapping.config.path());
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
	
	/**
	 * Returns the servlet mapping that has been registered for the specified
	 * request. Returns {@code null} if no such mapping exists. This method
	 * can be used to programatically access services.
	 */
	@VisibleForTesting
	public Function<RestRequest, HttpResponse> mapping(RestRequest request) {
		return mapping(request.getMethod(), request.getPath());
	}
	
	/**
	 * Returns the servlet mapping that has been registered for the specified
	 * HTTP method and path. Returns {@code null} if no such mapping exists.
	 * This method can be used to programatically access services. 
	 */
	@VisibleForTesting
	public Function<RestRequest, HttpResponse> mapping(Method method, String path) {
		return findMappingsForPath(path).get(method);
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
		handlePreflightedRequest(request, response);
	}

	private void handleRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
			throws ServletException, IOException {
		RestRequest serviceRequest = parseRequest(httpRequest);
		HttpResponse serviceResponse = dispatchRequest(serviceRequest);
		sendResponse(serviceResponse, httpResponse);
	}
	
	/**
	 * Parses the incoming HTTP request into a {@link RestRequest} instance.
	 */
	protected RestRequest parseRequest(HttpServletRequest httpRequest) {
		RestRequest restRequest = new RestRequest(httpRequest, extractPathComponents(httpRequest));
		restRequest.bindParameters(parseRequestParameters(httpRequest));
		return restRequest;
	}
	
	private List<String> extractPathComponents(HttpServletRequest request) {
		String requestPath = request.getRequestURI();
		String servletPath = request.getServletPath();
		if (servletPath.length() > 1 && requestPath.startsWith(servletPath)) {
			requestPath = requestPath.substring(servletPath.length());
		}
		
		int queryStringIndex = requestPath.indexOf('?');
		if (queryStringIndex != -1) {
			requestPath = requestPath.substring(0, queryStringIndex);
		}
		
		return extractPathComponents(requestPath);
	}
	
	private List<String> extractPathComponents(String path) {
		if (path == null || path.isEmpty() || path.equals("/")) {
			return Collections.emptyList();
		}
		path = TextUtils.removeTrailing(path, "/");
		return PATH_SPLITTER.splitToList(path);
	}
	
	/**
	 * Parses the request parameters (name/value pairs) present in the request
	 * body. The default implementation of this method supports both the "classic"
	 * POST data (with a Content-Type of "application/x-www-form-urlencoded") as
	 * well as JSON objects (with a Content-Type of "application/json"). Subclasses
	 * can override this method to support additional request parameter formats. 
	 */
	protected Map<String, String> parseRequestParameters(HttpServletRequest request) {
		Method method = Method.parse(request.getMethod());
		if (!method.hasRequestBody()) {
			return Collections.emptyMap();
		}
		
		String contentType = request.getContentType();
		if (contentType != null && contentType.contains("application/json")) {
			String requestBody = ServletUtils.getRequestBody(request);
			return parseJsonRequestParameters(requestBody);
		} else {
			return ServletUtils.getParameterMap(request, REQUEST_CHARSET);
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
	
	/**
	 * Parses the parameter values present in the request path, by comparing it
	 * to the path in the service definition. The supported syntax for path
	 * parameters is documented in {@link Rest#path()}.
	 */
	protected Map<String, String> parsePathParameters(RestRequest request, Rest serviceConfig) {
		List<String> requestPathComponents = request.getPathComponents();
		List<String> configPathComponents = extractPathComponents(serviceConfig.path());
		Map<String, String> pathParams = new HashMap<String, String>();
		
		for (int i = 0; i < configPathComponents.size(); i++) {
			String pathParamName = extractPathParameterName(configPathComponents.get(i));
			if (pathParamName != null) {
				pathParams.put(pathParamName, requestPathComponents.get(i));
			}
		}

		return pathParams;
	}
	
	private String extractPathParameterName(String pathComponent) {
		if (pathComponent.startsWith("{") && pathComponent.endsWith("}")) {
			return pathComponent.substring(1, pathComponent.length() - 1);
		} else if (pathComponent.startsWith(":")) {
			return pathComponent.substring(1);
		} else if (pathComponent.startsWith("@")) {
			return pathComponent.substring(1);
		} else {
			return null;
		}
	}

	/**
	 * Attempts to dispatch a request to one of the registered services. This 
	 * method will return a response with one of the following HTTP statuses:
	 * 
	 * <ul>
	 *   <li>400 if one or more required parameters are not set</li>
	 *   <li>404 if no service accepts the request</li>
	 *   <li>401 if the request fails authorization</li>
	 *   <li>405 if the service is called with the wrong request method</li>
	 *   <li>500 if an exception occurs while dispatching the request</li>
	 *   <li>The response of the matched service, if none of the above apply</li>
	 * </ul>
	 */
	protected HttpResponse dispatchRequest(RestRequest request) {
		Map<Method, Mapping> mappingsForPath = findMappingsForPath(request.getPath());
		Mapping matchingMapping = mappingsForPath.get(request.getMethod());
		
		if (mappingsForPath.isEmpty()) {
			return createEmptyResponse(HttpStatus.NOT_FOUND);
		} else if (matchingMapping == null) {
			return createEmptyResponse(HttpStatus.METHOD_NOT_ALLOWED);
		} else {
			return dispatchRequestTo(request, matchingMapping);
		}
	}
	
	private HttpResponse dispatchRequestTo(RestRequest request, Mapping mapping) {
		// Bind the request parameters based on the service configuration, now 
		// that we know for sure that the request was intended for this service.
		request.bindPathParameters(parsePathParameters(request, mapping.config));
		
		if (!isRequestAuthorized(request, mapping.config.authorized())) {
			return createEmptyResponse(HttpStatus.UNAUTHORIZED);
		}
		
		try {
			return callService(request, mapping);
		} catch (BadRequestException e) {
			return createEmptyResponse(HttpStatus.BAD_REQUEST);
		} catch (InternalServerException e) {
			LOGGER.log(Level.SEVERE, "Internal exception while handling service request", e);
			return createEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private Map<Method, Mapping> findMappingsForPath(String path) {
		Map<Method, Mapping> matches = new HashMap<Method, Mapping>();
		for (Mapping mapping : serviceMappings) {
			if (isMatchingPath(path, mapping.config.path())) {
				matches.put(mapping.config.method(), mapping);
			}
		}
		return matches;
	}
	
	private HttpResponse createEmptyResponse(HttpStatus status) {
		Map<String, String> headers = ImmutableMap.of(HttpHeaders.CONTENT_TYPE, "text/plain");
		return new HttpResponse(status, headers, "");
	}
	
	private boolean isMatchingPath(String firstPath, String secondPath) {
		return isMatchingPath(extractPathComponents(firstPath), extractPathComponents(secondPath));
	}

	private boolean isMatchingPath(List<String> firstPath, List<String> secondPath) {
		if (firstPath.size() != secondPath.size()) {
			return false;
		}
		
		for (int i = 0; i < firstPath.size(); i++) {
			if (!isMatchingPathComponent(firstPath.get(i), secondPath.get(i))) {
				return false;
			}
		}
		
		return true;
	}

	private boolean isMatchingPathComponent(String firstPathComponent, String secondPathComponent) {
		return firstPathComponent.equals(secondPathComponent) || 
				isPathParameter(firstPathComponent) || 
				isPathParameter(secondPathComponent);
	}
	
	private boolean isPathParameter(String pathComponent) {
		return extractPathParameterName(pathComponent) != null;
	}
	
	/**
	 * Returns whether a request is authorized to call a service. Subclasses
	 * should override this method based on whatever authentication/authorization
	 * mechanism is used by the REST API.
	 * @param authorizedRoles The roles authorized to access the service, as
	 *        indicated by {@link Rest#authorized()}.
	 */
	protected abstract boolean isRequestAuthorized(RestRequest request, String authorizedRoles);

	private HttpResponse callService(RestRequest request, Mapping mapping) {
		Exception thrown = null;
		try {
			return (HttpResponse) mapping.service.invoke(mapping.owner, request);
		} catch (Exception e) {
			thrown = e;
		}
		
		if (isCausedByInvalidParameters(thrown)) {
			throw new BadRequestException("Service request has invalid parameters", thrown);
		} else {
			throw new InternalServerException("Exception while handling service request", thrown);
		}
	}

	private void sendResponse(HttpResponse serviceResponse, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		Map<String, String> mergedHeaders = mergeHeaders(serviceResponse);
		HttpResponse mergedResponse = new HttpResponse(serviceResponse.getStatus(), mergedHeaders, 
				serviceResponse.getBodyText());
		
		ServletUtils.fillServletResponse(mergedResponse, servletResponse);
	}
	
	private Map<String, String> mergeHeaders(HttpResponse serviceResponse) {
		Map<String, String> mergedHeaders = new LinkedHashMap<String, String>();
		for (String header : serviceResponse.getHeaderNames()) {
			mergedHeaders.put(header, serviceResponse.getHeader(header));
		}
		if (serviceResponse.getHeader(HttpHeaders.CACHE_CONTROL) == null) {
			mergedHeaders.put(HttpHeaders.CACHE_CONTROL, "no-cache");
		}
		for (Map.Entry<String, String> header : getAccessControlHeaders().entrySet()) {
			mergedHeaders.put(header.getKey(), header.getValue());
		}
		return mergedHeaders;
	}

	private void handlePreflightedRequest(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		HttpResponse ok = new HttpResponse(HttpStatus.OK, getAccessControlHeaders(), "");
		sendResponse(ok, response);
	}
	
	private boolean isCausedByInvalidParameters(Exception thrown) {
		Exception cause = thrown;
		if (cause instanceof InvocationTargetException) {
			cause = (Exception) thrown.getCause();
		}
		
		return cause instanceof BadRequestException;
	}
	
	/**
	 * Returns the response headers that will be added for CORS requests from 
	 * other domains. These headers are used by the browser sending the request
	 * to determine if the request should be allowed. The same headers are 
	 * returned for both the "preflighted" request and the actual request. See
	 * https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS
	 * for more information on CORS.
	 * <p>
	 * The default implementation will allow all requests, regardless of their
	 * origin. However, subclasses can override {@link #getAllowedOrigin()}
	 * and {@link #getAllowedHeaders()} to introduce additional restrictions.
	 */
	protected final Map<String, String> getAccessControlHeaders() {
		Map<String, String> headers = new LinkedHashMap<String, String>();
		headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, getAllowedOrigin());
		headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
		headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HEADER_JOINER.join(getAllowedHeaders()));
		return headers;
	}
	
	/**
	 * Returns the allowed origins for CORS requests. The default implementation
	 * allows requests from all domains, but subclasses can override this to
	 * restrict CORS requests to a specific domain (e.g. "http://colorize.nl").
	 */
	protected String getAllowedOrigin() {
		return "*";
	}
	
	/**
	 * Returns the list of request headers that is allowed for CORS requests.
	 * The default implementation only allows the "Content-Type" and "Accept"
	 * headers, but subclasses can override this method to allow other headers.
	 */
	protected List<String> getAllowedHeaders() {
		List<String> allowedHeaders = new ArrayList<String>();
		allowedHeaders.add(HttpHeaders.CONTENT_TYPE);
		allowedHeaders.add(HttpHeaders.ACCEPT);
		return allowedHeaders;
	}
	
	/**
	 * Returns whether a {@code Cache-Control: no-cache} response header should 
	 * be added. By default this returns false, but this can be overridden by
	 * subclasses. Note that manually setting a {@code Cache-Control} header in
	 * a service response will take precedence over this default setting.
	 * <p>
	 * Be aware that iOS will cache <i>all</i> requests, even POST requests, if
	 * this header is not present.
	 */
	protected boolean shouldAddNoCacheHeader() {
		return true;
	}
	
	/**
	 * Maps a method to an URL so that it can be called from the REST API. The 
	 * mapping is typically obtained from the {@link Rest} annotation.
	 */
	private static class Mapping implements Function<RestRequest, HttpResponse> {

		private Object owner;
		private java.lang.reflect.Method service;
		private Rest config;
		
		public Mapping(Object owner, java.lang.reflect.Method service, Rest config) {
			this.owner = owner;
			this.service = service;
			this.config = config;
		}

		public HttpResponse apply(RestRequest request) {
			try {
				return (HttpResponse) service.invoke(owner, request);
			} catch (Exception e) {
				throw new InternalServerException("Exception while handling request", e);
			}
		}
	}
}
