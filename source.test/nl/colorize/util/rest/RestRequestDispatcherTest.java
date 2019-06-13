//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.http.HttpResponse;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.Method;
import nl.colorize.util.mock.MockRestRequest;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@code RestRequestDispatcher} class. Note that this only
 * tests the base mechanisms. The full flow is tested for implementations, such
 * as {@code RestServlet}.
 */
public class RestRequestDispatcherTest {
    
    private static final Map<String, String> NO_HEADERS = Collections.emptyMap();
    
    @Test
    public void testExtractPathComponents() {
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC, NO_HEADERS);
        
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
        MockRestRequest optionsRequest = new MockRestRequest(Method.OPTIONS, "/", "");
        MockRestRequest getRequest = new MockRestRequest(Method.GET, "/", "");
        
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC, NO_HEADERS);
        registerService(requestDispatcher, Method.POST, "/", HttpStatus.OK, NO_HEADERS, "");
        HttpResponse optionsResponse = requestDispatcher.dispatch(optionsRequest);
        HttpResponse getResponse = requestDispatcher.dispatch(getRequest);
        
        assertEquals(HttpStatus.OK, optionsResponse.getStatus());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, getResponse.getStatus());
    }
    
    @Test
    public void testRequireCorrectRequestMethod() {
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC, NO_HEADERS);
        registerService(requestDispatcher, Method.GET, "/", HttpStatus.OK, NO_HEADERS, "");
        HttpResponse getResponse = requestDispatcher.dispatch(createRequest(Method.GET, "/", ""));
        HttpResponse postResponse = requestDispatcher.dispatch(createRequest(Method.POST, "/", ""));
        
        assertEquals(HttpStatus.OK, getResponse.getStatus());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, postResponse.getStatus());
    }
    
    @Test
    public void testWildcardRequestMethod() {
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC, NO_HEADERS);
        registerService(requestDispatcher, null, "/", HttpStatus.OK, NO_HEADERS, "");
        HttpResponse response = requestDispatcher.dispatch(createRequest(Method.POST, "/", ""));
        
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    public void testSendPostData() {
        MockRestRequest request = createPostRequest("/test", "a=2&b=3");
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC, NO_HEADERS);
        requestDispatcher.bindRequest(request, createEmptyConfig());

        assertEquals("2", request.getPostData().getRequiredParameter("a"));
        assertEquals("3", request.getPostData().getRequiredParameter("b"));
    }

    @Test
    public void testConsiderJsonObjectAsPostData() {
        MockRestRequest request = createPostRequest("/test", "{a:2,b:\"3\", c=[3,4]}");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(AuthorizationCheck.PUBLIC, NO_HEADERS);
        requestDispatcher.bindRequest(request, createEmptyConfig());

        assertEquals("2", request.getPostData().getRequiredParameter("a"));
        assertEquals("3", request.getPostData().getRequiredParameter("b"));
        assertEquals("[3,4]", request.getPostData().getRequiredParameter("c"));
    }

    private MockRestRequest createRequest(Method method, String path, String body) {
        return new MockRestRequest(method, path, body);
    }
    
    private MockRestRequest createPostRequest(String path, String body) {
        return createRequest(Method.POST, path, body);
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
    
    private Rest createEmptyConfig() {
        return new Rest() {
            public Class<? extends Annotation> annotationType() {
                return Rest.class;
            }

            public Method method() {
                return Method.GET;
            }

            public String path() {
                return "/";
            }

            public String authorized() {
                return "";
            }
        };
    }
}
