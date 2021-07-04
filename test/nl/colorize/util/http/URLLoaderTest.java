//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.swing.Utils2D;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URLLoaderTest {
    
    private static SimpleHttpServer server;
    private static String testURL;

    private static final String TEST_HTTPS_URL = "https://www.gazzetta.it";
    
    @BeforeAll
    public static void before() {
        server = new SimpleHttpServer();
        server.start(9090);
        testURL = "http://localhost:9090";
    }
    
    @AfterAll
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
    
    @Test
    public void test404() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl/nothing.jpg", Charsets.UTF_8);
        assertThrows(IOException.class, () -> loader.sendRequest());
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
        String response = URLLoader.get(TEST_HTTPS_URL, Charsets.UTF_8)
            .sendRequest()
            .getBody();

        assertTrue(response.toLowerCase().contains("gazzetta"));
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

        assertEquals("test", response.getHeaders().getValue("header-name"));
        assertEquals("test", response.getHeaders().getValue("Header-Name"));
        assertEquals(ImmutableSet.of("header-name"), response.getHeaders().getNames());
    }
    
    @Test
    public void testDisallowNullHeaders() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> loader.addHeader(null, null));
    }
    
    @Test
    public void testDownloadResponseHeaders() throws Exception {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        URLResponse response = loader.sendRequest();

        assertEquals("text/html; charset=UTF-8", response.getHeaders().getValue("Content-Type"));
        assertEquals("0", response.getHeaders().getValue("Age"));
        assertEquals("Apache", response.getHeaders().getValue("Server"));
        assertEquals("Accept-Encoding", response.getHeaders().getValue("Vary"));
    }
    
    @Test
    public void testHttpToHttpsRedirect() throws Exception {
        URLResponse response = URLLoader.get(TEST_HTTPS_URL, Charsets.UTF_8).sendRequest();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getHeaders().getNames().contains("strict-transport-security"));
    }
    
    @Test
    public void testMalformedContentType() {
        URLResponse response = new URLResponse(HttpStatus.OK);
        response.addHeader(HttpHeaders.CONTENT_TYPE, "text/html;; charset=US-ASCII");

        assertNull(response.getContentType(null));
    }

    @Test
    public void testAddQueryParams() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        loader.addQueryParam("a", "2");
        loader.addQueryParam("b", "3");
        loader.addQueryParam("c", "4");

        assertEquals("http://www.colorize.nl?a=2&b=3&c=4", loader.getFullURL().toString());
    }

    @Test
    public void testDisableCertificateVerification() throws IOException {
        URLLoader request = URLLoader.get("https://html5.validator.nu", Charsets.UTF_8);
        request.disableCertificateVerification();
        URLResponse response = request.sendRequest();
        assertEquals(HttpStatus.OK, response.getStatus());
    }
    
    @Test
    public void testSetBasicAuthentication() {
        URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        request.setBasicAuthentication("Aladdin", "open sesame");

        assertEquals("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==",
            request.getHeaders().getValue(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testMultipleHeadersWithTheSameName() {
        URLLoader request = URLLoader.get("http://www.colorize.nl", Charsets.UTF_8);
        request.addHeader(HttpHeaders.COOKIE, "name=value");
        request.addHeader(HttpHeaders.COOKIE, "name2=value2");

        assertEquals(ImmutableList.of("name=value", "name2=value2"),
            request.getHeaders().getValues(HttpHeaders.COOKIE));
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
            loader.getHeaders().getValue(HttpHeaders.CONTENT_TYPE));
        assertEquals("a=2&b=3%3C4", loader.getBody());
    }
    
    @Test
    public void testJsonPostData() {
        URLLoader loader = URLLoader.post("http://www.colorize.nl", Charsets.UTF_8);
        loader.setBody("application/json", "{\"a\":2,\"b\":3}");
        
        assertEquals("[application/json]", loader.getHeaders().getValues(HttpHeaders.CONTENT_TYPE).toString());
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
        URLLoader request = URLLoader.get(TEST_HTTPS_URL, Charsets.UTF_8);
        URLResponse response = request.sendRequest();

        assertEquals(3, response.getCertificates().size());
    }

    @Test
    public void testHttpResponseWithContentLengthHeaderZero() throws IOException {
        URLLoader request = URLLoader.get("https://www.colorize-dashboard.nl/rest/website/check/colorize.nl",
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

    @Test
    public void testSendBackgroundRequest() throws InterruptedException {
        List<URLResponse> received = new ArrayList<>();

        URLLoader.get("http://www.colorize.nl", Charsets.UTF_8).sendBackgroundRequest()
            .then(received::add);

        Thread.sleep(2000);

        assertEquals(1, received.size());
    }

    @Test
    void mergeWithExistingQueryParameters() {
        URLLoader request = URLLoader.get("http://www.colorize.nl?a=2", Charsets.UTF_8);
        request.addQueryParam("b", "3");

        assertEquals(2, request.getQueryParams().getData().size());
        assertEquals("2", request.getQueryParams().getData().get("a"));
        assertEquals("3", request.getQueryParams().getData().get("b"));
    }

    @Test
    void httpsRequest() throws IOException {
        URLLoader request = URLLoader.get("https://clrz.nl/");
        URLResponse response = request.sendRequest();

        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void redirectHttpToHttps() throws IOException {
        URLLoader request = URLLoader.get("http://apple.com/");
        URLResponse response = request.sendRequest();

        assertEquals(HttpStatus.OK, response.getStatus());
    }

    private URLResponse toResponse(int status, String contentType, String body) {
        URLResponse response = new URLResponse(status, body, Charsets.UTF_8);
        response.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
        return response;
    }
}
