//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.base.Charsets;
import nl.colorize.util.Escape;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;

/**
 * Miscellaneous utility and convenience methods for working with servlets in
 * web applications.
 */
public final class ServletUtils {

    private static final Charset COOKIE_BASE64_CHARSET = Charsets.UTF_8;

    private ServletUtils() {
    }

    /**
     * Returns the request path relative to the servlet root, starting with a
     * leading slash. Requests to the root of the servlet will have a path of "/".
     */
    public static String getRequestPath(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String servletPath = request.getServletPath();

        if (servletPath.length() > 1 && requestPath.startsWith(servletPath) && !requestPath.equals(servletPath)) {
            requestPath = requestPath.substring(servletPath.length());
        }

        return requestPath;
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
}
