//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import nl.colorize.util.Escape;
import nl.colorize.util.http.HttpResponse;
import nl.colorize.util.http.Method;
import nl.colorize.util.http.URLLoader;

/**
 * Miscellaneous utility and convenience methods for working with servlets in
 * web applications.
 */
public final class ServletUtils {

    private static final MediaType DEFAULT_CONTENT_TYPE = MediaType.PLAIN_TEXT_UTF_8;
    private static final Charset COOKIE_BASE64_CHARSET = Charsets.UTF_8;

    private ServletUtils() {
    }
    
    /**
     * Adds a cookie with the specified name and value to the response.
     * @param expiresAfterSeconds Sets the cookie expire time to the specified
     *        number of seconds in the future.
     * @param encodeBase64 If true, stored the cookie's value BASE64-encoded.
     */
    public static void setCookie(HttpServletResponse response, String name, String value, 
            int expiresAfterSeconds, boolean encodeBase64) {
        if (encodeBase64 && value != null && !value.isEmpty()) {
            value = Escape.base64Encode(value, COOKIE_BASE64_CHARSET);
        }
        
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(expiresAfterSeconds);
        response.addCookie(cookie);
    }
    
    /**
     * Adds a cookie with the specified name and value to the response. The cookie
     * will be set to after 30 days.
     * @param encodeBase64 If true, stored the cookie's value BASE64-encoded.
     */
    public static void setCookie(HttpServletResponse response, String name, String value, 
            boolean encodeBase64) {
        int thirtyDaysInSeconds = 30 * 24 * 3600;
        setCookie(response, name, value, thirtyDaysInSeconds, encodeBase64);
    }
    
    /**
     * Removes the value of the cookie with the specified name. If there was no
     * such cookie this method does nothing.
     */
    public static void removeCookie(HttpServletResponse response, String name) {
        setCookie(response, name, "", 0, false);
    }
    
    /**
     * Returns the cookie with the specified name. Returns {@code null} if there
     * is no such cookie.
     */
    private static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the value of the cookie with the specified name. Returns {@code null}
     * if there is no such cookie. 
     * @param decodeBase64 If true, BASE64-decodes the cookie's value.
     */
    public static String getCookie(HttpServletRequest request, String name, boolean decodeBase64) {
        Cookie cookie = getCookie(request, name);
        if (cookie != null) {
            String value = cookie.getValue();
            if (value != null && !value.isEmpty()) {
                // Some JavaScript frameworks URL-encode cookie values. 
                value = Escape.urlDecode(value, COOKIE_BASE64_CHARSET);
                if (decodeBase64) {
                    value = Escape.base64Decode(value, COOKIE_BASE64_CHARSET);
                }
                return value;
            }
        }
        return null;
    }
    
    /**
     * Returns the request path relative to the servlet root, starting with a
     * leading slash. Requests to the root of the servlet will have a path of "/".
     */
    public static String getRequestPath(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String servletPath = request.getServletPath();
        if (servletPath.length() > 1 && requestPath.startsWith(servletPath)) {
            requestPath = requestPath.substring(servletPath.length());
        }
        return requestPath;
    }
    
    /**
     * Reads the request body and returns it as a string. If the request contains
     * no body, or if the request method does not support a request body to be
     * sent, this method will return an empty string.
     */
    public static String getRequestBody(HttpServletRequest request) {
        Method method = Method.parse(request.getMethod());
        if (!method.hasRequestBody()) {
            return "";
        }
        
        try {
            BufferedReader requestBody = request.getReader();
            return CharStreams.toString(requestBody);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected read error while reading request body", e);
        }
    }
    
    /**
     * Returns all headers sent with a HTTP request. If the request contains
     * multiple headers with the same name, the first occurrence will be used
     * (which is consistent with the behavor of
     * {@link HttpServletRequest#getHeader(String)}).
     */
    public static Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }
    
    /**
     * Returns all parameters sent with a HTTP request as a name/value map. The
     * map's iteration order is undefined and might not reflect the order in which
     * the parameters were sent in the request.
     * <p>
     * This method is similar to {@link HttpServletRequest#getParameterMap()}, but 
     * it returns a map with generics. Also, multi-value parameters (when the
     * request contains multiple parameters with the same name) are not supported.
     */
    public static Map<String, String> getParameterMap(HttpServletRequest request, Charset charset) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        Enumeration<?> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = request.getParameter(name);
            if (value != null && !value.isEmpty()) {
                value = Escape.urlDecode(value, charset);
            }
            parameters.put(name, value);
        }
        return parameters;
    }
    
    /**
     * Takes a {@code HttpResponse} and uses it to fill a {@code HttpServletResponse}.
     * @throws IOException if an I/O error occurs while writing the response.
     * @throws IllegalStateException if the destination response's output stream has
     *         already been opened.
     */
    public static void fillServletResponse(HttpResponse source, HttpServletResponse dest) 
            throws IOException {
        dest.setStatus(source.getStatus().getCode());
        
        MediaType contentType = source.getContentType(DEFAULT_CONTENT_TYPE);
        dest.setContentType(contentType.withoutParameters().toString());
        dest.setCharacterEncoding(source.getCharset().displayName());
        
        for (Map.Entry<String, String> entry : source.getHeaders().entrySet()) {
            // The Content-Type is a special case, because it's already set 
            // using HttpServletResponse.setContentType().
            if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(entry.getKey())) {
                dest.addHeader(entry.getKey(), entry.getValue());
            }
        }
        
        PrintWriter responseWriter = dest.getWriter();
        responseWriter.print(source.getBody());
        responseWriter.flush();
        responseWriter.close();
    }
    
    /**
     * Forwards an incoming request to an arbitrary URL (which may be external to
     * the current application).
     * @return The response returned by the URL after forwarding the request to it.
     * @throws IOException if an I/O error occurs during communication with the URL.
     */
    @SuppressWarnings("unchecked")
    public static HttpResponse forwardRequest(HttpServletRequest request, String forwardToURL) 
            throws IOException {
        Method requestMethod = Method.parse(request.getMethod());
        Charset requestCharset = request.getCharacterEncoding() == null ? Charsets.UTF_8 : 
            Charset.forName(request.getCharacterEncoding());
        URLLoader urlLoader = new URLLoader(forwardToURL, requestMethod, requestCharset);
        
        if (requestMethod.hasRequestBody()) {
            List<String> parameterNames = Collections.list(request.getParameterNames());
            for (String param : parameterNames) {
                urlLoader.addParam(param, request.getParameter(param));
            }
        }
        
        Enumeration<String> headers = request.getHeaderNames();
        if (headers != null) {
            for (String header : Collections.list(headers)) {
                if (header != null) {
                    urlLoader.setRequestHeader(header, request.getHeader(header));
                }
            }
        }
        
        return urlLoader.sendRequest();
    }
    
    /**
     * Forwards an incoming request to an arbitrary URL (which may be external to
     * the current application), and then uses the response sent by the URL to fill
     * {@code response}.
     * @throws IOException if an I/O error occurs during communication with the URL. 
     */
    public static void forwardRequest(HttpServletRequest request, String forwardToURL, 
            HttpServletResponse response) throws IOException {
        HttpResponse responseFromURL = forwardRequest(request, forwardToURL);
        fillServletResponse(responseFromURL, response);
    }
}
