//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.swing.Utils2D;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class URLLoaderTest {
    
    private static SimpleHttpServer server;
    private static String testURL;
    
    @BeforeClass
    public static void before() {
        server = new SimpleHttpServer();
        server.start(9090);
        testURL = "http://localhost:9090";

        System.setProperty(URLLoader.CLASSIC_LOADER_PROPERTY, "true");
    }
    
    @AfterClass
    public static void after() {
        server.stop(true);
    }
    
    // Implementation note:
    // To make sure reading URLs and sending parameters actually works, some of
    // these tests send read requests to real websites. This is obviously not 
    // very robust, since the tests will break if the site changes.
    
    @Test
    public void testLoadTextResponse() throws IOException {
        String response = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8)
                .sendRequest().getBody();
        assertTrue(response.contains("<base href=\"http://www.colorize.nl/\" />"));
        assertTrue(response.contains("</html>"));
    }
    
    @Test
    public void testLoadBinaryResponse() throws IOException {
        URLResponse response = URLLoader.get("http://www.colorize.nl/images/logo.png", 
                Charsets.UTF_8).sendRequest();
        assertNotNull(Utils2D.loadImage(response.openBodyStream()));
    }
    
    @Test
    public void testPostRequest() throws IOException {
        URLLoader urlLoader = URLLoader.post(testURL, Charsets.UTF_8);
        urlLoader.setBody(PostData.create("a", "2"));
        URLResponse response = urlLoader.sendRequest();
        
        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getBody().contains("POST /"));
        assertTrue(response.getBody().contains("Content-Type: application/x-www-form-urlencoded"));
        assertTrue(response.getBody().contains("a=2"));
    }
    
    @Test(expected=IOException.class)
    public void test404() throws IOException {
        URLLoader loader = URLLoader.get("http://www.colorize.nl/nothing.jpg", Charsets.UTF_8);
        loader.sendRequest();
    }

    @Test
    public void testGetVariables() throws IOException {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        loader.addQueryParam("page", "contact");
        String response = loader.sendRequest().getBody();

        assertEquals("http://www.colorize.nl?page=contact", loader.getFullURL().toString());
        assertTrue(response.contains("<h1>Contact"));
    }

    @Test
    public void testUrlEncodeParameters() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        loader.addQueryParam("a", "b c");

        assertEquals("http://www.colorize.nl?a=b+c", loader.getFullURL().toString());
    }

    @Test
    public void testResponseHttpStatus() throws Exception {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        assertEquals(HttpStatus.OK, loader.sendRequest().getStatus());
    }
    
    @Test
    public void testHttps() throws Exception {
        String response = URLLoader.get("https://twitter.com", Charsets.UTF_8)
            .sendRequest()
            .getBody();

        assertTrue(response.contains("Twitter"));
    }
    
    @Test
    public void testContentType() {
        assertEquals("text/html", toResponse(HttpStatus.OK, 
                "text/html", "").getContentType(null).toString());
        assertEquals("text/html; charset=utf-8", toResponse(HttpStatus.OK, 
                "text/html; charset=UTF-8", "").getContentType(null).toString());
        assertEquals("text/html", toResponse(HttpStatus.OK, 
                "text/html; charset=UTF-8", "").getContentType(null).withoutParameters().toString());
        assertNull(toResponse(HttpStatus.OK, "", "").getContentType(null));
    }

    @Test
    public void testHeaderNamesAreCaseInsensitive() {
        URLResponse response = new URLResponse(HttpStatus.OK);
        response.addHeader("header-name", "test");

        assertEquals("test", response.getHeader("header-name"));
        assertEquals("test", response.getHeader("Header-Name"));
        assertEquals(ImmutableSet.of("header-name"), response.getHeaderNames());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDisallowNullHeaders() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        loader.addHeader(null, null);
    }
    
    @Test
    public void testDownloadResponseHeaders() throws Exception {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        URLResponse response = loader.sendRequest();

        List<String> headers = response.getHeaderNames().stream()
            .map(header -> header.toLowerCase())
            .sorted()
            .collect(Collectors.toList());
            
        // Ignore headers that depend on the HTTP version.
        headers.remove("connection");
        headers.remove("transfer-encoding");

        assertEquals("text/html; charset=UTF-8", response.getHeader("Content-Type"));
        assertEquals(ImmutableList.of("accept-ranges", "age", "content-type", "date",
            "server", "vary", "via", "x-powered-by", "x-varnish"), headers);
    }
    
    @Test
    public void testHttpToHttpsRedirect() throws Exception {
        URLResponse response = URLLoader.get("http://www.twitter.com", Charsets.UTF_8).sendRequest();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getHeaderNames().contains("strict-transport-security"));
    }
    
    @Test
    public void testMalformedContentType() {
        URLResponse response = new URLResponse(HttpStatus.OK);
        response.addHeader(HttpHeaders.CONTENT_TYPE, "text/html;; charset=US-ASCII");

        assertNull(response.getContentType(null));
    }

    public void testAddQueryParams() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        loader.addQueryParam("a", "2");
        loader.addQueryParam("b", "3");
        loader.addQueryParam("c", "4");

        assertEquals("http://www.colorize.nl?a=2&b=3&c=4", loader.getFullURL().toString());
    }

    @Test
    public void testDisableCertificateVerification() throws Exception {
        URLResponse response = URLLoader.get("https://html5.validator.nu", Charsets.UTF_8)
                .disableCertificateVerification()
                .sendRequest();
        assertEquals(HttpStatus.OK, response.getStatus());
    }
    
    @Test
    public void testSetBasicAuthentication() throws Exception {
        URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        request.setBasicAuthentication("Aladdin", "open sesame");

        assertEquals("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==", request.getHeader(HttpHeaders.AUTHORIZATION));
    }
    
    @Test
    public void testHttpStatus() throws Exception {
        HttpStatus status = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8)
                .sendRequest().getStatus();
        assertEquals(HttpStatus.OK, status);
        assertEquals(200, status.getCode());
        assertEquals("200 OK", status.toString());
        assertFalse(status.isClientError());
        assertFalse(status.isServerError());
    }
    
    @Test
    public void testMultipleHeadersWithTheSameName() {
        URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        request.addHeader(HttpHeaders.COOKIE, "name=value");
        request.addHeader(HttpHeaders.COOKIE, "name2=value2");

        assertEquals(ImmutableList.of("name=value", "name2=value2"),
            request.getHeaderValues(HttpHeaders.COOKIE));
    }
    
    @Test
    public void testHeadRequestsOnlyDownloadHeaders() throws IOException {
        URLResponse getResponse = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8)
            .sendRequest();
        assertEquals(HttpStatus.OK, getResponse.getStatus());
        assertFalse(getResponse.getBody().isEmpty());
        
        URLResponse headResponse = URLLoader.create(Method.HEAD, "http://www.colorize.nl", Charsets.UTF_8)
            .sendRequest();
        assertEquals(HttpStatus.OK, headResponse.getStatus());
        assertTrue(headResponse.getBody().isEmpty());
    }

    @Test
    public void testRequestMethodsWithRequestBody() {
        List<Method> supported = new ArrayList<Method>();
        for (Method method : Method.values()) {
            if (method.hasRequestBody()) {
                supported.add(method);
            }
        }
        
        assertEquals(ImmutableList.of(Method.POST, Method.PUT), supported);
    }
    
    @Test
    public void testRequestMethodsWithHeadersOnly() {
        List<Method> supported = new ArrayList<Method>();
        for (Method method : Method.values()) {
            if (method.isResponseHeadersOnly()) {
                supported.add(method);
            }
        }
        
        assertEquals(ImmutableList.of(Method.HEAD), supported);
    }
    
    @Test
    public void testFormEncodedPostData() {
        URLLoader loader = URLLoader.post("http://www.colorize.nl", Charsets.UTF_8);
        loader.setBody(PostData.create(ImmutableMap.of("a", "2", "b", "3<4")));
        
        assertEquals("application/x-www-form-urlencoded;charset=UTF-8", 
                loader.getHeader(HttpHeaders.CONTENT_TYPE));
        assertEquals("a=2&b=3%3C4", loader.getBody());
    }
    
    @Test
    public void testJsonPostData() {
        URLLoader loader = URLLoader.post("http://www.colorize.nl", Charsets.UTF_8);
        loader.setBody("application/json", "{\"a\":2,\"b\":3}");
        
        assertEquals("[application/json]", loader.getHeaderValues(HttpHeaders.CONTENT_TYPE).toString());
        assertEquals("{\"a\":2,\"b\":3}", loader.getBody());
    }
    
    @Test
    public void testSingleParameterWithoutName() {
        URLLoader urlLoader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        urlLoader.addQueryParam("", "123");

        assertEquals("http://www.colorize.nl?123", urlLoader.getFullURL().toString());
    }

    @Test
    public void testCertificatesForHTTPS() throws IOException {
        URLLoader request = URLLoader.get("https://twitter.com", Charsets.UTF_8);
        URLResponse response = request.sendRequest();

        assertEquals(2, response.getCertificates().size());
    }

    @Test
    public void testHttpResponseWithContentLengthHeaderZero() throws IOException {
        URLLoader request = URLLoader.get("https://www.colorize-dashboard.nl/rest/website/colorize.nl/check",
            Charsets.UTF_8);
        URLResponse response = request.sendRequest();

        assertEquals(HttpStatus.ACCEPTED, response.getStatus());
        assertEquals("", response.getBody());
    }

    @Test
    public void testSendRequestAsync() throws ExecutionException, InterruptedException {
        URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        Future<URLResponse> future = request.sendRequestAsync();

        assertEquals(HttpStatus.OK, future.get().getStatus());
    }

    private URLResponse toResponse(HttpStatus status, String contentType, String body) {
        URLResponse response = new URLResponse(status, body, Charsets.UTF_8);
        response.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
        return response;
    }
}
