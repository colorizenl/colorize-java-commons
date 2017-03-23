//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.testutil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import nl.colorize.util.http.HttpStatus;

/**
 * Mock implementation of {@code HttpServletResponse} for testing purposes.
 */
public class MockHttpServletResponse implements HttpServletResponse {
	
	private HttpStatus status;
	private MediaType contentType;
	private Map<String, String> headers;
	private List<Cookie> cookies;
	private ByteArrayOutputStream buffer;
	
	public MockHttpServletResponse() {
		headers = new LinkedHashMap<String, String>();
		buffer = new ByteArrayOutputStream();
		cookies = new ArrayList<Cookie>();
	}

	public String getCharacterEncoding() {
		if (contentType == null || !contentType.charset().isPresent()) {
			return null;
		}
		return contentType.charset().get().displayName();
	}

	public String getContentType() {
		if (contentType == null) {
			return null;
		}
		return contentType.toString();
	}

	public ServletOutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(new OutputStreamWriter(buffer, Charsets.UTF_8));
	}
	
	public String getBuffer() {
		return new String(buffer.toByteArray(), Charsets.UTF_8);
	}

	public void setCharacterEncoding(String charset) {
		contentType = contentType.withCharset(Charset.forName(charset));
	}

	public void setContentLength(int len) {
	}

	public void setContentType(String type) {
		contentType = MediaType.parse(type);
	}

	public void setBufferSize(int size) {
		throw new UnsupportedOperationException();
	}

	public int getBufferSize() {
		throw new UnsupportedOperationException();
	}

	public void flushBuffer() throws IOException {
		throw new UnsupportedOperationException();
	}

	public void resetBuffer() {
		throw new UnsupportedOperationException();
	}

	public boolean isCommitted() {
		return false;
	}

	public void reset() {
		throw new UnsupportedOperationException();
	}

	public void setLocale(Locale loc) {
		throw new UnsupportedOperationException();
	}

	public Locale getLocale() {
		throw new UnsupportedOperationException();
	}

	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
	}
	
	public List<Cookie> getCookies() {
		return cookies;
	}

	public boolean containsHeader(String name) {
		return headers.containsKey(name);
	}

	public String encodeURL(String url) {
		throw new UnsupportedOperationException();
	}

	public String encodeRedirectURL(String url) {
		throw new UnsupportedOperationException();
	}

	public String encodeUrl(String url) {
		throw new UnsupportedOperationException();
	}

	public String encodeRedirectUrl(String url) {
		throw new UnsupportedOperationException();
	}

	public void sendError(int sc, String msg) throws IOException {
		status = HttpStatus.parse(sc);
	}

	public void sendError(int sc) throws IOException {
		status = HttpStatus.parse(sc);
	}

	public void sendRedirect(String location) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void setDateHeader(String name, long date) {
		setHeader(name, String.valueOf(date));
	}

	public void addDateHeader(String name, long date) {
		setHeader(name, String.valueOf(date));
	}

	public void setHeader(String name, String value) {
		if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
			setContentType(value);
		} else {
			headers.put(name, value);
		}
	}

	public void addHeader(String name, String value) {
		setHeader(name, value);
	}

	public void setIntHeader(String name, int value) {
		setHeader(name, String.valueOf(value));
	}

	public void addIntHeader(String name, int value) {
		setHeader(name, String.valueOf(value));
	}
	
	public String getHeader(String name) {
		if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
			return contentType.toString();
		} else {
			return headers.get(name);
		}
	}

	public void setStatus(int sc) {
		status = HttpStatus.parse(sc);
	}

	public void setStatus(int sc, String sm) {
		status = HttpStatus.parse(sc);
	}
	
	public HttpStatus getStatus() {
		return status;
	}
}
