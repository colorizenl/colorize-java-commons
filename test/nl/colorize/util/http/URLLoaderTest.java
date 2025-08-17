//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link URLLoader} class. Most of these tests originate
 * from a time when {@link URLLoader} was a cross-platform wrapper around
 * various HTTP client implementations. As such, these tests depend on several
 * real-world websites. {@link URLLoader} is now just a wrapper around the
 * standard HTTP client, but these tests are left in place to ensure backward
 * compatibility with the older versions of the class.
 */
public class URLLoaderTest {
    
    private static final String TEST_HTTPS_URL = "https://www.gazzetta.it";

    @Test
    public void testLoadTextResponse() throws IOException {
        String response = URLLoader.get("https://clrz.nl").body();

        assertTrue(response.contains("<div class=\"content\">"));
        assertTrue(response.contains("</html>"));
    }

    @Test
    public void test404() {
        assertThrows(HttpException.class, () -> URLLoader.get("https://clrz.nl/nothing.jpg"));
    }

    @Test
    public void testResponseHttpStatus() throws Exception {
        HttpResponse<String> response = URLLoader.get("http://www.colorize.nl");

        assertEquals(HttpStatus.OK, response.statusCode());
    }
    
    @Test
    public void testHttps() throws IOException {
        String body = URLLoader.get(TEST_HTTPS_URL).body();

        assertTrue(body.toLowerCase().contains("gazzetta"));
    }

    @Test
    public void testDownloadResponseHeaders() throws Exception {
        HttpResponse<String> response = URLLoader.get("https://clrz.nl");

        assertEquals("text/html", response.headers().firstValue("Content-Type").get());
        assertEquals("Apache", response.headers().firstValue("Server").get());
    }
    
    @Test
    public void testHttpToHttpsRedirect() throws Exception {
        HttpResponse<String> response = URLLoader.get("http://clrz.nl");

        assertEquals(HttpStatus.OK, response.statusCode());
        assertTrue(response.sslSession().isPresent());
    }

    @Test
    public void testHeadRequestsOnlyDownloadHeaders() throws IOException {
        HttpResponse<String> response = URLLoader.send("HEAD", "http://www.colorize.nl",
            Map.of(), null);

        assertEquals(HttpStatus.OK, response.statusCode());
        assertTrue(response.body().isEmpty());
    }

    @Test
    public void testCertificatesForHTTPS() throws IOException {
        HttpResponse<String> response = URLLoader.get(TEST_HTTPS_URL);
        Certificate[] certificates = response.sslSession().get().getPeerCertificates();

        assertEquals(3, certificates.length);
    }

    @Test
    public void testHttpResponseWithContentLengthHeaderZero() throws IOException {
        HttpResponse<String> response = URLLoader.get(
            "https://dashboard.clrz.nl/rest/website/check/colorize.nl");

        assertEquals(HttpStatus.ACCEPTED, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void redirectHttpToHttps() throws IOException {
        HttpResponse<String> response = URLLoader.get("http://apple.com/");

        assertEquals(HttpStatus.OK, response.statusCode());
    }

    @Test
    void sendPostData() throws IOException {
        HttpResponse<String> response = URLLoader.send("POST", "https://dashboard.clrz.nl/rest/echo",
            Map.of(), PostData.create("message", "1234").encode());

        assertEquals(HttpStatus.OK, response.statusCode());
        assertTrue(response.body().contains("\"message\": \"1234\""),
            "Response was:\n" + response.body());
    }

    @Test
    void sendWithCallback() throws InterruptedException {
        List<Integer> result = new ArrayList<>();

        URLLoader.sendAsync("GET", "https://clrz.nl", Map.of(), null)
            .subscribe(response -> result.add(response.statusCode()));

        Thread.sleep(5000);

        assertEquals(List.of(200), result);
    }
}
