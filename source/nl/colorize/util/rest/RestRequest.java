//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nl.colorize.util.Escape;
import nl.colorize.util.http.HttpMessageFragment;
import nl.colorize.util.http.Method;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Describes a request to a REST API. This class is different from "regular" HTTP
 * requests (represented by {@link javax.servlet.http.HttpServletRequest}) in
 * that it is "bound" to a specific REST service. Binding requests to services
 * means that the request is interpreted based on the service's configuration.
 * <p>
 * Path parameters are obtained from the service configuration. For example, if
 * the configuration defines a path with a parameter as "/person/{name}", and a
 * request is made with the path "/person/jim", the bound request will contain a
 * path parameter "name" with the value "jim".
 * <p>
 * The request body can be accessed using {@link #getBody()}. A convenience method
 * ((@link {@link #getPostData()}}) is provided to parse the request body as POST
 * data. Parsing other types of request body (e.g. XML, JSON) is not supported
 * directly by this class, due to the large variation in standardization for such
 * content types, and must be done manually.
 */
public class RestRequest implements HttpMessageFragment {
    
    private HttpServletRequest request;
    private String path;
    private String body;

    private List<String> pathComponents;
    private Map<String, String> pathParameters;

    protected RestRequest(HttpServletRequest request) {
        this(request, ServletUtils.getRequestPath(request), ServletUtils.getRequestBody(request));
    }

    protected RestRequest(HttpServletRequest request, String path, String body) {
        this.request = request;
        this.path = path;
        this.body = body;

        pathComponents = Collections.emptyList();
        pathParameters = Collections.emptyMap();
    }
    
    protected void bindPath(List<String> pathComponents, Map<String, String> pathParameters) {
        this.pathComponents = ImmutableList.copyOf(pathComponents);
        this.pathParameters = ImmutableMap.copyOf(pathParameters);
    }

    public Method getMethod() {
        return Method.parse(request.getMethod());
    }

    public String getPath() {
        return path;
    }

    /**
     * Returns the path components for the path to which this request was made, 
     * relative to the REST API. Leading slashes are ignored, and will not lead
     * to an empty path component. The path does not include the protocol, host 
     * name, or query string.
     */
    protected List<String> getPathComponents() {
        return pathComponents;
    }
    
    /**
     * Returns the (URL-decoded) path component at the specified index. For
     * example, a request to "/a/b/c" returns "b" for the path component at 
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

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    public Map<String, String> getHeaders() {
        return ServletUtils.getRequestHeaders(request);
    }
    
    public String getBody() {
        return body;
    }

    /**
     * Parses the request body as POST data, encoded using the
     * {@code application/x-www-form-urlencoded} content type. This will return
     * an empty {@link PostData} object if the request does not contain a body,
     * or if it cannot be parsed as POST data. Note that this method does not
     * require the correct Content-Type header. This behavior is intentionally
     * lenient to support the widespread practice of incomplete HTTP requests.
     */
    public PostData getPostData() {
        return PostData.parse(getBody(), getCharset());
    }

    public HttpServletRequest getHttpRequest() {
        return request;
    }
}
