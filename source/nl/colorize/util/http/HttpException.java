//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import lombok.Getter;

import java.io.IOException;

/**
 * Exception for when sending an HTTP request has resulted in a response with
 * an error status. This exception is different from the "plain"
 * {@link IOException}, which indicates that <em>sending</em> the request
 * has failed.
 */
@Getter
public class HttpException extends IOException {

    private int statusCode;

    public HttpException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpException(int statusCode) {
        this("Received response with HTTP status " + statusCode, statusCode);
    }
}
