//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.Method;
import nl.colorize.util.http.URLResponse;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static nl.colorize.util.rest.AuthorizationCheck.PUBLIC;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@code RestRequestDispatcher} class. Note that this only
 * tests the base mechanisms. The full flow is tested for implementations, such
 * as {@code RestServlet}.
 */
public class RestRequestDispatcherTest {

    private static final ResponseSerializer NO_SERIALIZER = response -> null;
    private static final Map<String, String> NO_HEADERS = Collections.emptyMap();

    @Test
    public void testExtractPathComponents() {
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(NO_SERIALIZER, PUBLIC);
        
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
        ResponseSerializer serializer = new ResponseSerializer() {
            @Override
            public URLResponse process(RestResponse response) {
                return null;
            }

            @Override
            public URLResponse process(URLResponse response) {
                response.addHeader("Test-1", "test1");
                response.addHeader("Test-2", "test2");
                return response;
            }
        };

        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(serializer, PUBLIC);
        registerService(requestDispatcher, Method.POST, "/", HttpStatus.OK, 
            ImmutableMap.of("Test-2", "something else", "Test-3", "test3"), "");
        URLResponse response = requestDispatcher.dispatch(createPostRequest("/", ""));
        Set<String> headers = response.getHeaderNames();

        assertEquals(8, headers.size());
        assertEquals("test1", response.getHeader("Test-1"));
        assertEquals("test2", response.getHeader("Test-2"));
        assertEquals("test3", response.getHeader("Test-3"));
    }
    
    @Test
    public void testHandlePrelightedRequest() {
        MockRestRequest optionsRequest = new MockRestRequest(Method.OPTIONS, "/", "");
        MockRestRequest getRequest = new MockRestRequest(Method.GET, "/", "");
        
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(NO_SERIALIZER, PUBLIC);
        registerService(requestDispatcher, Method.POST, "/", HttpStatus.OK, NO_HEADERS, "");
        URLResponse optionsResponse = requestDispatcher.dispatch(optionsRequest);
        URLResponse getResponse = requestDispatcher.dispatch(getRequest);
        
        assertEquals(HttpStatus.OK, optionsResponse.getStatus());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, getResponse.getStatus());
    }
    
    @Test
    public void testRequireCorrectRequestMethod() {
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(NO_SERIALIZER, PUBLIC);
        registerService(requestDispatcher, Method.GET, "/", HttpStatus.OK, NO_HEADERS, "");
        URLResponse getResponse = requestDispatcher.dispatch(createRequest(Method.GET, "/", ""));
        URLResponse postResponse = requestDispatcher.dispatch(createRequest(Method.POST, "/", ""));
        
        assertEquals(HttpStatus.OK, getResponse.getStatus());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, postResponse.getStatus());
    }
    
    @Test
    public void testWildcardRequestMethod() {
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(NO_SERIALIZER, PUBLIC);
        registerService(requestDispatcher, null, "/", HttpStatus.OK, NO_HEADERS, "");
        URLResponse response = requestDispatcher.dispatch(createRequest(Method.POST, "/", ""));
        
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    public void testSendPostData() {
        MockRestRequest request = createPostRequest("/test", "a=2&b=3");
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(NO_SERIALIZER, PUBLIC);
        requestDispatcher.bindRequest(request, createEmptyConfig());

        assertEquals("2", request.getPostData().getRequiredParameter("a"));
        assertEquals("3", request.getPostData().getRequiredParameter("b"));
    }

    @Test
    public void testConsiderJsonObjectAsPostData() {
        MockRestRequest request = createPostRequest("/test", "{a:2,b:\"3\", c=[3,4]}");
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(NO_SERIALIZER, PUBLIC);
        requestDispatcher.bindRequest(request, createEmptyConfig());

        assertEquals("2", request.getPostData().getRequiredParameter("a"));
        assertEquals("3", request.getPostData().getRequiredParameter("b"));
        assertEquals("[3,4]", request.getPostData().getRequiredParameter("c"));
    }

    @Test
    public void testCustomResponseSerializer() {
        ResponseSerializer serializer = response -> {
            URLResponse httpResponse = new URLResponse(response.getStatus());
            httpResponse.setBody("123-" + response.getBodyObject().toString());
            return httpResponse;
        };

        RestRequestDispatcher requestDispatcher = new RestRequestDispatcher(serializer, PUBLIC);
        requestDispatcher.registerServices(new FakeService(request -> new RestResponse(2)));
        URLResponse response = requestDispatcher.dispatch(new MockRestRequest(Method.GET, "/test", ""));

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("123-2", response.getBody());
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
        Function<RestRequest, URLResponse> service = request -> {
            URLResponse response = new URLResponse(status, body, Charsets.UTF_8);
            response.addHeaders(headers);
            return response;
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
        
        requestDispatcher.registerServices(new FakeService(service), config);
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

    /**
     * This class redirects to the provided callback. It is needed because the
     * request dispatched obtains the services using reflection.
     */
    private static class FakeService {

        private Function<RestRequest, ?> action;

        public FakeService(Function<RestRequest, ?> action) {
            this.action = action;
        }

        @Rest(method = Method.GET, path = "/test")
        public Object call(RestRequest request) {
            return action.apply(request);
        }
    }
}
