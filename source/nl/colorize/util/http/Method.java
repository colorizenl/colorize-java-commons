//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

/**
 * HTTP request methods, as defined by the HTTP/1.1 standard. Refer to
 * <a href="http://tools.ietf.org/html/rfc7231">RFC 7231</a> for more
 * information.
 */
public enum Method {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    CONNECT,
    OPTIONS,
    TRACE;

    /**
     * If true, HTTP requests using this method will send request parameters as
     * part of the request body. Note that the specification does not forbid a
     * request body to be present, but some web servers will ignore the request
     * body if it's not "supposed" to be there.
     */
    public boolean hasRequestBody() {
        return this == POST || this == PUT;
    }
    
    /**
     * If true, HTTP requests using this method should <em>only</em> download
     * the response headers, not the response body.
     */
    public boolean isResponseHeadersOnly() {
        return this == HEAD;
    }
    
    /**
     * Indicates that this request method is "safe" and intended for information
     * retrieval without changing the state of the server.
     */
    public boolean isSafe() {
        return this == GET || this == HEAD || this == OPTIONS || this == TRACE;
    }

    /**
     * Returns the {@code Method} instance corresponding to the HTTP request
     * method with the specified name.
     *
     * @throws IllegalArgumentException if there is no matching request method.
     * @throws NullPointerException if {@code methodName} is {@code null}.
     */
    public static Method parse(String methodName) {
        return Method.valueOf(methodName.toUpperCase());
    }
}
