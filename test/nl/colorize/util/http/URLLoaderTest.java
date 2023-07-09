//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.swing.Utils2D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static nl.colorize.util.http.URLLoader.PROPERTY_SSL_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

public class URLLoaderTest {
    
    private static final String TEST_HTTPS_URL = "https://www.gazzetta.it";

    // Implementation note:
    // To make sure reading URLs and sending parameters actually works, some of
    // these tests send read requests to real websites. This is obviously not 
    // very robust, since the tests will break if the site changes.

    @BeforeEach
    @AfterEach
    public void reset() {
        System.setProperty(URLLoader.FORCE_LEGACY_HTTP_CLIENT_SYSTEM_PROPERTY, "");
    }
    
    @Test
    public void testLoadTextResponse() throws IOException {
        String response = URLLoader.get("http://www.colorize.nl")
            .send()
            .read(Charsets.UTF_8);

        assertTrue(response.contains("<base href=\"http://www.colorize.nl/\" />"));
        assertTrue(response.contains("</html>"));
    }
    
    @Test
    public void testLoadBinaryResponse() throws IOException {
        URLResponse response = URLLoader.get("http://www.colorize.nl/images/logo.png")
            .send();

        assertNotNull(Utils2D.loadImage(response.openStream()));
    }

    @Test
    public void test404() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl/nothing.jpg");

        assertThrows(IOException.class, loader::send);
    }

    @Test
    public void testGetVariables() throws IOException {
        URLLoader loader = URLLoader.get("http://www.colorize.nl");
        loader.withQueryParam("page", "contact");
        String response = loader.send().read(Charsets.UTF_8);

        assertEquals("http://www.colorize.nl?page=contact", loader.toString());
        assertTrue(response.contains("<h1>Contact"));
    }

    @Test
    public void testUrlEncodeParameters() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl");
        loader.withQueryParam("a", "b c");

        assertEquals("http://www.colorize.nl?a=b+c", loader.toString());
    }

    @Test
    public void testResponseHttpStatus() throws Exception {
        URLLoader loader = URLLoader.get("http://www.colorize.nl");
        URLResponse response = loader.send();

        assertEquals(HttpStatus.OK, response.status());
    }
    
    @Test
    public void testHttps() throws IOException {
        String response = URLLoader.get(TEST_HTTPS_URL)
            .send()
            .read(Charsets.UTF_8);

        assertTrue(response.toLowerCase().contains("gazzetta"));
    }

    @Test
    public void testDisallowNullHeaders() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl");
        assertThrows(IllegalArgumentException.class, () -> loader.withHeader(null, null));
    }
    
    @Test
    public void testDownloadResponseHeaders() throws Exception {
        URLLoader loader = URLLoader.get("http://www.colorize.nl");
        URLResponse response = loader.send();

        assertEquals("text/html; charset=UTF-8", response.getHeader("Content-Type").get());
        assertEquals("0", response.getHeader("Age").get());
        assertEquals("Apache", response.getHeader("Server").get());
        assertEquals("Accept-Encoding", response.getHeader("Vary").get());
    }
    
    @Test
    public void testHttpToHttpsRedirect() throws Exception {
        URLResponse response = URLLoader.get(TEST_HTTPS_URL).send();

        assertEquals(HttpStatus.OK, response.status());
        assertTrue(response.headers().getHeaderNames().contains("strict-transport-security"));
    }

    @Test
    public void testAddQueryParams() {
        URLLoader loader = URLLoader.get("http://www.colorize.nl");
        loader.withQueryParam("a", "2");
        loader.withQueryParam("b", "3");
        loader.withQueryParam("c", "4");

        assertEquals("http://www.colorize.nl?a=2&b=3&c=4", loader.toString());
    }

    @Test
    public void testHeadRequestsOnlyDownloadHeaders() throws IOException {
        URLResponse getResponse = URLLoader.get("http://www.colorize.nl").send();

        assertEquals(HttpStatus.OK, getResponse.status());
        assertFalse(getResponse.read(Charsets.UTF_8).isEmpty());
        
        URLResponse headResponse = new URLLoader(Method.HEAD, "http://www.colorize.nl", Charsets.UTF_8)
            .send();

        assertEquals(HttpStatus.OK, headResponse.status());
        assertTrue(headResponse.read(Charsets.UTF_8).isEmpty());
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
    public void testSingleParameterWithoutName() {
        URLLoader urlLoader = URLLoader.get("http://www.colorize.nl");
        urlLoader.withQueryParam("", "123");

        assertEquals("http://www.colorize.nl?123", urlLoader.toString());
    }

    @Test
    public void testCertificatesForHTTPS() throws IOException {
        URLLoader request = URLLoader.get(TEST_HTTPS_URL);
        URLResponse response = request.send();
        SSLSession sslSession = (SSLSession) response.getConnectionProperty(PROPERTY_SSL_CONTEXT).get();
        Certificate[] certificates = sslSession.getPeerCertificates();

        assertEquals(3, certificates.length);
    }

    @Test
    public void testHttpResponseWithContentLengthHeaderZero() throws IOException {
        URLLoader request = URLLoader.get("https://dashboard.clrz.nl/rest/website/check/colorize.nl");
        URLResponse response = request.send();

        assertEquals(HttpStatus.ACCEPTED, response.status());
        assertEquals("", response.read(Charsets.UTF_8));
    }

    @Test
    public void testSendRequestAsync() throws ExecutionException, InterruptedException {
        URLLoader request = URLLoader.get("http://www.colorize.nl");
        Future<URLResponse> future = request.sendAsync();

        assertEquals(HttpStatus.OK, future.get().status());
    }

    @Test
    void httpsRequest() throws IOException {
        URLLoader request = URLLoader.get("https://clrz.nl/");
        URLResponse response = request.send();

        assertEquals(HttpStatus.OK, response.status());
    }

    @Test
    void redirectHttpToHttps() throws IOException {
        URLLoader request = URLLoader.get("http://apple.com/");
        URLResponse response = request.send();

        assertEquals(HttpStatus.OK, response.status());
    }

    @Test
    void sendPostData() throws IOException {
        URLResponse response = URLLoader.post("https://dashboard.clrz.nl/rest/echo")
            .withHeader(HttpHeaders.USER_AGENT, "test")
            .withBody(PostData.create("message", "1234"))
            .send();

        String expected = """
            {
              "userAgent": "test",
              "platform": "macOS",
              "message": "1234"
            }""";

        assertEquals(HttpStatus.OK, response.status());
        assertEquals(expected, response.getBody());
    }

    @Test
    void classicLoaderGet() throws IOException {
        System.setProperty(URLLoader.FORCE_LEGACY_HTTP_CLIENT_SYSTEM_PROPERTY, "true");

        String response = URLLoader.get("http://www.colorize.nl")
            .send()
            .read(Charsets.UTF_8);

        assertTrue(response.contains("<base href=\"http://www.colorize.nl/\" />"));
        assertTrue(response.contains("</html>"));
    }

    @Test
    void classicLoaderPost() throws IOException {
        System.setProperty(URLLoader.FORCE_LEGACY_HTTP_CLIENT_SYSTEM_PROPERTY, "true");

        URLLoader request = URLLoader.post("https://httpbin.org/post");
        request.withJSON("clrz1234");
        URLResponse response = request.send();

        assertEquals(HttpStatus.OK, response.status());
        assertTrue(response.read(Charsets.UTF_8).contains("\"clrz1234\""),
            response.read(Charsets.UTF_8));
    }

    @Test
    void addAdditionalQueryParams() {
        URLLoader request = URLLoader.get("https://clrz.nl?a=2&b=3");
        request.withQueryParam("c", "4");

        assertEquals("https://clrz.nl?a=2&b=3&c=4", request.toString());
    }

    @Test
    void addPathComponent() {
        assertEquals("https://clrz.nl/a",
            URLLoader.get("https://clrz.nl").withPathComponent("a").toString());

        assertEquals("https://clrz.nl/a",
            URLLoader.get("https://clrz.nl/").withPathComponent("a").toString());

        assertEquals("https://clrz.nl/a",
            URLLoader.get("https://clrz.nl").withPathComponent("/a").toString());

        assertEquals("https://clrz.nl/a",
            URLLoader.get("https://clrz.nl").withPathComponent("/a/").toString());

        assertEquals("https://clrz.nl/a/b",
            URLLoader.get("https://clrz.nl/a").withPathComponent("b").toString());

        assertEquals("https://clrz.nl/a/b",
            URLLoader.get("https://clrz.nl/a/").withPathComponent("b").toString());

        assertEquals("https://clrz.nl/a/b%2Fc",
            URLLoader.get("https://clrz.nl/a/").withPathComponent("b/c").toString());
    }

    @Test
    void disableCertificateValidation() throws IOException {
        URLResponse response = URLLoader.get("https://clrz.nl")
            .disableCertificateValidation()
            .send();

        assertEquals(200, response.status());
    }

    @Test
    void sendWithCallback() throws InterruptedException {
        List<Integer> result = new ArrayList<>();

        URLLoader.get("https://www.colorize.nl/en/")
            .sendPromise()
            .then(response -> result.add(response.status()));

        Thread.sleep(3000);

        assertEquals(List.of(200), result);
    }
}
