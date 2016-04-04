//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import java.util.Map;

import javax.servlet.http.Cookie;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;

import nl.colorize.util.http.HttpResponse;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.testutil.MockHttpServletRequest;
import nl.colorize.util.testutil.MockHttpServletResponse;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@code ServletUtils} class.
 */
public class TestServletUtils {
	
	@Test
	public void testGetCookie() {
		String plain = "test";
		String encoded = "dGVzdA==";
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		ServletUtils.setCookie(mockResponse, "a", plain, 60, true);
		
		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/");
		for (Cookie cookie : mockResponse.getCookies()) {
			mockRequest.addCookie(cookie);
		}
		
		assertNull(ServletUtils.getCookie(mockRequest, "nonexisting", true));
		assertEquals(encoded, ServletUtils.getCookie(mockRequest, "a", false));
		assertEquals(plain, ServletUtils.getCookie(mockRequest, "a", true));
	}
	
	@Test
	public void testGetRequestBody() {
		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/");
		mockRequest.setParameter("a", "2");
		mockRequest.setParameter("b", "3");
		
		assertEquals("a=2&b=3", ServletUtils.getRequestBody(mockRequest));
	}

	@Test
	public void testParameterMap() {
		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/");
		mockRequest.setParameter("a", "2");
		mockRequest.setParameter("b", "3");
		mockRequest.setParameter("b", "4%205");
		
		assertEquals(ImmutableMap.of("a", "2", "b", "4 5"), 
				ServletUtils.getParameterMap(mockRequest, Charsets.UTF_8));
	}
	
	@Test
	public void testFillServletResponse() throws Exception {
		Map<String, String> headers = ImmutableMap.of(HttpHeaders.CONTENT_LENGTH, "1",
				HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		ServletUtils.fillServletResponse(new HttpResponse(HttpStatus.OK, headers, "Test"), mockResponse);
		
		assertEquals(200, mockResponse.getStatus().getStatusCode());
		assertEquals("Test", mockResponse.getBuffer());
		assertEquals("1", mockResponse.getHeader(HttpHeaders.CONTENT_LENGTH));
		assertEquals("application/json; charset=utf-8", mockResponse.getHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("UTF-8", mockResponse.getCharacterEncoding());
		assertEquals("application/json; charset=utf-8", mockResponse.getContentType());
	}
	
	@Test
	public void testForwardRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setParameter("a", "2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		ServletUtils.forwardRequest(request, "http://www.dennisbijlsma.com/temp/test_post.php", response);
		
		assertEquals(200, response.getStatus().getStatusCode());
		assertEquals("2", response.getBuffer());
	}
}
