//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nl.colorize.util.http.Method;

/**
 * Indicates that a method is part of a REST API and requests can be dispatched
 * to it. Methods with this annotation must be public, must have exactly one 
 * parameter of type {@link RestRequest}, and should have a return value of type 
 * {@code HttpResponse}. The HTTP status, Content-Type, additional response 
 * headers, and response body of the response will all also  be used as response 
 * of the service.  
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
     * Describes the role(s) that are authorized for this service. The
     * authorization check is performed by the {@link RestServlet}, in
     * {@link RestServlet#isRequestAuthorized(RestRequest, String)}. Calling
     * this service without the required role will result in HTTP status 401 
     * (unauthorized).
     */
    public String authorized() default "";
}
