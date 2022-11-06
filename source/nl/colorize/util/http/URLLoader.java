//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assists in creating and sending HTTP requests. This class originally intended
 * to provide a more convenient API on top of {@link java.net.HttpURLConnection},
 * but has been updated to act as a unified API for switching between the new
 * HTTP client introduced in Java 11, which supports HTTP/2 but is not supported
 * on all platforms, and the legacy {@link java.net.HttpURLConnection} which has
 * an inconvenient API but is supported everywhere.
 * <p>
 * The automatic detection can be overruled by using the system property
 * {@code colorize.urlloader.classic}. Setting this to true or false will force
 * the usage of the old or new HTTP client respectively.
 * <p>
 * Instances of this class can be obtained using either
 * {@link #create(Method, String, Charset)}, or one of the convenience versions
 * such as {@link #get(String, Charset)} or {@link #post(String, Charset)}. All
 * of these will return a {@link URLLoader} implementation suitable for the
 * current platform.
 * <p>
 * This class only supports the HTTP and HTTPS protocols. Other protocols, such
 * as {@code file://} URLs, are not supported.
 */
public abstract class URLLoader extends HttpMessage {

    private Method method;
    private URI url;
    private PostData queryParameters;

    private int timeout;
    private boolean allowErrorStatus;
    private boolean certificateVerification;

    protected static final String CLASSIC_LOADER_PROPERTY = "colorize.urlloader.classic";

    private static final List<String> SUPPORTED_PROTOCOLS = List.of("http", "https");
    private static final int DEFAULT_TIMEOUT = 30_000;
    private static final Logger LOGGER = LogHelper.getLogger(URLLoader.class);
    
    /**
     * This constructor is intended to be used by subclasses. Refer to the class
     * documentation for instructions on how to obtain an instance of this class.
     *
     * @throws IllegalArgumentException if the URL protocol is not supported,
     *         including relative URLs for which the protocol cannot be determined.
     */
    protected URLLoader(Method method, URI url, Charset encoding) {
        Preconditions.checkArgument(url.isAbsolute(),
            "Sending requests to relative URLs not supported: " + url);

        Preconditions.checkArgument(SUPPORTED_PROTOCOLS.contains(url.getScheme()),
            "URL protocol not supported: " + url);

        this.method = method;
        this.url = stripURL(url);
        this.queryParameters = PostData.empty();

        this.timeout = DEFAULT_TIMEOUT;
        this.allowErrorStatus = false;
        this.certificateVerification = true;
        
        // Default request headers
        addHeader(HttpHeaders.ACCEPT_CHARSET, encoding.displayName());

        initQueryParameters(url);
    }

    private URI stripURL(URI url) {
        if (url.toString().indexOf('?') == -1) {
            return url;
        }

        String baseURL = url.toString().substring(0, url.toString().indexOf('?'));
        return URI.create(baseURL);
    }

    private void initQueryParameters(URI url) {
        if (url.toString().indexOf('?') != -1) {
            String queryString = url.toString().substring(url.toString().indexOf('?'));
            PostData data = PostData.parse(queryString, getEncoding());
            queryParameters = queryParameters.merge(data);
        }
    }

    public Method getMethod() {
        return method;
    }

    public URI getBaseURL() {
        return url;
    }

    public URI getFullURL() {
        if (queryParameters.isEmpty()) {
            return url;
        }

        String queryString = queryParameters.encode(getEncoding());
        return URI.create(url + "?" + queryString);
    }

    /**
     * Adds a query parameter that will be added to the request URL.
     *
     * @throws IllegalArgumentException if the parameter name and/or value
     *         is {@code null}.
     * @throws IllegalStateException if the request already contains a query
     *         parameter with the same name.
     */
    public void addQueryParam(String name, String value) {
        Preconditions.checkArgument(name != null && value != null,
            "Invalid query parameter: " + name + "=" + value);

        Preconditions.checkState(!queryParameters.contains(name),
            "Request already contains query parameter: " + name);

        queryParameters = queryParameters.merge(PostData.create(name, value));
    }

    public PostData getQueryParams() {
        return queryParameters;
    }

    /**
     * Convenience method to add the HTTP basic authentication header to the
     * request.
     * <p>
     * <strong>Security note:</strong> Only use this method when sending
     * requests to HTTP URLs. Do not use it for requests over plain HTTP, as
     * the header can potentially be intercepted by anyone with network access.
     */
    public void setBasicAuthentication(String user, String password) {
        String identity = user + ":" + password;
        String base64 = Base64.getEncoder().encodeToString(identity.getBytes(Charsets.UTF_8));

        getHeaders().replace(HttpHeaders.AUTHORIZATION, "Basic " + base64);
    }

    /**
     * Sets the request timeout to the specified value, in milliseconds.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    protected int getTimeout() {
        return timeout;
    }

    /**
     * Allows response error codes (HTTP status 4xx and 5xx) without throwing
     * an exception.
     */
    public void allowErrorStatus() {
        allowErrorStatus = true;
    }

    protected boolean isAllowErrorStatus() {
        return allowErrorStatus;
    }

    /**
     * Disables SSL certificate verification performed for HTTPS connections,
     * ignoring any errors or warnings that may occur while checking the
     * certificate.
     * <p>
     * <strong>Security note:</strong> Ignoring these errors and warnings means
     * you can no longer assume communication to be secure. It is strongly
     * recommended to correct the SSL certificate itself, rather than using
     * HTTPS with an insecure certificate.
     */
    public void disableCertificateVerification() {
        certificateVerification = false;
    }

    protected boolean hasCertificateVerification() {
        return certificateVerification;
    }

    protected SSLContext getDisabledSSLContext() throws GeneralSecurityException {
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String auth) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String auth) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new X509TrustManager[] { trustManager }, new SecureRandom());
        return sslContext;
    }

    /**
     * Sends the HTTP request and returns the response. The request headers and
     * body are derived from the current configuration of this class.
     * @throws IOException if the request produces a response code of
     *         4xx (client error) or 5xx (server error).
     */
    public abstract URLResponse sendRequest() throws IOException;

    /**
     * Sends the HTTP request asynchronously from a separate thread. Returns
     * a {@code Future} that will produce the response.
     */
    public Future<URLResponse> sendRequestAsync() {
        FutureTask<URLResponse> task = new FutureTask<>(this::sendRequest);

        Thread thread = new Thread(task, "ColorizeJavaCommons-URLLoader");
        thread.start();

        return task;
    }

    /**
     * Creates a new {@link URLLoader} that will send a request using the specified
     * HTTP request method. In addition to this generic factory method a number of
     * convenience methods are provided, e.g. {@link #get(String, Charset)}, 
     * {@link #post(String, Charset)}.
     */
    public static URLLoader create(Method httpMethod, String url, Charset requestCharset) {
        boolean useClassicLoader = Platform.isAndroid();

        if (System.getProperty(CLASSIC_LOADER_PROPERTY) != null) {
            useClassicLoader = System.getProperty(CLASSIC_LOADER_PROPERTY).equals("true");
        }

        if (useClassicLoader) {
            return new ClassicURLLoader(httpMethod, URI.create(url), requestCharset);
        } else {
            return new HttpURLLoader(httpMethod, URI.create(url), requestCharset);
        }
    }

    public static URLLoader get(String url, Charset requestCharset) {
        return create(Method.GET, url, requestCharset);
    }

    public static URLLoader get(String url) {
        return get(url, Charsets.UTF_8);
    }
    
    public static URLLoader post(String url, Charset requestCharset) {
        return create(Method.POST, url, requestCharset);
    }

    public static URLLoader post(String url) {
        return post(url, Charsets.UTF_8);
    }
    
    public static URLLoader put(String url, Charset requestCharset) {
        return create(Method.PUT, url, requestCharset);
    }

    public static URLLoader put(String url) {
        return put(url, Charsets.UTF_8);
    }
    
    public static URLLoader delete(String url, Charset requestCharset) {
        return create(Method.DELETE, url, requestCharset);
    }

    public static URLLoader delete(String url) {
        return delete(url, Charsets.UTF_8);
    }

    /**
     * Enables the use of the classic HTTP client {@code HttpURLConnection},
     * instead of the new HTTP client introduced in Java 11. This method is
     * the programmatic version of using the system property
     * {@code colorize.urlloader.classic}.
     *
     * @deprecated Existing code should be migrated to support the new HTTP
     *             client, as newer featurs such as HTTP/2 will not be
     *             supported by {@code HttpURLConnection}. Unfortunately,
     *             the new HTTP client is not 100% compatible with the old
     *             behavior, so this might require changes in application logic.
     */
    @Deprecated
    public static void useClassicHttpClient() {
        System.setProperty(CLASSIC_LOADER_PROPERTY, "true");
    }

    /**
     * Sends URL requests using the classic {@link java.net.HttpURLConnection}
     * API which is available since Java 1.0 and supported by all platforms.
     */
    private static class ClassicURLLoader extends URLLoader {

        public ClassicURLLoader(Method method, URI url, Charset requestCharset) {
            super(method, url, requestCharset);
        }

        @Override
        public URLResponse sendRequest() throws IOException {
            URI fullURL = getFullURL();
            HttpURLConnection connection = openConnection(fullURL);
            return readResponse(connection);
        }

        /**
         * Sends a request to {@code url} and opens a connection to reads the response.
         * The URL is passed as a parameter to handle redirects to URLs other than the
         * original.
         */
        private HttpURLConnection openConnection(URI target) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) target.toURL().openConnection();
            connection.setRequestMethod(getMethod().toString());
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(getTimeout());
            connection.setReadTimeout(getTimeout());
            for (String header : getHeaders().getNames()) {
                for (String value : getHeaders().getValues(header)) {
                    connection.setRequestProperty(header, value);
                }
            }

            if (connection instanceof HttpsURLConnection && !hasCertificateVerification()) {
                disableCertificateVerification((HttpsURLConnection) connection);
            }

            if (getMethod().hasRequestBody()) {
                connection.setDoOutput(true);

                Writer postWriter = new OutputStreamWriter(connection.getOutputStream(), getEncoding());
                postWriter.write(getBody());
                postWriter.flush();
                postWriter.close();
            }

            int status = readHttpStatus(connection);

            if (!isAllowErrorStatus() && HttpStatus.isError(status)) {
                throw new IOException(String.format("HTTP status %d for URL %s", status, target));
            } else if (HttpStatus.isRedirect(status)) {
                return followRedirect(status, connection);
            } else {
                return connection;
            }
        }

        private void disableCertificateVerification(HttpsURLConnection connection) {
            try {
                connection.setSSLSocketFactory(getDisabledSSLContext().getSocketFactory());
                connection.setHostnameVerifier((host, session) -> true);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Cannot disable HTTPS certificate verification", e);
            }
        }

        /**
         * The default behavior of {@code HttpURLConnection} is to <i>sometimes</i>
         * follow redirects, depending on the HTTP status and the protocol. This
         * method will perform additional redirects if the HTTP status indicates that
         * one should be performed.
         */
        private HttpURLConnection followRedirect(int status, HttpURLConnection connection)
                throws IOException {
            URLResponse head = new URLResponse(status, new byte[0], getEncoding());
            readResponseHeaders(connection, head);
            String location = head.getHeaders().getValue(HttpHeaders.LOCATION);

            if (location == null || location.isEmpty()) {
                // HTTP status indicates redirect but no alternative location is provided.
                return connection;
            }

            try {
                URI redirectURL = URI.create(location);
                return openConnection(redirectURL);
            } catch (MalformedURLException e) {
                throw new IOException("Malformed redirect location: " + location);
            }
        }

        /**
         * Downloads the response headers and body from the specified connection. The
         * connection is closed afterwards.
         * @return {@code URLResponse} containing the downloaded response.
         * @throws IOException if an I/O error occurs while downloading the response.
         */
        private URLResponse readResponse(HttpURLConnection connection) throws IOException {
            URLResponse response = new URLResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                new byte[0], getEncoding());

            try (InputStream responseStream = connection.getInputStream()) {
                // The entire response has to be downloaded first. We cannot read
                // the HTTP status first, as that will call .getInuputStream()
                // internally, and the stream will block in case of Keep-Alive
                // connections.
                byte[] body = responseStream.readAllBytes();
                int status = readHttpStatus(connection);
                response = new URLResponse(status, body, getEncoding());
                readResponseHeaders(connection, response);

                if (connection instanceof HttpsURLConnection) {
                    // If the response contains a body, the body has already
                    // been downloaded at this point. If the response does
                    // not have a body, this redundant is needed to download
                    // the HTTPS certificates.
                    connection.connect();

                    for (Certificate certificate : ((HttpsURLConnection) connection).getServerCertificates()) {
                        response.addCertificate(certificate);
                    }
                }
            }

            return response;
        }

        /**
         * Downloads the HTTP status from the specified connection.
         * @throws IOException if an I/O error occurs while downloading the response.
         */
        private int readHttpStatus(URLConnection connection) throws IOException {
            try {
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection  = (HttpURLConnection) connection;
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

        /**
         * Downloads the response headers from the specified open connection and
         * returns them as a map. If the same header is set multiple times the
         * map's value will contain all values, separated by newlines.
         */
        private void readResponseHeaders(HttpURLConnection connection, URLResponse response) {
            for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    for (String value : entry.getValue()) {
                        response.addHeader(entry.getKey(), value);
                    }
                }
            }
        }
    }

    /**
     * Sends URL requests using the new HTTP client that was introduced in Java 11.
     * This sends HTTP/2 requests by default, but also supports HTTP/1.1 to ensure
     * backwards compatibility.
     */
    private static class HttpURLLoader extends URLLoader {

        public HttpURLLoader(Method method, URI url, Charset requestCharset) {
            super(method, url, requestCharset);
        }

        @Override
        public URLResponse sendRequest() throws IOException {
            try {
                HttpClient httpClient = createClient();
                HttpRequest request = createRequest();
                URLResponse response = convertResponse(httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray()));

                if (!isAllowErrorStatus() && HttpStatus.isError(response.getStatus())) {
                    throw new IOException("URL " + getFullURL() +
                        " returns HTTP status " + response.getStatus());
                }

                return response;
            } catch (GeneralSecurityException | InterruptedException e) {
                throw new RuntimeException("Sending HTTP request failed", e);
            }
        }

        private HttpClient createClient() throws GeneralSecurityException {
            HttpClient.Builder builder = HttpClient.newBuilder();
            builder.followRedirects(HttpClient.Redirect.ALWAYS);
            builder.connectTimeout(Duration.ofMillis(getTimeout()));
            builder.version(HttpClient.Version.HTTP_1_1);

            if (!hasCertificateVerification()) {
                builder.sslContext(getDisabledSSLContext());
            }

            return builder.build();
        }

        private HttpRequest createRequest() {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(getFullURL());

            if (getMethod().hasRequestBody()) {
                requestBuilder.method(getMethod().toString(),
                    HttpRequest.BodyPublishers.ofString(getBody(), getEncoding()));
            } else {
                requestBuilder.method(getMethod().toString(), HttpRequest.BodyPublishers.noBody());
            }

            for (String header : getHeaders().getNames()) {
                for (String value : getHeaders().getValues(header)) {
                    requestBuilder.header(header, value);
                }
            }

            return requestBuilder.build();
        }

        private URLResponse convertResponse(java.net.http.HttpResponse<byte[]> response) {
            URLResponse result = new URLResponse(response.statusCode(), response.body(), getEncoding());

            for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
                if (!entry.getKey().equals(":status")) {
                    for (String value : entry.getValue()) {
                        result.addHeader(entry.getKey(), value);
                    }
                }
            }

            response.sslSession().ifPresent(ssl -> {
                try {
                    for (Certificate certificate : ssl.getPeerCertificates()) {
                        result.addCertificate(certificate);
                    }
                } catch (SSLPeerUnverifiedException e) {
                    LOGGER.warning("Cannot read HTTP certificate: " + e.getMessage());
                }
            });

            return result;
        }
    }
}
