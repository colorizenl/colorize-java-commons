//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.net.UrlEscapers;
import nl.colorize.util.Subject;
import nl.colorize.util.stats.Tuple;
import nl.colorize.util.stats.TupleList;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

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

    public static final List<String> HTTP_REQUEST_METHODS = List.of(
        "GET",
        "POST",
        "PUT",
        "DELETE",
        "PATCH",
        "HEAD",
        "OPTIONS"
    );

    private static final BiPredicate<String, String> ALL_HEADERS = (name, value) -> true;

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
            return send(httpClient, request);
        }
    }

    /**
     * Sends the specified request using the provided HTTP client, throws an
     * exception if the response contains an HTTP status code indicating an
     * error.
     *
     * @throws HttpException if sending the request succeeded, but the response
     *         contains an HTTP error status code.
     * @throws IOException if an I/O error occurred while sending the request.
     */
    public static HttpResponse<String> send(HttpClient client, HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(UTF_8));
            checkStatus(response);
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
        return Subject.runAsync(() -> send(request));
    }

    /**
     * Sends the specified request in a background thread using the provided
     * HTTP client. Results in an error if the response contains an HTTP
     * status code indicating an error.
     */
    public static Subject<HttpResponse<String>> sendAsync(HttpClient client, HttpRequest request) {
        return Subject.runAsync(() -> send(client, request));
    }

    /**
     * Similar to {@link #send(HttpRequest)}, but does <em>not</em> throw an
     * exception if a response with an HTTP status error code is received.
     * However, an exception is still thrown if a response is never received.
     *
     * @throws IOException if an I/O error occurred while sending the request.
     */
    public static HttpResponse<String> sendUnchecked(HttpRequest request) throws IOException {
        try (HttpClient httpClient = createClient()) {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        } catch (InterruptedException e) {
            throw new IOException("HTTP request interrupted", e);
        }
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
    public static HttpResponse<byte[]> getBinary(String url) throws IOException {
        try (HttpClient httpClient = createClient()) {
            HttpRequest request = buildRequest("GET", url, Collections.emptyMap(), null);
            HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());
            checkStatus(response);
            return response;
        } catch (InterruptedException e) {
            throw new IOException("HTTP request interrupted", e);
        }
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

    /**
     * Appends the specified path components to a URL. The contents of each
     * path component will be URL-encoded.
     *
     * @throws IllegalArgumentException if the base URL already includes a
     *         query string, meaning that path components can no longer be
     *         appended to it.
     */
    public static String appendURL(String baseURL, String... pathComponents) {
        Preconditions.checkArgument(!baseURL.contains("?"),
            "URL already contains a query string: " + baseURL);

        if (pathComponents.length == 0) {
            return baseURL;
        }

        String path = Arrays.stream(pathComponents)
            .filter(pathComponent -> !pathComponent.isEmpty())
            .map(pathComponent -> UrlEscapers.urlPathSegmentEscaper().escape(pathComponent))
            .collect(Collectors.joining("/"));

        String url = baseURL;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += path;
        return url;
    }

    /**
     * Appends the specified query string to a URL. For example, appending the
     * query parameters "a=2" to the URL "https://example.com" will result in
     * "https://example.com?a=2".
     */
    public static String appendURL(String baseURL, PostData queryParams) {
        if (queryParams.isEmpty()) {
            return baseURL;
        } if (baseURL.contains("?")) {
            return baseURL + "&" + queryParams.encode();
        } else {
            return baseURL + "?" + queryParams.encode();
        }
    }

    /**
     * Returns a {@link HttpHeaders} instance based on the contents of the
     * specified map. The order of the headers will be based on the map's
     * iteration order.
     */
    public static HttpHeaders toHeaders(Map<String, String> headers) {
        Map<String, List<String>> headerMap = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (!entry.getKey().isEmpty() && !entry.getValue().isEmpty()) {
                headerMap.put(entry.getKey(), List.of(entry.getValue()));
            }
        }

        return HttpHeaders.of(headerMap, ALL_HEADERS);
    }

    /**
     * Returns a {@link HttpHeaders} instance based on the contents of the
     * specified list of tuples.
     */
    public static HttpHeaders toHeaders(TupleList<String, String> headers) {
        ListMultimap<String, String> headerMap = ArrayListMultimap.create();

        for (Tuple<String, String> entry : headers) {
            headerMap.put(entry.left(), entry.right());
        }

        return HttpHeaders.of(Multimaps.asMap(headerMap), ALL_HEADERS);
    }

    /**
     * Returns a {@link TupleList} based on the contents of the specified
     * {@link HttpHeaders} instance.
     */
    public static TupleList<String, String> getHeaders(HttpHeaders headers) {
        TupleList<String, String> result = new TupleList<>();
        for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
            for (String value : entry.getValue()) {
                result.add(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * Checks the status code of the specified HTTP response, and throws an
     * exception if this indicates an error.
     *
     * @throws HttpException if the HTTP status code indicates a client error
     *         (status code 4xx) or server error (status code 5xx).
     */
    public static void checkStatus(HttpResponse<?> response) throws HttpException {
        if (HttpStatus.isError(response.statusCode())) {
            throw new HttpException(response.statusCode());
        }
    }

    /**
     * Returns a {@link HttpResponse} with the specified properties, without
     * actually sending a request. This method is intended primarily, though
     * not exclusively, for testing purposes.
     */
    public static HttpResponse<String> buildResponse(
        String url,
        int status,
        Map<String, String> headers,
        String body
    ) {
        return new InMemoryResponse(URI.create(url), status, toHeaders(headers), body);
    }

    /**
     * In-memory implementation of the {@link HttpResponse} interface.
     */
    private record InMemoryResponse(
        URI uri,
        int statusCode,
        HttpHeaders headers,
        String body
    ) implements HttpResponse<String> {

        @Override
        public HttpRequest request() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
