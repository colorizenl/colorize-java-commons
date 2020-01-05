//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.LogHelper;
import nl.colorize.util.ReflectionUtils;
import nl.colorize.util.TextUtils;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.Method;
import nl.colorize.util.http.URLResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes incoming requests to a REST API and dispatches them to one of the
 * registered services. This class provides a base implementation that still
 * needs to be connected to a web app environment. For example, the
 * {@link RestServlet} class implements a request dispatcher that can expose
 * REST services through a servlet.
 */
public class RestRequestDispatcher {
    
    private List<MappedService> mappedServices;
    private AuthorizationCheck authorizationCheck;
    private Map<String, String> defaultResponseHeaders;

    private static final Splitter PATH_SPLITTER = Splitter.on('/').omitEmptyStrings();
    private static final Logger LOGGER = LogHelper.getLogger(RestServlet.class);
    
    /**
     * Creates a new request dispatcher that will dispatch requests to one of the
     * registered REST services.
     * @param authorizationCheck Authorization check performed on all incoming
     *        request. Failing this check will result in a response with HTTP
     *        status 401 (Unauthorized).
     * @param defaultResponseHeaders HTTP headers that will be added to each 
     *        response. If the service itself also sets one of these headers, 
     *        the header value set by the service overrules the default value.
     */
    public RestRequestDispatcher(AuthorizationCheck authorizationCheck, 
            Map<String, String> defaultResponseHeaders) {
        this.mappedServices = new CopyOnWriteArrayList<>();
        this.authorizationCheck = authorizationCheck;
        this.defaultResponseHeaders = ImmutableMap.copyOf(defaultResponseHeaders);
    }
    
    public void registerServices(Object serviceObject) {
        for (java.lang.reflect.Method method : ReflectionUtils.getMethodsWithAnnotation(
                serviceObject, Rest.class)) {
            MappedService mappedService = new MappedService(ReflectionUtils.toMethodCallback(
                serviceObject, method, RestRequest.class, URLResponse.class),
                method.getAnnotation(Rest.class));

            registerService(mappedService);
        }
    }
    
    public void registerService(Function<RestRequest, URLResponse> service, Rest config) {
        MappedService mappedService = new MappedService(service, config);
        registerService(mappedService);
    }
    
    private void registerService(MappedService mappedService) {
        if (!mappedService.config.path().startsWith("/")) {
            throw new IllegalArgumentException("Method is annotated with @Rest, " +
                    "path must have a leading slash: " + mappedService.toString());
        }
        
        if (getMappedServices(mappedService.config.path()).containsKey(mappedService.config.method())) {
            throw new IllegalStateException("Mapping already exists: " + 
                    mappedService.config.method() + " " + mappedService.config.path());
        }
        
        mappedServices.add(mappedService);
    }

    /**
     * Attempts to dispatches an incoming request to one of the registered
     * services. This will return a response with one of the following HTTP 
     * status codes:
     * <ul>
     *   <li>400 if one or more required parameters are not set</li>
     *   <li>401 if the request fails authorization</li>
     *   <li>404 if no service accepts the request</li>
     *   <li>405 if the service is called with the wrong request method</li>
     *   <li>406 if the service does not support the request's Accept header</li>
     *   <li>500 if an exception occurs while dispatching the request</li>
     *   <li>The response of the matched service, if none of the above apply</li>
     * </ul>
     */
    public URLResponse dispatch(RestRequest request) {
        Map<Method, MappedService> mappedForPath = getMappedServices(request.getPath());
        MappedService mappedForMethod = getMappedService(mappedForPath, request.getMethod());
        
        URLResponse response = null;
        if (mappedForPath.isEmpty()) {
            response = createEmptyResponse(HttpStatus.NOT_FOUND);
        } else if (request.getMethod() == Method.OPTIONS) {
            response = handlePreflightedRequest();
        } else if (mappedForMethod == null) {
            response = createEmptyResponse(HttpStatus.METHOD_NOT_ALLOWED);
        } else {
            response = dispatch(request, mappedForMethod);
        }
        
        return processResponse(response);
    }
    
    private URLResponse dispatch(RestRequest request, MappedService mappedService) {
        bindRequest(request, mappedService.config);
        
        if (!authorizationCheck.isRequestAuthorized(request, mappedService.config)) {
            return createEmptyResponse(HttpStatus.UNAUTHORIZED);
        }
        
        try {
            return callService(request, mappedService);
        } catch (BadRequestException e) {
            return createEmptyResponse(HttpStatus.BAD_REQUEST);
        } catch (InternalServerException e) {
            LOGGER.log(Level.SEVERE, "Internal exception while handling service request", e);
            return createEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    protected void bindRequest(RestRequest request, Rest config) {
        List<String> pathComponents = extractPathComponents(request.getPath());
        Map<String, String> pathParameters = parsePathParameters(request, config);

        request.bindPath(pathComponents, pathParameters);
    }

    private URLResponse callService(RestRequest boundRequest, MappedService mappedService) {
        Exception thrown = null;
        try {
            return mappedService.apply(boundRequest);
        } catch (Exception e) {
            thrown = e;
        }
        
        if (isCausedByInvalidParameters(thrown)) {
            throw new BadRequestException("Service request has invalid parameters", thrown);
        } else {
            throw new InternalServerException("Exception while handling service request", thrown);
        }
    }
    
    private URLResponse processResponse(URLResponse response) {
        Map<String, String> mergedHeaders = new LinkedHashMap<>();
        mergedHeaders.putAll(defaultResponseHeaders);
        for (String header : response.getHeaderNames()) {
            for (String value : response.getHeaderValues(header)) {
                mergedHeaders.put(header, value);
            }
        }

        URLResponse result = new URLResponse(response.getStatus(), response.getBody(),
            response.getEncoding());
        result.addHeaders(mergedHeaders);
        return result;
    }
    
    /**
     * Returns the service(s) that has been mapped to handle requests to the
     * specified path. Note that multiple services can map to the same path, 
     * as long as they handle different request methods.
     */
    private Map<Method, MappedService> getMappedServices(String path) {
        Map<Method, MappedService> matches = new HashMap<>();

        for (MappedService mappedService : mappedServices) {
            if (isMatchingPath(path, mappedService.config.path())) {
                matches.put(mappedService.config.method(), mappedService);
            }
        }

        return matches;
    }
    
    private MappedService getMappedService(Map<Method, MappedService> mappedForPath, Method method) {
        MappedService mapped = mappedForPath.get(method);
        if (mapped == null) {
            mapped = mappedForPath.get(null);
        }
        return mapped;
    }
    
    private boolean isCausedByInvalidParameters(Exception thrown) {
        Exception cause = thrown;
        while (cause != null && cause.getCause() instanceof Exception) {
            cause = (Exception) cause.getCause();
            if (cause instanceof BadRequestException) {
                return true;
            }
        }
        return false;
    }
    
    private URLResponse createEmptyResponse(HttpStatus status) {
        URLResponse response = new URLResponse(status, new byte[0], Charsets.UTF_8);
        response.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        return response;
    }
    
    /**
     * Handles incoming "preflighted" requests that are sent by browsers to
     * check if cross-domain requests are allowed. See
     * https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS
     * for more information on CORS (Cross Origin Resource Sharing).
     */
    private URLResponse handlePreflightedRequest() {
        return createEmptyResponse(HttpStatus.OK);
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
     * Parses the parameter values present in the request path, by comparing it
     * to the path in the service definition. The supported syntax for path
     * parameters is documented in {@link Rest#path()}.
     */
    protected Map<String, String> parsePathParameters(RestRequest request, Rest serviceConfig) {
        List<String> requestPathComponents = extractPathComponents(request.getPath());
        List<String> configPathComponents = extractPathComponents(serviceConfig.path());
        Map<String, String> pathParams = new HashMap<>();
        
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
     * Extracts the path components from the specified request path. For example,
     * a request made to "/a/b?c=2" will produce ["a", "b"]. Requests made to the
     * root (i.e. with a path of "/") will return an empty list.
     */
    protected List<String> extractPathComponents(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return Collections.emptyList();
        }
        
        path = TextUtils.removeTrailing(path, "/");
        
        int queryStringIndex = path.indexOf('?');
        if (queryStringIndex != -1) {
            path = path.substring(0, queryStringIndex);
        }
        
        return PATH_SPLITTER.splitToList(path);
    }

    /**
     * Represents a REST service that can handle requests based on the configuration
     * in the {@link Rest} annotation used to annotate the method. Any exceptions
     * that occur while handling the request will result in an 
     * {@link InternalServerException}.
     */
    private static class MappedService implements Function<RestRequest, URLResponse> {
        
        private Function<RestRequest, URLResponse> methodCallback;
        private Rest config;
        
        public MappedService(Function<RestRequest, URLResponse> methodCallback, Rest config) {
            this.methodCallback = methodCallback;
            this.config = config;
        }

        @Override
        public URLResponse apply(RestRequest request) {
            try {
                return methodCallback.apply(request);
            } catch (Exception e) {
                throw new InternalServerException("Exception while handling request", e);
            }
        }
    }
}
