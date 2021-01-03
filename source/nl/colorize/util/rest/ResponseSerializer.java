//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import nl.colorize.util.http.URLResponse;

/**
 * Serializes a response returned by a REST service to a HTTP response. This
 * mechanism is commonly used to implement serialization of the response body,
 * for example to JSON or XML, in a central location rather than letting every
 * service manage this separately.
 */
public interface ResponseSerializer {

    public URLResponse process(RestRequest request, RestResponse response);

    /**
     * By default, REST services that already return a HTTP response will be
     * returned directly without further processing. However, it is possible
     * to override this method and add additional behavior for such services.
     */
    default URLResponse process(RestRequest request, URLResponse response) {
        return response;
    }
}
