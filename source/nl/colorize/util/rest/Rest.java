//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import nl.colorize.util.http.Method;
import nl.colorize.util.http.URLResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method is part of a REST API and requests can be dispatched
 * to it. Methods with this annotation must be public, and must have exactly one
 * parameter of type {@link RestRequest}. Methods can either return an instance
 * of {@link URLResponse} when the service returns a "raw" HTTP response, or a
 * {@link RestResponse} when the service returns an object that should be
 * serialized later based on the REST API configuration.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Rest {
    
    /**
     * The HTTP request method accepted by this service. Calling this service
     * using the wrong request method will result in HTTP status 405 (method 
     * not allowed). A value of {@code null} indicates that all requests to this
     * service's path should be accepted, regardless of the request method.
     */
    public Method method();

    /**
     * Path that this service is mapped to. The path should start with a leading
     * slash, and is relative to the base URL of the REST API. The path can
     * contain named parameters, indicated by one of the following notations:
     * 
     * <ul>
     *   <li>Surrounded by curly brackets: <pre>/test/person/{id}</pre></li>
     *   <li>By a colon: <pre>/test/person/:id</pre></li>
     *   <li>By an at sign: <pre>/test/person/@id</pre></li>
     * </ul>
     */
    public String path();
    
    /**
     * Describes the role(s) that are authorized for this service. This value
     * is used by the {@link AuthorizationCheck} configured for this service.
     * Calling this service without the required role will result in HTTP
     * status 401 (unauthorized).
     */
    public String authorized() default "";
}
