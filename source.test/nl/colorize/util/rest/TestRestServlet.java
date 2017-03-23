//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import static org.junit.Assert.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;

import org.junit.Before;
import org.junit.Test;

import nl.colorize.util.http.HttpResponse;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.Method;
import nl.colorize.util.testutil.MockHttpServletRequest;
import nl.colorize.util.testutil.MockHttpServletResponse;

/**
 * Unit tests for all classes in the REST API framework.
 */
@SuppressWarnings("deprecation")
public class TestRestServlet {
	
	private RestServlet servlet;
	
	@Before
	public void before() throws Exception {
		final List<Object> servicesForTesting = new ArrayList<Object>();
		servicesForTesting.add(this);
		
		servlet = new RestServlet() {
			@Override
			protected List<?> getServiceObjects() {
				return servicesForTesting;
			}
			
			@Override
			protected boolean isRequestAuthorized(RestRequest request, String authorizedRoles) {
				return authorizedRoles.isEmpty() || 
						request.getOptionalParameter("authenticated", "").equals("true");
			}
		};
		servlet.init();
	}
	
	@Test
	public void testSimpleRequest() throws Exception {
		RestRequest request = createMockRequest("GET", "/first");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.OK, response.getStatus());
		assertEquals("text/plain", response.getContentType(null).withoutParameters().toString());
		assertEquals("OK", response.getBody());
	}
	
	@Test
	public void testRequestWithTrailingSlash() throws Exception {
		RestRequest request = createMockRequest("GET", "/first/");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.OK, response.getStatus());
	}
	
	@Test
	public void testUnmatchedRequest() throws Exception {
		RestRequest request = createMockRequest("GET", "/nonexisting");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
		assertEquals("", response.getBody());
	}
	
	@Test
	public void testRequestWithParameters() throws Exception {
		RestRequest request = createMockRequest("GET", "/second", "test", "testvalue");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals("OK: testvalue", response.getBody());
	}
	
	@Test
	public void testRequiredParameterNotSet() throws Exception {
		RestRequest request = createMockRequest("GET", "/second");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
		assertEquals("", response.getBody());
	}
	
	@Test
	public void testPathParameter() throws Exception {
		RestRequest request = createMockRequest("POST", "/third/123");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.OK, response.getStatus());
		assertEquals("OK: 123", response.getBody());
	}
	
	@Test
	public void testPathParameterIsUrlEncoded() throws Exception {
		RestRequest request = createMockRequest("POST", "/third/456%2F7");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.OK, response.getStatus());
		assertEquals("OK: 456/7", response.getBody());
	}
	
	@Test
	public void testNamedPathParameter() throws Exception {
		RestRequest request = createMockRequest("POST", "/third/123");
		servlet.dispatchRequest(request);
		assertEquals("123", request.getPathParameter("id"));
	}
	
	@Test(expected=BadRequestException.class)
	public void testExceptionForNonExistingPathParameter() throws Exception {
		RestRequest request = createMockRequest("POST", "/third/123");
		servlet.dispatchRequest(request);
		request.getPathParameter("nonexisting");
	}
	
	@Test
	public void testMethodNotAllowed() throws Exception {
		RestRequest request = createMockRequest("DELETE", "/first");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatus());
		assertEquals("", response.getBody());
	}
	
	@Test
	public void testServiceWithAuthentication() throws Exception {
		RestRequest request = createMockRequest("POST", "/fourth", "authenticated", "false");
		HttpResponse response = servlet.dispatchRequest(request);
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
		assertEquals("", response.getBody());
		
		request = createMockRequest("POST", "/fourth", "authenticated", "true");
		response = servlet.dispatchRequest(request);
		assertEquals(HttpStatus.CREATED, response.getStatus());
		assertEquals("OK", response.getBody());
	}
	
	@Test
	public void testDecodePathParameter() throws Exception {
		RestRequest request = createMockRequest("POST", "/third/a1%242");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals("OK: a1$2", response.getBody());
	}
	
	@Test
	public void testOverlappingPaths() throws Exception {
		java.lang.reflect.Method noOp = getClass().getMethod("mockService", RestRequest.class);
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/overlapping", "GET"));
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/overlapping/2", "GET"));
		
		RestRequest request = createMockRequest("GET", "/overlapping");
		assertEquals("GET /overlapping", servlet.dispatchRequest(request).getBody());
		
		request = createMockRequest("GET", "/overlapping/2");
		assertEquals("GET /overlapping/2", servlet.dispatchRequest(request).getBody());
	}
	
	@Test
	public void testSamePathDifferentMethod() throws Exception {
		java.lang.reflect.Method noOp = getClass().getMethod("mockService", RestRequest.class);
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/same", "GET"));
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/same", "POST"));
		
		RestRequest request = createMockRequest("GET", "/same");
		assertEquals("GET /same", servlet.dispatchRequest(request).getBody());
		
		request = createMockRequest("POST", "/same");
		assertEquals("POST /same", servlet.dispatchRequest(request).getBody());
	}
	
	@Test(expected=IllegalStateException.class)
	public void testSamePathSameMethodNotAllowed() throws Exception {
		java.lang.reflect.Method noOp = getClass().getMethod("mockService", RestRequest.class);
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/samesame", "GET"));
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/samesame", "GET"));
	}
	
	@Test
	public void testErrorIfParameterIsInvalid() throws Exception {
		RestRequest request = createMockRequest("POST", "/third/1invalid");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
	}
	
	@Test
	public void testBadRequestForMissingParameter() throws Exception {
		RestRequest request = createMockRequest("GET", "/second");
		HttpResponse response = servlet.dispatchRequest(request);
		
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
	}
	
	@Test
	public void testNamedPathParameterWithCurlies() throws Exception {
		java.lang.reflect.Method noOp = getClass().getMethod("mockService", RestRequest.class);
		Rest mockService = createMockAnnotation("/person/{id}", "GET");
		servlet.registerServiceMethod(this, noOp, mockService);
		
		RestRequest request = createMockRequest("GET", "/person/123");
		request.bindPathParameters(new RestRequestDispatcher(null, ImmutableMap.<String, String>of())
				.parsePathParameters(request, mockService));
		
		assertEquals("123", request.getPathParameter(1));
		assertEquals("123", request.getPathParameter("id"));
	}
	
	@Test
	public void testNamedPathParameterWithColon() throws Exception {
		java.lang.reflect.Method noOp = getClass().getMethod("mockService", RestRequest.class);
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/person/:id", "GET"));
		
		RestRequest request = createMockRequest("GET", "/person/123");
		servlet.dispatchRequest(request);
		assertEquals("123", request.getPathParameter(1));
		assertEquals("123", request.getPathParameter("id"));
	}
	
	@Test
	public void testNamedPathParameterWithAtSign() throws Exception {
		java.lang.reflect.Method noOp = getClass().getMethod("mockService", RestRequest.class);
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/person/@id", "GET"));
		
		RestRequest request = createMockRequest("GET", "/person/123");
		servlet.dispatchRequest(request);
		assertEquals("123", request.getPathParameter(1));
		assertEquals("123", request.getPathParameter("id"));
	}
	
	@Test(expected=NullPointerException.class)
	public void testParameterWithNullName() throws Exception {
		createMockRequest("GET", "/test", null, "value");
	}
	
	@Test
	public void testFilterParameterWithNullValue() throws Exception {
		RestRequest request = createMockRequest("GET", "/test", "key", null);
		assertEquals("default", request.getOptionalParameter("key", "default"));
		assertEquals(0, request.getParameters().size());
	}
	
	@Test
	public void testPostRequestWithoutParameters() throws Exception {
		RestRequest request = createMockRequest("GET", "/test");
		assertEquals(0, request.getParameters().size());
	}
	
	@Test
	public void testConversionToServletResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/first");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doGet(request, response);
		
		assertEquals(HttpStatus.OK, response.getStatus());
		assertEquals("GET, POST, PUT, DELETE, OPTIONS", 
				response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("OK", response.getBuffer());
	}
	
	@Test
	public void testPathContainsEscapedSlash() throws Exception {
		RestRequest request = servlet.parseRequest(new MockHttpServletRequest("GET", "a/b%2Fc/d"));
		
		assertEquals(3, request.getPathComponents().size());
		assertEquals("a", request.getPathParameter(0));
		assertEquals("b/c", request.getPathParameter(1));
		assertEquals("d", request.getPathParameter(2));
	}
	
	@Test
	public void testQueryStringIsNotPathParameter() throws Exception {
		RestRequest request = servlet.parseRequest(new MockHttpServletRequest("GET", "a/b?c=d"));
		
		assertEquals(2, request.getPathComponents().size());
		assertEquals("a", request.getPathParameter(0));
		assertEquals("b", request.getPathParameter(1));
	}
	
	@Test(expected=IllegalStateException.class)
	public void testPathParameterCollidesWithOtherPath() throws Exception {
		java.lang.reflect.Method noOp = getClass().getMethod("mockService", RestRequest.class);
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/test/test2", "GET"));
		servlet.registerServiceMethod(this, noOp, createMockAnnotation("/test/:id", "GET"));
	}
	
	@Test
	public void testParsePostData() {
		MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", "/test");
		mockRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
		mockRequest.setParameter("a", "2");
		mockRequest.setParameter("b", "3");
		Map<String, String> params = servlet.parseRequestBody(mockRequest, "a=2&b=3");
		
		assertEquals(2, params.size());
		assertEquals("2", params.get("a"));
		assertEquals("3", params.get("b"));
	}
	
	@Test
	public void testParseJsonRequestBody() {
		MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", "/test");
		mockRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		mockRequest.setBody("{\"a\":2,\"b\":3}");
		Map<String, String> params = servlet.parseRequestBody(mockRequest, "{\"a\":2,\"b\":3}");
		
		assertEquals(2, params.size());
		assertEquals("2", params.get("a"));
		assertEquals("3", params.get("b"));
	}
	
	@Test
	public void testParseUrlParameters() {
		MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", "/test?a=2&b=");
		mockRequest.setQueryString("?a=2&b=");
		RestRequest restRequest = servlet.parseRequest(mockRequest);
		
		assertEquals("2", restRequest.getOptionalUrlParameter("a", ""));
		assertEquals("", restRequest.getOptionalUrlParameter("b", ""));
		assertEquals("", restRequest.getOptionalUrlParameter("c", ""));
	}
	
	private RestRequest createMockRequest(final String method, String path, String... params) {
		MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest(method, path);
		
		final Map<String, String> parameterMap = new HashMap<String, String>();
		for (int i = 0; i < params.length; i += 2) {
			parameterMap.put(params[i], params[i + 1]);
		}
		
		RestRequest request = servlet.parseRequest(mockHttpRequest);
		request.bindParameters(parameterMap);
		return request;
	}
	
	private Rest createMockAnnotation(final String path, final String method) {
		Rest annotation = new Rest() {
			public String path() {
				return path;
			}
			
			public Method method() {
				return Method.parse(method);
			}
			
			public String authorized() {
				return "";
			}
			
			public Class<? extends Annotation> annotationType() {
				return Rest.class;
			}
		};
		return annotation;
	}
	
	// The test implements @Rest on its own methods. The reason
	// is that those annotations are not allowed on inner classes.

	@Rest(method=Method.GET, path="/first")
	public HttpResponse firstService(RestRequest request) {
		return toResponse(HttpStatus.OK, "text/plain", "OK");
	}
	
	@Rest(method=Method.GET, path="/second")
	public HttpResponse secondService(RestRequest request) {
		return toResponse(HttpStatus.OK, "text/plain", 
				"OK: " + request.getRequiredParameter("test"));
	}
	
	@Rest(method=Method.POST, path="/third/{id}")
	public HttpResponse thirdService(RestRequest request) {
		String id = request.getPathParameter(1);
		if (id.startsWith("1")) {
			Integer.parseInt(id);
		}
		return toResponse(HttpStatus.OK, "text/plain", "OK: " + id);
	}
	
	@Rest(method=Method.POST, path="/fourth", authorized="admin")
	public HttpResponse fourthService(RestRequest request) {
		return toResponse(HttpStatus.CREATED, "text/plain", "OK");
	}
	
	public HttpResponse mockService(RestRequest request) {
		String response = request.getMethod() + " " + request.getPath();
		return toResponse(HttpStatus.OK, "text/plain", response);
	}
	
	private HttpResponse toResponse(HttpStatus status, String contentType, String body) {
		Map<String, String> headers = ImmutableMap.of(HttpHeaders.CONTENT_TYPE, contentType);
		return new HttpResponse(status, headers, body);
	}
}
