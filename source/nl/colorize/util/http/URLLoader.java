//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Preconditions;
import nl.colorize.util.Subject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class that contains convenience methods for creating and sending
 * HTTP requests using the HTTP client in {@code java.net.http}.
 * <p>
 * The name and purpose of this class, which was originally created many years
 * ago, is based on the Flash ActionScript class of the same name. Previous
 * versions of this class provided a cross-platform HTTP client, with various
 * implementations for different platforms. With the introduction of the HTTP
 * client in {@code java.net.http} this is no longer necessary, and all
 * methods in this class now use the standard HTTP client.
 */
public final class URLLoader {

    private static final List<String> HTTP_REQUEST_METHODS = List.of(
        "GET",
        "POST",
        "PUT",
        "DELETE",
        "PATCH",
        "HEAD",
        "OPTIONS"
    );

    private URLLoader() {
    }

    /**
     * Returns a {@link HttpClient} with a default configuration. This method
     * is used by all convenience methods in this class for sending HTTP
     * requests.
     */
    public static HttpClient createClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Returns a {@link HttpClient} with a default configuration and the
     * specified connection timeout.
     */
    public static HttpClient createClient(Duration timeout) {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(timeout)
            .build();
    }

    /**
     * Creates a request with the specified properties. This is a convenience
     * method for creating requests with less boilerplate code than required
     * by {@link HttpRequest.Builder}.
     */
    public static HttpRequest buildRequest(
        String method,
        String url,
        Map<String, String> headers,
        @Nullable String body
    ) {
        Preconditions.checkArgument(HTTP_REQUEST_METHODS.contains(method),
            "Invalid HTTP request method: " + method);

        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create(url));
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        if (body != null && !body.isEmpty()) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return builder.build();
    }

    /**
     * Creates a request with the specified properties, and no request body.
     * This is a convenience method for creating requests with less boilerplate
     * code than required by {@link HttpRequest.Builder}.
     */
    public static HttpRequest buildRequest(String method, String url, Map<String, String> headers) {
        return buildRequest(method, url, headers, null);
    }

    /**
     * Creates a simple request without a request body or headers. This is a
     * convenience method for creating requests with less boilerplate code
     * than required by {@link HttpRequest.Builder}.
     */
    public static HttpRequest buildRequest(String method, String url) {
        return buildRequest(method, url, Map.of(), null);
    }

    /**
     * Sends the specified request using a {@link HttpClient} with the default
     * configuration defined by {@link #createClient()}.
     *
     * @throws HttpException if sending the request succeeded, but the response
     *         contains an HTTP error status code.
     * @throws IOException if an I/O error occurred while sending the request.
     */
    public static HttpResponse<String> send(HttpRequest request) throws IOException {
        try (HttpClient httpClient = createClient()) {
            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(UTF_8));

            if (HttpStatus.isError(response.statusCode())) {
                throw new HttpException(response.statusCode());
            }

            return response;
        } catch (InterruptedException e) {
            throw new IOException("HTTP request interrupted", e);
        }
    }

    /**
     * Sends the specified request in a background thread, using a
     * {@link HttpClient} with the default configuration defined by
     * {@link #createClient()}. Returns a {@link Subject} that can be
     * used to subscribe to the response.
     *
     * @see #send(HttpRequest)
     */
    public static Subject<HttpResponse<String>> sendAsync(HttpRequest request) {
        return Subject.runBackground(() -> send(request));
    }

    /**
     * Creates a request with the specified properties, then sends it using a
     * {@link HttpClient} with the default configuration defined by
     * {@link #createClient()}.
     *
     * @throws HttpException if sending the request succeeded, but the response
     *         contains an HTTP error status code.
     * @throws IOException if an I/O error occurred while sending the request.
     */
    public static HttpResponse<String> send(
        String method,
        String url,
        Map<String, String> headers,
        @Nullable String body
    ) throws IOException {
        HttpRequest request = buildRequest(method, url, headers, body);
        return send(request);
    }

    /**
     * Creates a request with the specified properties, then sends it in a
     * background thread using a {@link HttpClient} with the default
     * configuration defined by {@link #createClient()}. Returns a
     * {@link Subject} that can be used to subscribe to the response.
     *
     * @see #send(String, String, Map, String)
     */
    public static Subject<HttpResponse<String>> sendAsync(
        String method,
        String url,
        Map<String, String> headers,
        @Nullable String body
    ) {
        HttpRequest request = buildRequest(method, url, headers, body);
        return sendAsync(request);
    }

    /**
     * Sends a GET request using a {@link HttpClient} with the default
     * configuration defined by {@link #createClient()}.
     *
     * @see #send(HttpRequest)
     */
    public static HttpResponse<String> get(String url) throws IOException {
        return get(url, Collections.emptyMap());
    }

    /**
     * Sends a GET request using a {@link HttpClient} with the default
     * configuration defined by {@link #createClient()}.
     *
     * @see #send(HttpRequest)
     */
    public static HttpResponse<String> get(String url, Map<String, String> headers) throws IOException {
        HttpRequest request = buildRequest("GET", url, headers, null);
        return send(request);
    }
}
