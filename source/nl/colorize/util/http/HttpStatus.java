//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

/**
 * HTTP response status codes, as defined by the HTTP/1.1 standard, as defined
 * in <a href="https://tools.ietf.org/html/rfc2616#section-10">RFC 2616</a>.
 */
public interface HttpStatus {
    
    // Informational 1xx
    int CONTINUE = 100;
    int SWITCHING_PROTOCOLS = 101;
    
    // Successful 2xx
    int OK = 200;
    int CREATED = 201;
    int ACCEPTED = 202;
    int NON_AUTHORITATIVE_INFORMATION = 203;
    int NO_CONTENT = 204;
    int RESET_CONTENT = 205;
    int PARTIAL_CONTENT = 206;
    
    // Redirection 3xx
    int MULTIPLE_CHOICES = 300;
    int MOVED_PERMANENTLY = 301;
    int FOUND = 302;
    int SEE_OTHER = 303;
    int NOT_MODIFIED = 304;
    int USE_PROXY = 305;
    int TEMPORARY_REDIRECT = 307;
    int PERMANENT_REDIRECT = 308;
    
    // Client Error 4xx,
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int PAYMENT_REQUIRED = 402;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int METHOD_NOT_ALLOWED = 405;
    int NOT_ACCEPTABLE = 406;
    int PROXY_AUTHENTICATION_REQUIRED = 407;
    int REQUEST_TIMEOUT = 408;
    int CONFLICT = 409;
    int GONE = 410;
    int LENGTH_REQUIRED = 411;
    int PRECONDITION_FAILED = 412;
    int PAYLOAD_TOO_LARGE = 413;
    int URI_TOO_LONG = 414;
    int UNSUPPORTED_MEDIA_TYPE = 415;
    int RANGE_NOT_SATISFIABLE = 416;
    int EXPECTATION_FAILED = 417;
    int IM_A_TEAPOT = 418;
    int TOO_EARLY = 425;
    int UPGRADE_REQUIRED = 426;
    int PRECONDITION_REQUIRED = 428;
    int TOO_MANY_REQUESTS = 429;
    int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;
    int UNAVAILABLE_FOR_LEGAL_REASONS = 451;
    
    // Server Error 5xx
    int INTERNAL_SERVER_ERROR = 500;
    int NOT_IMPLEMENTED = 501;
    int BAD_GATEWAY = 502;
    int SERVICE_UNAVAILABLE = 503;
    int GATEWAY_TIMEOUT = 504;
    int HTTP_VERSION_NOT_SUPPORTED = 505;
    int VARIANT_ALSO_NEGOTATES = 506;
    int NOT_EXTENDED = 510;
    int NETWORK_AUTHENTICATION_REQUIRED = 511;

    public static boolean isRedirect(int status) {
        return status >= 300 && status <= 399;
    }

    public static boolean isClientError(int status) {
        return status >= 400 && status <= 499;
    }

    public static boolean isServerError(int status) {
        return status >= 400 && status <= 499;
    }

    public static boolean isError(int status) {
        return isClientError(status) || isServerError(status);
    }
}
