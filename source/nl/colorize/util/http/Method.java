//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * HTTP request methods, as defined by the HTTP/1.1 standard
 * (http://tools.ietf.org/html/rfc7231).
 */
public enum Method {
    GET("SAFE"),
    HEAD("SAFE", "RESPONSE_HEADERS_ONLY"),
    POST("REQUEST_BODY"),
    PUT("REQUEST_BODY"),
    DELETE(),
    CONNECT(),
    OPTIONS("SAFE"),
    TRACE("SAFE");
    
    private List<String> characteristics;
    
    private Method(String... characteristics) {
        this.characteristics = ImmutableList.copyOf(characteristics);
    }
    
    /**
     * If true, HTTP requests using this method will send request parameters as
     * part of the request body. Note that the specification does not forbid a
     * request body to be present, but some web servers will ignore the request
     * body if it's not "supposed" to be there.
     */
    public boolean hasRequestBody() {
        return characteristics.contains("REQUEST_BODY");
    }
    
    /**
     * If true, HTTP requests using this method should <em>only</em> download
     * the response headers, not the response body.
     */
    public boolean isResponseHeadersOnly() {
        return characteristics.contains("RESPONSE_HEADERS_ONLY");
    }
    
    /**
     * Indicates that this request method is "safe" and intended for information
     * retrieval without changing the state of the server.
     */
    public boolean isSafe() {
        return characteristics.contains("SAFE");
    }

    /**
     * Returns the {@code Method} instance corresponding to the HTTP request
     * method with the specified name.
     * @throws IllegalArgumentException if there is no matching request method.
     * @throws NullPointerException if {@code methodName} is {@code null}.
     */
    public static Method parse(String methodName) {
        return Method.valueOf(methodName.toUpperCase());
    }
}
