//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.URLResponse;

/**
 * Represents a response that is returned by a REST service. Unlike a "raw"
 * {@link URLResponse}, the response body consists of an object which is later
 * serialized to a text format such as JSON or XML.
 */
public class RestResponse {

    private HttpStatus status;
    private Object bodyObject;

    public RestResponse(HttpStatus status, Object bodyObject) {
        this.status = status;
        this.bodyObject = bodyObject;
    }

    public RestResponse(HttpStatus status) {
        this(status, null);
    }

    public RestResponse(Object bodyObject) {
        this(HttpStatus.OK, bodyObject);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getBodyObject() {
        return bodyObject;
    }
}
