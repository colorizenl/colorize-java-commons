//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.ReflectionUtils;
import nl.colorize.util.http.HttpResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        requestDispatcher = new RestRequestDispatcher(this, getDefaultResponseHeaders(),
                isParseRequestBodyEnabled());
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
        HttpResponse serviceResponse = dispatchRequest(httpRequest);
        ServletUtils.fillServletResponse(serviceResponse, httpResponse);
    }

    /**
     * Attempts to dispatch a request to one of the registered services. This
     * method delegates to {@link RestRequestDispatcher#dispatch(RestRequest)}.
     */
    protected HttpResponse dispatchRequest(HttpServletRequest httpRequest) {
        RestRequest restRequest = new RestRequest(httpRequest);
        return requestDispatcher.dispatch(restRequest);
    }
    
    @Override
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
        headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Accept");
        return headers;
    }
    
    protected boolean isParseRequestBodyEnabled() {
        return true;
    }
}
