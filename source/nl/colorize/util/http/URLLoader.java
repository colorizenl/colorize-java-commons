//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;
import com.google.common.net.UrlEscapers;
import nl.colorize.util.Callback;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;
import nl.colorize.util.PlatformFamily;
import nl.colorize.util.TextUtils;
import nl.colorize.util.TupleList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-platform API for building HTTP requests, then sending those requests
 * using a HTTP client suitable for the current platform.
 * <p>
 * Depending on the platform, this class will automatically choose between using
 * {@link java.net.http.HttpClient} and {@link java.net.HttpURLConnection}. The
 * former was introduced in Java 11 and supports HTTP/2. However, it is not yet
 * fully supported across all platforms and environments. On such platforms, the
 * legacy HTTP client is used instead.
 * <p>
 * The auto-detect mechanism can be overruled using the
 * {@link #FORCE_LEGACY_HTTP_CLIENT_SYSTEM_PROPERTY} system property. This is a
 * global setting that affects all {@code URLLoader} instances. It is not
 * possible to override the auto-detect mechanism on a per-request basis, since
 * this would be akin to just using the preferred HTTP client directly.
 * <p>
 * This class will automatically retry failed requests. The retry mechanism is
 * provided by {@link Retry}. The retry behavior can be disabled or changed
 * using {@link #withAttempts(int)}.
 * <p>
 * This class only supports URL requests for the HTTP and HTTPS protocols. Other
 * protocols, such as {@code file://} URLs, are not supported.
 * <p>
 * Though the purpose of this class is to provide a consistent API on top of the
 * two HTTP clients, the name is based on the Flash ActionScript class of the
 * same name.
 */
public class URLLoader {

    private Method method;
    private URI url;
    private PostData queryParameters;
    private Headers headers;
    private byte[] body;
    private Charset encoding;
    private int attempts;
    private int timeout;
    private boolean allowErrorStatus;
    private boolean certificateValidation;

    public static final String PROPERTY_SSL_CONTEXT = "sslContext";

    private static final List<String> SUPPORTED_PROTOCOLS = List.of("http", "https");
    private static final int DEFAULT_TIMEOUT = 30_000;
    private static final Logger LOGGER = LogHelper.getLogger(URLLoader.class);

    /**
     * System property to override the automatication HTTP client detection,
     * and forces the usage of the classic {@link HttpURLConnection} when set
     * to true.
     *
     * @deprecated Applications should prefer automatic detection, to avoid
     *             ending up with a HTTP client that is not actually supported
     *             on the current platform.
     */
    @Deprecated
    public static final String FORCE_LEGACY_HTTP_CLIENT_SYSTEM_PROPERTY = "colorize.urlloader.classic";

    /**
     * Starts building a URL request. In addition to this constructor, this class
     * also provides a number of factory methods.
     *
     * @throws IllegalArgumentException if the URL cannot be parsed into a
     *         valid HTTP or HTTPS URL.
     */
    public URLLoader(Method method, URI url, Charset encoding) {
        Preconditions.checkArgument(url.isAbsolute(),
            "Relative URL not supported: " + url);

        Preconditions.checkArgument(SUPPORTED_PROTOCOLS.contains(url.getScheme()),
            "URL protocol not supported: " + url);

        this.method = method;
        this.encoding = encoding;
        this.parseInitialURL(url);
        this.headers = new Headers();
        this.body = new byte[0];
        this.attempts = 2;
        this.timeout = DEFAULT_TIMEOUT;
        this.allowErrorStatus = false;
        this.certificateValidation = true;
        
        // Default request headers.
        withHeader(HttpHeaders.ACCEPT_CHARSET, encoding.displayName());
        withHeader("X-Colorize-Platform", Platform.getPlatformName());
    }

    /**
     * Starts building a URL request. In addition to this constructor, this class
     * also provides a number of factory methods.
     *
     * @throws IllegalArgumentException if the URL cannot be parsed into a
     *         valid HTTP or HTTPS URL.
     */
    public URLLoader(Method method, String url, Charset encoding) {
        this(method, URI.create(url), encoding);
    }

    private void parseInitialURL(URI initial) {
        int queryStringIndex = initial.toString().indexOf('?');

        if (queryStringIndex == -1) {
            url = initial;
            queryParameters = PostData.empty();
        } else {
            String baseURL = initial.toString().substring(0, queryStringIndex);
            url = URI.create(baseURL);

            String queryString = initial.toString().substring(queryStringIndex);
            queryParameters = PostData.parse(queryString, encoding);
        }
    }

    /**
     * Appends the specified path component to the end of the URL. The path
     * component is allowed to include a leading or trailing slash. Any other
     * characters, including slashes, will be encoded.
     *
     * @throws IllegalArgumentException if the combination of the existing URL
     *         plus the new path component leads to an invalid URL.
     */
    public URLLoader withPathComponent(String pathComponent) {
        pathComponent = TextUtils.removeLeading(pathComponent, "/");
        pathComponent = TextUtils.removeTrailing(pathComponent, "/");

        String urlString = url.toString();
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        urlString += UrlEscapers.urlPathSegmentEscaper().escape(pathComponent);

        url = URI.create(urlString);

        return this;
    }

    /**
     * Adds the specified request header. If the {@code replace} option is true,
     * this will replace any headers with the same name. If the option is false,
     * it will simply add the new header, leaving any existing headers with the
     * same name in place.
     */
    public URLLoader withHeader(String name, String value, boolean replace) {
        Preconditions.checkArgument(name != null && !name.isEmpty(), "Empty header name");
        Preconditions.checkArgument(value != null, "Null header value");

        headers = replace ? headers.replace(name, value) : headers.append(name, value);

        return this;
    }

    /**
     * Adds the specified request header. This method is the equivalent of using
     * {@code withHeader(name, value, false}.
     */
    public URLLoader withHeader(String name, String value) {
        return withHeader(name, value, false);
    }

    /**
     * Adds the specified request headers. This method is the equivalent of using
     * {@code withHeader(name, value, false} on all headers in the map.
     */
    public URLLoader withHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            withHeader(entry.getKey(), entry.getValue(), false);
        }

        return this;
    }

    /**
     * Adds the specified request headers. This method is the equivalent of using
     * {@code withHeader(name, value, false} on all headers.
     */
    public URLLoader withHeaders(Headers additional) {
        additional.forEach((name, value) -> withHeader(name, value, false));
        return this;
    }

    /**
     * Uses the specified request body and sets the Content-Type accordingly.
     * See the {@link #withBody(PostData)}, {@link #withJSON(String)}, and
     * {@link #withXML(String)} convenience methods for common request body
     * types.
     */
    public URLLoader withBody(String body, String contentType) {
        withHeader(HttpHeaders.CONTENT_TYPE, contentType, true);
        this.body = body.getBytes(encoding);
        return this;
    }

    /**
     * Uses the specified form data as the request body, and changes the
     * Content-Type header to "application/x-www-form-urlencoded".
     */
    public URLLoader withBody(PostData data) {
        return withBody(data.encode(encoding), "application/x-www-form-urlencoded");
    }

    /**
     * Uses the specified XML request body, and changes the Content-Type header
     * to "text/xml".
     */
    public URLLoader withXML(String xml) {
        return withBody(xml, "text/xml");
    }

    /**
     * Uses the specified JSON request body, and changes the Content-Type header
     * to "application/json".
     */
    public URLLoader withJSON(String json) {
        return withBody(json, "application/json");
    }

    public URLLoader withQueryParam(String name, String value) {
        Preconditions.checkArgument(name != null && value != null,
            "Invalid query parameter: " + name + "=" + value);

        Preconditions.checkState(!queryParameters.contains(name),
            "Request already contains query parameter: " + name);

        queryParameters = queryParameters.merge(PostData.create(name, value));

        return this;
    }

    /**
     * Convenience method to add the HTTP basic authentication header to the
     * request.
     * <p>
     * <strong>Security note:</strong> Only use this method when sending
     * requests to HTTP URLs. Do not use it for requests over plain HTTP, as
     * the header can potentially be intercepted by anyone with network access.
     */
    public URLLoader withBasicAuth(String user, String password) {
        String identity = user + ":" + password;
        String base64 = Base64.getEncoder().encodeToString(identity.getBytes(Charsets.UTF_8));
        headers = headers.replace(HttpHeaders.AUTHORIZATION, "Basic " + base64);
        return this;
    }

    /**
     * Changes the number of attempts this {@code URLLoader} will perform. A
     * number larger than 1 means automatic retry for failed requests.
     */
    public URLLoader withAttempts(int attempts) {
        Preconditions.checkArgument(timeout >= 1, "Invalid numer of attempts: " + attempts);
        this.attempts = attempts;
        return this;
    }

    public URLLoader withTimeout(int timeout) {
        Preconditions.checkArgument(timeout > 0, "Invalid timeout: " + timeout);
        this.timeout = timeout;
        return this;
    }

    /**
     * Allows response error codes (HTTP status 4xx and 5xx). This allows the
     * {@code URLLoader} to process the response for such a request, instead of
     * throwing an exception.
     */
    public URLLoader allowErrorStatus() {
        allowErrorStatus = true;
        return this;
    }

    /**
     * Disables SSL certificate validation performed for HTTPS connections,
     * ignoring any errors or warnings that may occur while checking the
     * certificate.
     * <p>
     * <strong>Security note:</strong> Ignoring these errors and warnings means
     * you can no longer assume communication to be secure. It is strongly
     * recommended to correct the SSL certificate itself, rather than using
     * HTTPS with an insecure certificate.
     */
    public URLLoader disableCertificateValidation() {
        certificateValidation = false;
        return this;
    }

    private URI getFullURL() {
        if (queryParameters.isEmpty()) {
            return url;
        }

        String queryString = queryParameters.encode(encoding);
        return URI.create(url + "?" + queryString);
    }

    /**
     * Sends the HTTP request and returns the response. This method will
     * automatically select a suitable HTTP client for the current platform,
     * as described in the class documentation.
     *
     * @throws IOException if the request failes, of if the request produces
     *         HTTP status 4xx (client error) or 5xx (server error). However,
     *         if {@link #allowErrorStatus()} was used, no such exception
     *         will be thrown.
     */
    public URLResponse send() throws IOException {
        try {
            Retry retry = new Retry(attempts);
            return retry.attempt(this::sendRequest);
        } catch (ExecutionException e) {
            throw new IOException("Sending HTTP request failed", e);
        }
    }

    /**
     * Internal version of {@link #send()} that is used by the retry mechanism
     * when attempting to perform the request.
     */
    private URLResponse sendRequest() throws IOException {
        boolean force = System.getProperty(FORCE_LEGACY_HTTP_CLIENT_SYSTEM_PROPERTY, "").equals("true");
        PlatformFamily platform = Platform.getPlatformFamily();
        boolean classicHttpClient = platform.isBrowser() || platform.isMobile() || force;

        if (classicHttpClient) {
            URI fullURL = getFullURL();
            HttpURLConnection connection = openConnection(fullURL);
            return readResponse(connection);
        } else {
            HttpClient httpClient = createHttpClient();
            HttpRequest httpRequest = convertRequest();
            return sendRequest(httpClient, httpRequest);
        }
    }

    /**
     * Asynchronous version of {@link #send()} that sends the request from a
     * separate thread.
     */
    public Future<URLResponse> sendAsync() {
        FutureTask<URLResponse> task = new FutureTask<>(this::send);

        Thread thread = new Thread(task, "Colorize-URLLoader");
        thread.start();

        return task;
    }

    /**
     * Asynchronous version of {@link #send()} that sends the request from a
     * separate thread and invokes a callback with the result.
     */
    public void sendAsync(Callback<URLResponse> callback) {
        Runnable task = () -> {
            try {
                URLResponse response = send();
                callback.onResponse(response);
            } catch (IOException e) {
                callback.onError(e);
            }
        };

        Thread thread = new Thread(task, "Colorize-URLLoader");
        thread.start();
    }

    private HttpURLConnection openConnection(URI target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) target.toURL().openConnection();
        connection.setRequestMethod(method.toString());
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        headers.forEach(connection::setRequestProperty);

        if (connection instanceof HttpsURLConnection && !certificateValidation) {
            disableCertificateVerification((HttpsURLConnection) connection);
        }

        if (method.hasRequestBody() && body.length > 0) {
            sendBody(connection);
        }

        int status = readHttpStatus(connection);

        if (!allowErrorStatus && HttpStatus.isError(status)) {
            throw new IOException(String.format("HTTP status %d for URL %s", status, target));
        } else if (HttpStatus.isRedirect(status)) {
            return followRedirect(connection);
        } else {
            return connection;
        }
    }

    private void sendBody(HttpURLConnection connection) throws IOException {
        connection.setDoOutput(true);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
            output.flush();
        }
    }

    /**
     * The default behavior of {@code HttpURLConnection} is to *sometimes*
     * follow redirects, depending on the HTTP status and the protocol. This
     * method will perform additional redirects if the HTTP status indicates
     * that one should be performed.
     */
    private HttpURLConnection followRedirect(HttpURLConnection connection) throws IOException {
        List<String> location = connection.getHeaderFields().get(HttpHeaders.LOCATION);

        if (location == null || location.isEmpty() || location.get(0).isEmpty()) {
            // HTTP status indicates redirect but no alternative location is provided.
            return connection;
        }

        try {
            URI redirectURL = URI.create(location.get(0));
            return openConnection(redirectURL);
        } catch (MalformedURLException e) {
            throw new IOException("Malformed redirect location: " + location);
        }
    }

    private int readHttpStatus(URLConnection connection) throws IOException {
        try {
            if (connection instanceof HttpURLConnection httpConnection) {
                return httpConnection.getResponseCode();
            } else {
                // This class has limited support for non-HTTP URLs.
                return 200;
            }
        } catch (IllegalArgumentException e) {
            // In these cases the URL produces a numeric HTTP status,
            // but with a status code that doesn't exist.
            throw new IOException("Invalid HTTP status", e);
        } catch (RuntimeException e) {
            // HttpURLConnection throws a RuntimeException when the HTTP headers
            // contain a redirect to a malformed URL.
            // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6536522
            throw new IOException("Malformed URL in response header", e);
        }
    }

    private URLResponse readResponse(HttpURLConnection connection) throws IOException {
        try (InputStream responseStream = connection.getInputStream()) {
            // The entire response has to be downloaded first. We cannot read
            // the HTTP status first, as that will call getInputStream()
            // internally, and the stream will block in case of Keep-Alive
            // connections.
            byte[] body = responseStream.readAllBytes();
            int status = readHttpStatus(connection);
            Headers headers = readResponseHeaders(connection);
            Map<String, Object> connectionProperties = new HashMap<>();

            if (connection instanceof HttpsURLConnection https) {
                // If the response contains a body, the body has already
                // been downloaded at this point. If the response does
                // not have a body, this redundant is needed to download
                // the HTTPS certificates.
                connection.connect();

                https.getSSLSession().ifPresent(sslContext -> {
                    connectionProperties.put(PROPERTY_SSL_CONTEXT, sslContext);
                });
            }

            return new URLResponse(status, headers, body, encoding, connectionProperties);
        }
    }

    private Headers readResponseHeaders(HttpURLConnection connection) {
        TupleList<String, String> headers = TupleList.create();

        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                for (String value : entry.getValue()) {
                    headers.add(entry.getKey(), value);
                }
            }
        }

        return new Headers(headers);
    }

    private void disableCertificateVerification(HttpsURLConnection connection) {
        try {
            connection.setSSLSocketFactory(getDisabledSSLContext().getSocketFactory());
            connection.setHostnameVerifier((host, session) -> true);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot disable HTTPS certificate verification", e);
        }
    }

    private SSLContext getDisabledSSLContext() {
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String auth) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String auth) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new UnsupportedOperationException("Failed to disable SSL context", e);
        }
    }

    private HttpClient createHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.followRedirects(HttpClient.Redirect.ALWAYS);
        builder.connectTimeout(Duration.ofMillis(timeout));
        builder.version(HttpClient.Version.HTTP_1_1);
        if (!method.hasRequestBody()) {
            builder.sslContext(getDisabledSSLContext());
        }
        return builder.build();
    }

    private HttpRequest convertRequest() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(getFullURL());

        if (method.hasRequestBody()) {
            requestBuilder.method(method.toString(), HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            requestBuilder.method(method.toString(), HttpRequest.BodyPublishers.noBody());
        }

        headers.forEach(requestBuilder::header);

        return requestBuilder.build();
    }

    private URLResponse sendRequest(HttpClient httpClient, HttpRequest request) throws IOException {
        try {
            HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();

            if (!allowErrorStatus && HttpStatus.isError(status)) {
                throw new IOException("URL " + getFullURL() + " returns HTTP status " + status);
            }

            return convertResponse(response);
        } catch (InterruptedException e) {
            throw new IOException("HTTP request interrupted", e);
        }
    }

    private URLResponse convertResponse(HttpResponse<byte[]> response) {
        int status = response.statusCode();
        Headers headers = new Headers();
        Map<String, Object> connectionProperties = populateConnectionProperties(response);

        for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
            if (!entry.getKey().equals(":status")) {
                for (String value : entry.getValue()) {
                    headers = headers.append(entry.getKey(), value);
                }
            }
        }

        return new URLResponse(status, headers, response.body(), encoding, connectionProperties);
    }

    private Map<String, Object> populateConnectionProperties(HttpResponse<byte[]> response) {
        if (response.sslSession().isPresent()) {
            SSLSession sslContext = response.sslSession().get();
            return Map.of(PROPERTY_SSL_CONTEXT, sslContext);
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public String toString() {
        return getFullURL().toString();
    }

    /**
     * Factory method that creates a GET request to the specified URL. The
     * request will use the UTF-8 character encoding.
     */
    public static URLLoader get(String url) {
        return new URLLoader(Method.GET, URI.create(url), Charsets.UTF_8);
    }

    /**
     * Factory method that creates a POST request to the specified URL. The
     * request will use the UTF-8 character encoding.
     */
    public static URLLoader post(String url) {
        return new URLLoader(Method.POST, URI.create(url), Charsets.UTF_8);
    }

    /**
     * Factory method that creates a PUT request to the specified URL. The
     * request will use the UTF-8 character encoding.
     */
    public static URLLoader put(String url) {
        return new URLLoader(Method.PUT, URI.create(url), Charsets.UTF_8);
    }

    /**
     * Factory method that creates a DELETE request to the specified URL. The
     * request will use the UTF-8 character encoding.
     */
    public static URLLoader delete(String url) {
        return new URLLoader(Method.DELETE, URI.create(url), Charsets.UTF_8);
    }
}
