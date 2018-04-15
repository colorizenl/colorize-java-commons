//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import nl.colorize.util.Formatting;

/**
 * HTTP response status codes, as defined by the HTTP/1.1 standard
 * (http://tools.ietf.org/html/rfc7231).
 */
public enum HttpStatus {
    
    // Informational 1xx
    CONTINUE(100),
    SWITCHING_PROTOCOLS(101),
    
    // Successful 2xx
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NON_AUTHORITATIVE_INFORMATION(203),
    NO_CONTENT(204),
    RESET_CONTENT(205),
    
    // Redirection 3xx
    MULTIPLE_CHOICES(300),
    MOVED_PERMANENTLY(301),
    FOUND(302),
    SEE_OTHER(303),
    NOT_MODIFIED(304),
    USE_PROXY(305),
    TEMPORARY_REDIRECT(307),
    
    // Client Error 4xx,
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    PAYMENT_REQUIRED(402),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    NOT_ACCEPTABLE(406),
    PROXY_AUTHENTICATION_REQUIRED(407),
    REQUEST_TIMEOUT(408),
    CONFLICT(409),
    GONE(410),
    LENGTH_REQUIRED(411),
    PRECONDITION_FAILED(412),
    PAYLOAD_TOO_LARGE(413),
    URI_TOO_LONG(414),
    UNSUPPORTED_MEDIA_TYPE(415),
    EXPECTATION_FAILED(417),
    UPGRADE_REQUIRED(426),
    
    // Server Error 5xx
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504),
    HTTP_VERSION_NOT_SUPPORTED(505);
    
    private int statusCode;
    
    private HttpStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns the numerical status code for this HTTP status (e.g. 200).
     */
    public int getCode() {
        return statusCode;
    }
    
    /**
     * Returns the textual description for this HTTP status (e.g. "OK").
     */
    public String getDescription() {
        if (name().length() <= 2) {
            return name();
        }
        return Formatting.toTitleCase(name());
    }
    
    @Override
    public String toString() {
        return statusCode + " " + getDescription();
    }
    
    public boolean isInformational() {
        return statusCode >= 100 && statusCode <= 199;
    }
    
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode <= 299;
    }
    
    public boolean isRedirection() {
        return statusCode >= 300 && statusCode <= 399;
    }
    
    public boolean isClientError() {
        return statusCode >= 400 && statusCode <= 499;
    }
    
    public boolean isServerError() {
        return statusCode >= 500 && statusCode <= 599;
    }
    
    /**
     * Returns the {@code HttpStatus} instance corresponding to the specified
     * status code.
     * @throws IllegalArgumentException if there is no matching HTTP status.
     */
    public static HttpStatus parse(int statusCode) {
        for (HttpStatus httpStatus : values()) {
            if (httpStatus.getCode() == statusCode) {
                return httpStatus;
            }
        }
        throw new IllegalArgumentException("No matching HTTP status: " + statusCode);
    }
}
