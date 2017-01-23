//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;

import nl.colorize.util.Escape;

/**
 * Mock implementation of {@code HttpServletRequest} for testing purposes.
 */
public class MockHttpServletRequest implements HttpServletRequest {

	private String method;
	private String path;
	private String queryString;
	private Map<String, String> headers;
	private Map<String, String> params;
	private String body;
	private List<Cookie> cookies;
	private String remoteAddress;
	private String servletPath;
	
	public MockHttpServletRequest(String method, String path) {
		this.method = method;
		this.path = path;
		headers = new LinkedHashMap<String, String>();
		params = new LinkedHashMap<String, String>();
		cookies = new ArrayList<Cookie>();
		remoteAddress = "127.0.0.1";
		servletPath = "";
	}

	public Object getAttribute(String name) {
		return params.get(name);
	}

	public Enumeration<?> getAttributeNames() {
		return Collections.enumeration(params.keySet());
	}

	public String getCharacterEncoding() {
		// No-op
		return Charsets.UTF_8.displayName();
	}

	public void setCharacterEncoding(String charset) throws UnsupportedEncodingException {
		// No-op
	}

	public int getContentLength() {
		return -1;
	}

	public String getContentType() {
		return getHeader(HttpHeaders.CONTENT_TYPE);
	}

	public ServletInputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException();
	}
	
	public void setParameter(String name, String value) {
		params.put(name, value);
	}

	public String getParameter(String name) {
		return params.get(name);
	}

	public Enumeration<?> getParameterNames() {
		return Collections.enumeration(params.keySet());
	}

	public String[] getParameterValues(String name) {
		return params.values().toArray(new String[0]);
	}

	public Map<?, ?> getParameterMap() {
		return params;
	}

	public String getProtocol() {
		throw new UnsupportedOperationException();
	}

	public String getScheme() {
		throw new UnsupportedOperationException();
	}

	public String getServerName() {
		throw new UnsupportedOperationException();
	}

	public int getServerPort() {
		throw new UnsupportedOperationException();
	}
	
	public void setBody(String body) {
		this.body = body;
	}

	public BufferedReader getReader() throws IOException {
		if (body != null) {
			return new BufferedReader(new StringReader(body));
		} else {
			StringBuilder buffer = new StringBuilder();
			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (buffer.length() > 0) {
					buffer.append("&");
				}
				buffer.append(entry.getKey());
				buffer.append("=");
				buffer.append(Escape.urlEncode(entry.getValue(), Charsets.UTF_8));
			}
			return new BufferedReader(new StringReader(buffer.toString()));
		}
	}
	
	public void setRemoteAddr(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public String getRemoteAddr() {
		return remoteAddress;
	}

	public String getRemoteHost() {
		throw new UnsupportedOperationException();
	}

	public void setAttribute(String name, Object o) {
		params.put(name, o.toString());
	}

	public void removeAttribute(String name) {
		params.remove(name);
	}

	public Locale getLocale() {
		throw new UnsupportedOperationException();
	}

	public Enumeration<?> getLocales() {
		throw new UnsupportedOperationException();
	}

	public boolean isSecure() {
		throw new UnsupportedOperationException();
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException();
	}

	public String getRealPath(String path) {
		throw new UnsupportedOperationException();
	}

	public int getRemotePort() {
		throw new UnsupportedOperationException();
	}

	public String getLocalName() {
		throw new UnsupportedOperationException();
	}

	public String getLocalAddr() {
		throw new UnsupportedOperationException();
	}

	public int getLocalPort() {
		throw new UnsupportedOperationException();
	}

	public String getAuthType() {
		throw new UnsupportedOperationException();
	}
	
	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
	}
	
	public void addCookie(String name, String value) {
		cookies.add(new Cookie(name, value));
	}

	public Cookie[] getCookies() {
		return cookies.toArray(new Cookie[0]);
	}

	public long getDateHeader(String name) {
		throw new UnsupportedOperationException();
	}
	
	public void setHeader(String name, String value) {
		headers.put(name, value);
	}

	public String getHeader(String name) {
		return headers.get(name);
	}

	public Enumeration<?> getHeaders(String name) {
		return Collections.enumeration(headers.values());
	}

	public Enumeration<?> getHeaderNames() {
		return Collections.enumeration(headers.keySet());
	}

	public int getIntHeader(String name) {
		return Integer.parseInt(headers.get(name));
	}

	public String getMethod() {
		return method;
	}

	public String getPathInfo() {
		return path;
	}

	public String getPathTranslated() {
		throw new UnsupportedOperationException();
	}

	public String getContextPath() {
		return "";
	}
	
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getRemoteUser() {
		throw new UnsupportedOperationException();
	}

	public boolean isUserInRole(String role) {
		throw new UnsupportedOperationException();
	}

	public Principal getUserPrincipal() {
		throw new UnsupportedOperationException();
	}

	public String getRequestedSessionId() {
		throw new UnsupportedOperationException();
	}

	public String getRequestURI() {
		return path;
	}

	public StringBuffer getRequestURL() {
		throw new UnsupportedOperationException();
	}
	
	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	public String getServletPath() {
		return servletPath;
	}

	public HttpSession getSession(boolean create) {
		return null;
	}

	public HttpSession getSession() {
		return null;
	}

	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException();
	}

	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException();
	}

	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException();
	}

	public boolean isRequestedSessionIdFromUrl() {
		throw new UnsupportedOperationException();
	}
}
