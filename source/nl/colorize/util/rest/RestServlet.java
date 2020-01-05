//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import nl.colorize.util.http.Method;
import nl.colorize.util.http.URLResponse;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes a REST API through a servlet. Mapping requests to services is handled
 * by a {@link RestRequestDispatcher}. This class is abstract, it needs to be 
 * configured by extending it and implementing the abstract methods.
 * <p>
 * This class also acts as a servlet filter, so that it can easily be used within
 * Spring Boot application. Of course, Spring also provides its own REST API
 * framework, but this is mainly provides for backward compatibility for
 * applications that were already using the Colorize REST API framework and have
 * not yet migrated to Spring.
 */
public abstract class RestServlet extends HttpServlet implements Filter, AuthorizationCheck {
    
    private RestRequestDispatcher requestDispatcher;
    
    public static final Charset REQUEST_CHARSET = Charsets.UTF_8;

    /**
     * Registers all methods from {@link #getServiceObjects()} annotated with 
     * {@link Rest} as services.
     * @throws IllegalArgumentException if one of the annotated methods cannot
     *         be used as service.
     * @throws IllegalStateException if multiple services are attempting to use
     *         the same path and method, or when no services were registered.
     */
    @Override
    public final void init() {
        requestDispatcher = new RestRequestDispatcher(this, getDefaultResponseHeaders());
        for (Object serviceObject : getServiceObjects()) {
            requestDispatcher.registerServices(serviceObject);
        }
    }

    @Override
    public final void init(FilterConfig config) {
        init();
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

    @Override
    public final void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(request, response);
    }
    
    @Override
    public final void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(request, response);
    }
    
    @Override
    public final void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(request, response);
    }
    
    @Override
    public final void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(request, response);
    }
    
    @Override
    public final void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(request, response);
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            handleRequest((HttpServletRequest) request, (HttpServletResponse) response);
        } else {
            chain.doFilter(request, response);
        }
    }

    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        URLResponse serviceResponse = dispatchRequest(request);
        fillServletResponse(serviceResponse, response);
    }

    private void fillServletResponse(URLResponse source, HttpServletResponse dest) throws IOException {
        dest.setStatus(source.getStatus().getCode());

        MediaType contentType = source.getContentType(MediaType.PLAIN_TEXT_UTF_8);
        dest.setContentType(contentType.withoutParameters().toString());
        dest.setCharacterEncoding(source.getEncoding().displayName());

        for (String header : source.getHeaderNames()) {
            // The Content-Type is a special case, because it's already set
            // using HttpServletResponse.setContentType().
            if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(header)) {
                for (String value : source.getHeaderValues(header)) {
                    dest.addHeader(header, value);
                }
            }
        }

        PrintWriter responseWriter = dest.getWriter();
        responseWriter.print(source.getBody());
        responseWriter.flush();
        responseWriter.close();
    }

    /**
     * Attempts to dispatch a request to one of the registered services. This
     * method delegates to {@link RestRequestDispatcher#dispatch(RestRequest)}.
     */
    protected URLResponse dispatchRequest(HttpServletRequest httpRequest) {
        String path = ServletUtils.getRequestPath(httpRequest);
        if (path.startsWith(getRequestPrefix())) {
            path = path.substring(getRequestPrefix().length());
        }

        Method method = Method.parse(httpRequest.getMethod());
        RestRequest restRequest = new RestRequest(httpRequest, method, path);
        extractRequestHeaders(httpRequest, restRequest);
        extractRequestBody(httpRequest, restRequest);

        return requestDispatcher.dispatch(restRequest);
    }

    private void extractRequestHeaders(HttpServletRequest httpRequest, RestRequest result) {
        Enumeration<String> headerNames = httpRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = httpRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                result.addHeader(headerName, headerValue);
            }
        }
    }

    /**
     * Reads the request body and returns it as a string. If the request contains
     * no body, or if the request method does not support a request body to be
     * sent, this method will return an empty string.
     */
    private void extractRequestBody(HttpServletRequest httpRequest, RestRequest result) {
        if (!result.getMethod().hasRequestBody()) {
            return;
        }

        try {
            BufferedReader requestBody = httpRequest.getReader();
            String body = CharStreams.toString(requestBody);
            result.setBody(body);
        } catch (IOException e) {
            throw new InternalServerException("Unexpected read error while reading request body", e);
        }
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

    public abstract String getRequestPrefix();
}
