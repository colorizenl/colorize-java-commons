//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import nl.colorize.util.http.HttpRequest;
import nl.colorize.util.http.HttpResponse;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.Method;

/**
 * Unit tests for the {@code RestRequestDispatcher} class. Note that this only
 * tests the base mechanisms. The full flow is tested for implementations, such
 * as {@code RestServlet}.
 */
public class TestRestRequestDispatcher {
	
	private static final Map<String, String> NO_HEADERS = Collections.emptyMap();
	
	@Test
	public void testExtractPathComponents() {
		RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC,
				NO_HEADERS);
		
		assertEquals(ImmutableList.of(), requestDispatcher.extractPathComponents(""));
		assertEquals(ImmutableList.of(), requestDispatcher.extractPathComponents("/"));
		assertEquals(ImmutableList.of("a"), requestDispatcher.extractPathComponents("/a"));
		assertEquals(ImmutableList.of("a", "b"), requestDispatcher.extractPathComponents("/a/b"));
		assertEquals(ImmutableList.of("a", "b"), requestDispatcher.extractPathComponents("/a/b/"));
		assertEquals(ImmutableList.of("a", "b"), requestDispatcher.extractPathComponents("a/b/"));
		assertEquals(ImmutableList.of("a", "b"), requestDispatcher.extractPathComponents("/a/b?c=2"));
		assertEquals(ImmutableList.of("a", "b"), requestDispatcher.extractPathComponents("/a/b/?c=2"));
		assertEquals(ImmutableList.of(), requestDispatcher.extractPathComponents("/?c=2"));
		assertEquals(ImmutableList.of(), requestDispatcher.extractPathComponents("?c=2"));
	}

	@Test
	public void testParsePostData() throws IOException {
		assertEquals(ImmutableMap.of(), createPostRequest("").parsePostData());
		assertEquals(ImmutableMap.of("a", "2"), createPostRequest("a=2").parsePostData());
		assertEquals(ImmutableMap.of("a", "2", "b", "3"), createPostRequest("a=2&b=3").parsePostData());
		assertEquals(ImmutableMap.of("a", "6#7"), createPostRequest("a=6%237").parsePostData());
	}
	
	@Test
	public void testDefaultResponseHeaders() {
		RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC,
				ImmutableMap.of("Test-1", "test1", "Test-2", "test2"));
		registerService(requestDispatcher, Method.POST, "/", HttpStatus.OK, 
				ImmutableMap.of("Test-2", "something else", "Test-3", "test3"), "");
		HttpResponse response = requestDispatcher.dispatch(createPostRequest("/", ""));
		Map<String, String> responseHeaders = response.getHeaders();
		
		assertEquals(3, responseHeaders.size());
		assertEquals("test1", responseHeaders.get("Test-1"));
		assertEquals("something else", responseHeaders.get("Test-2"));
		assertEquals("test3", responseHeaders.get("Test-3"));
	}
	
	@Test
	public void testHandlePrelightedRequest() {
		RestRequest optionsRequest = new RestRequest(Method.OPTIONS, ImmutableList.of("/"), NO_HEADERS, "");
		RestRequest getRequest = new RestRequest(Method.GET, ImmutableList.of("/"), NO_HEADERS, "");
		
		RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC,
				NO_HEADERS);
		registerService(requestDispatcher, Method.POST, "/", HttpStatus.OK, NO_HEADERS, "");
		HttpResponse optionsResponse = requestDispatcher.dispatch(optionsRequest);
		HttpResponse getResponse = requestDispatcher.dispatch(getRequest);
		
		assertEquals(HttpStatus.OK, optionsResponse.getStatus());
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, getResponse.getStatus());
	}
	
	@Test
	public void testRequireCorrectRequestMethod() {
		RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC,
				NO_HEADERS);
		registerService(requestDispatcher, Method.GET, "/", HttpStatus.OK, NO_HEADERS, "");
		HttpResponse getResponse = requestDispatcher.dispatch(createRequest(Method.GET, "/", ""));
		HttpResponse postResponse = requestDispatcher.dispatch(createRequest(Method.POST, "/", ""));
		
		assertEquals(HttpStatus.OK, getResponse.getStatus());
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, postResponse.getStatus());
	}
	
	@Test
	public void testWildcardRequestMethod() {
		RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC,
				NO_HEADERS);
		registerService(requestDispatcher, null, "/", HttpStatus.OK, NO_HEADERS, "");
		HttpResponse response = requestDispatcher.dispatch(createRequest(Method.POST, "/", ""));
		
		assertEquals(HttpStatus.OK, response.getStatus());
	}
	
	private RestRequest createRequest(Method method, String path, String body) {
		return new RestRequest(method, Splitter.on("/").omitEmptyStrings().splitToList(path), 
				NO_HEADERS, body);
	}
	
	private RestRequest createPostRequest(String path, String body) {
		return createRequest(Method.POST, path, body);
	}
	
	private HttpRequest createPostRequest(String body) {
		return createPostRequest("/", body);
	}
	
	private void registerService(RestRequestDispatcher requestDispatcher, 
			final Method method, final String path, 
			final HttpStatus status, final Map<String, String> headers, final String body) {
		Function<RestRequest, HttpResponse> service = new Function<RestRequest, HttpResponse>() {
			public HttpResponse apply(RestRequest request) {
				return new HttpResponse(status, headers, body);
			}
		};
		
		Rest config = new Rest() {
			public Class<? extends Annotation> annotationType() {
				return Rest.class;
			}

			public Method method() {
				return method;
			}

			public String path() {
				return path;
			}

			public String authorized() {
				return "";
			}
		};
		
		requestDispatcher.registerService(service, config);
	}
}
