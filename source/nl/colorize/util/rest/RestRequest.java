//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import nl.colorize.util.Escape;
import nl.colorize.util.http.HttpMessage;
import nl.colorize.util.http.Method;
import nl.colorize.util.http.PostData;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a HTTP request sent to a REST API. This class wraps the underlying
 * {@link javax.servlet.http.HttpServletRequest}, and adds a number of convenience
 * methods. The request can be "bound" to a REST service, whic mean that the
 * request is interpreted based on the service's configuration.
 * <p>
 * Path parameters are obtained from the service configuration. For example, if
 * the configuration defines a path with a parameter as "/person/{name}", and a
 * request is made with the path "/person/jim", the bound request will contain a
 * path parameter "name" with the value "jim".
 * <p>
 * This class provides methods for obtaining the request body as text, as binary
 * data, or as post data (i.e. {@code application/x-www-form-urlencoded}).
 */
public class RestRequest extends HttpMessage {
    
    private HttpServletRequest httpRequest;

    private Method method;
    private String path;

    private List<String> pathComponents;
    private Map<String, String> pathParameters;

    /**
     * Creates a new {@code RestRequest} based on the underlying HTTP request.
     * The created request will not initially be bound to any REST service.
     */
    protected RestRequest(HttpServletRequest httpRequest, Method method, String path) {
        this.httpRequest = httpRequest;

        this.method = method;
        this.path = path;

        this.pathComponents = Collections.emptyList();
        this.pathParameters = Collections.emptyMap();
    }
    
    protected void bindPath(List<String> pathComponents, Map<String, String> pathParameters) {
        this.pathComponents = ImmutableList.copyOf(pathComponents);
        this.pathParameters = ImmutableMap.copyOf(pathParameters);
    }

    public Method getMethod() {
        return method;
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

    /**
     * Attempts to parse the request body as POST data encoded using the
     * {@code application/x-www-form-urlencoded} content type. This will return
     * an empty {@link PostData} object if the request does not contain a body,
     * or if the body cannot be parsed as POST data. Note that this method does
     * not require the correct Content-Type header. This behavior is intentionally
     * lenient to support the widespread practice of incomplete HTTP requests.
     * <p>
     * This method also provides limited support to treat a JSON request body as
     * POST data. If the request's content type is "application/json" and the
     * request body is a JSON object, the keys and values of the JSON object will
     * be used as POST data names and values. Other types of JSON request body
     * are not supported.
     */
    public PostData getPostData() {
        if (!hasBody()) {
            return PostData.empty();
        }

        String body = getBody();
        String contentType = getHeader(HttpHeaders.CONTENT_TYPE, null);

        if (contentType != null && contentType.startsWith("application/json")) {
            return PostData.create(parseJsonRequestBody(body));
        } else {
            return PostData.parse(body, getEncoding());
        }
    }

    /**
     * Attempts to parse the request body as a JSON object.
     * @deprecated This class should not make assumptions on the structure of
     *             the JSON, and should allow for complex JSON objects that can
     *             be serialized to Java object structures. This is already
     *             possible by accessing the request body directly.
     */
    @Deprecated
    private Map<String,String> parseJsonRequestBody(String body) {
        JsonParser jsonParser = new JsonParser();
        JsonElement json = jsonParser.parse(body);

        if (!(json instanceof JsonObject)) {
            return Collections.emptyMap();
        }

        Map<String, String> data = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : ((JsonObject) json).entrySet()) {
            if (entry.getValue() instanceof JsonPrimitive) {
                data.put(entry.getKey(), entry.getValue().getAsString());
            } else {
                data.put(entry.getKey(), entry.getValue().toString());
            }
        }

        return data;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }
}
