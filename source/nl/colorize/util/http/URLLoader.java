//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import nl.colorize.util.Escape;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Tuple;

/**
 * Assists in creating and sending HTTP requests. This class is built on top of
 * {@link java.net.HttpURLConnection} but provides a more convenient and high-level
 * API. For example, parameter values are URL-encoded by default, and request
 * parameters are sent either in the query string or as POST data depending on the
 * request method.
 * <p>
 * The name and purpose of this class were originally based on the ActionScript 3
 * class of the same name.
 */
public class URLLoader {
	
	private URL url;
	private Method method;
	private Charset requestCharset;
	private Map<String, String> params;
	private List<Tuple<String, String>> requestHeaders;
	private String requestBody;
	private boolean certificateVerification;
	
	private static final Splitter URL_PARAMETER_SPLITTER = Splitter.on('&').omitEmptyStrings();
	private static final Joiner MULTIPLE_HEADER_JOINER = Joiner.on('\n');
	private static final Logger LOGGER = LogHelper.getLogger(URLLoader.class);
	
	/**
	 * Creates an {@code URLLoader} instance that will send requests to the
	 * specified URL. Note that calling this constructor does not immediately
	 * send a request (see {@link #sendRequest()} or {@link #openConnection()}).
	 * @param requestCharset Character encoding for request parameters.
	 */
	public URLLoader(URL url, Method method, Charset requestCharset) {
		//TODO deprecate using this class for non-HTTP URLs. In practice
		//     this depends on too many features (POST requests, request
		//     headers) that only apply to HTTP and HTTPS, and not to
		//     some "generic" URL handler like "file://".
		this.url = url;
		this.method = method;
		this.requestCharset = requestCharset;
		this.params = new LinkedHashMap<>();
		this.requestHeaders = new ArrayList<>();
		this.requestBody = "";
		this.certificateVerification = true;
		
		// Set default request headers.
		setRequestHeader(HttpHeaders.ACCEPT_CHARSET, requestCharset.displayName());
		if (method.hasRequestBody()) {
			setRequestHeader(HttpHeaders.CONTENT_TYPE, 
					"application/x-www-form-urlencoded;charset=" + requestCharset.displayName());
		}
		
		// Strips out any URL parameters that might be present in the URL,
		// to prevent confusion between parameters in the URL and parameters
		// in the parameters map.
		stripParametersFromURL();
	}
	
	/**
	 * Creates an {@code URLLoader} instance that will send requests to the
	 * specified URL. Note that calling this constructor does not immediately
	 * send a request, see {@link #sendRequest()} or {@link #openConnection()}.
	 * @param requestCharset Character encoding for request parameters.
	 * @throws IllegalArgumentException if {@code url} is not a valid URL.
	 */
	public URLLoader(String url, Method method, Charset requestCharset) {
		this(LoadUtils.toURL(url), method, requestCharset);
	}
	
	private void stripParametersFromURL() {
		String urlString = url.toString();
		if (urlString.indexOf('?') != -1) {
			url = LoadUtils.toURL(urlString.substring(0, urlString.indexOf('?')));
			String queryString = urlString.substring(urlString.indexOf('?') + 1);
			for (String param : URL_PARAMETER_SPLITTER.split(queryString)) {
				if (param.indexOf('=') != -1) {
					addParam(param.substring(0, param.indexOf('=')), 
							param.substring(param.indexOf('=') + 1));
				} else {
					addParam(param, "");
				}
			}
		}
	}
	
	/**
	 * Returns the URL that requests will be sent to. Depending on the request
	 * method, added parameters might end up in the URL's query string.
	 */
	public URL getURL() {
		return LoadUtils.toURL(toString());
	}
	
	public Method getMethod() {
		return method;
	}
	
	public Charset getRequestCharset() {
		return requestCharset;
	}
	
	/**
	 * Adds a request parameter. If the request method is POST this parameter will
	 * be sent as POST data, for other request methods the parameter is appended to
	 * the URL's query string. In both cases parameter values are sent URL-encoded.
	 * If the request already contains a parameter with the same name its value
	 * is replaced.
	 * @throws NullPointerException if parameter name and/or value are null.
	 * @return This, for method chaining.
	 */
	public URLLoader addParam(String name, String value) {
		if (name == null || value == null) {
			throw new NullPointerException();
		}
		params.put(name, value);
		return this;
	}
	
	/**
	 * Adds multiple request parameters. This method is identical to calling
	 * {@link #addParam(String, String)} for all parameters in the map.
	 * @throws NullPointerException if the map contains null keys or values.
	 * @return This, for method chaining.
	 */
	public URLLoader addParams(Map<String, String> params) {
		for (Map.Entry<String, String> param : params.entrySet()) {
			addParam(param.getKey(), param.getValue());
		}
		return this;
	}
	
	public String getParamValue(String name) {
		return params.get(name);
	}
	
	public Map<String, String> getParams() {
		return params;
	}
	
	private String encodeRequestParameters() {
		return Escape.formEncode(params, requestCharset);
	}
	
	/**
	 * Adds a request header with the specified name and value.
	 * @param allowMultiple Handles situations in which a request header with the
	 *        same name has already been set. If true, this is allowed. If false,
	 *        this will replace the value of the header with the new value.
	 * @throws NullPointerException if the header name and/or value are null.
	 * @return This, for method chaining.
	 */
	public URLLoader addRequestHeader(String name, String value, boolean allowMultiple) {
		if (name == null || value == null) {
			throw new NullPointerException();
		}
		
		if (allowMultiple) {
			requestHeaders.add(Tuple.of(name, value));
		} else {
			removeRequestHeader(name);
			requestHeaders.add(Tuple.of(name, value));
		}
		
		return this;
	}
	
	/**
	 * Adds a request header with the specified name and value. Note that unlike
	 * {@link #addRequestHeader(String, String, boolean)} this does not allow
	 * multiple headers with the same name.
	 * @throws IllegalStateException if a request header with the same name has 
	 *         already been set.
	 * @throws NullPointerException if the name and/or value are null.
	 * @return This, for method chaining.
	 */
	public URLLoader setRequestHeader(String name, String value) {
		return addRequestHeader(name, value, false);
	}
	
	/**
	 * Returns the value of the request header with the specified name. Note that
	 * header names are case-insensitive, as defined in the HTTP standard. Returns
	 * {@code null} if no such header has been set.
	 */
	public String getRequestHeader(String name) {
		for (Tuple<String, String> header : requestHeaders) {
			if (header.getLeft().equalsIgnoreCase(name)) {
				return header.getRight();
			}
		}
		return null;
	}
	
	public void removeRequestHeader(String name) {
		Iterator<Tuple<String, String>> iterator = requestHeaders.iterator();
		while (iterator.hasNext()) {
			Tuple<String, String> header = iterator.next();
			if (header.getLeft().equalsIgnoreCase(name)) {
				iterator.remove();
			}
		}
	}
	
	public List<Tuple<String, String>> getRequestHeaders() {
		return ImmutableList.copyOf(requestHeaders);
	}
	
	/**
	 * Sets the request body that will be sent with the request. This will
	 * set the request's Content-Type header to reflect the data type of the
	 * new request body.
	 * @throws IllegalStateException if the request method does not allow a
	 *         request body to be sent.
	 */
	public void setRequestBody(String requestBody, String contentType) {
		if (!method.hasRequestBody()) {
			throw new IllegalArgumentException("Request body not allowed for method " + method);
		}
		
		setRequestHeader(HttpHeaders.CONTENT_TYPE, contentType);
		this.requestBody = requestBody;
	}
	
	/**
	 * Returns the request's respons body. This will normally consist of the
	 * URL-encoded request parameters, unless the response body has been
	 * replaced using {@link #setRequestBody(String, String)}.
	 * @throws IllegalStateException if the request method does not allow a
	 *         request body to be set.
	 */
	public String getRequestBody() {
		if (!method.hasRequestBody()) {
			throw new IllegalArgumentException("Request body not allowed for method " + method);
		}
		
		if (requestBody.isEmpty()) {
			return encodeRequestParameters();
		} else {
			return requestBody;
		}
	}
	
	/**
	 * Adds basic HTTP authentication to the request.
	 * <p>
	 * <b>Security note:</b> This will BASE64-encode the username and password,
	 * and adds them as a HTTP header. BASE64 encoding can easily be reversed,
	 * meaning anyone on the network can obtain the username and password if
	 * sent over an insecure connection.
	 */
	public void setBasicAuthentication(String user, String password) {
		setRequestHeader(HttpHeaders.AUTHORIZATION, 
				"Basic " + Escape.base64Encode(user + ":" + password, requestCharset));
	}
	
	/**
	 * Disables SSL certificate verification performed for HTTPS connections,
	 * ignoring any errors or warnings that may occur while checking the
	 * certificate.
	 * <p>
	 * <b>Security note:</b> Ignoring these errors and warnings means you can
	 * no longer assume communication to be secure. It is strongly recommended
	 * to correct the SSL certificate itself, rather than using HTTPS with an
	 * insecure certificate.
	 * @return This, for method chaining.
	 */
	public URLLoader disableCertificateVerification() {
		certificateVerification = false;
		return this;
	}
	
	/**
	 * Sends a request to the URL and reads the response. Depending on the request
	 * method, the request parameters will be sent in the query string or in the
	 * request body using a Content-Type of application/x-www-form-urlencoded.
	 * However, if the request body or Content-Type header were replaced using
	 * {@link #setRequestBody(String, String)}, those values will be used instead.   
	 * @throws IOException if an I/O error occurs while sending the request, or
	 *         if the URL returns a HTTP status that indicates error (4xx or 5xx).
	 * @throws ProtocolException if the URL's protocol does not accept the request 
	 *         method or request parameters. 
	 */
	public HttpResponse sendRequest() throws IOException {
		URLConnection connection = openConnection();
		return readResponse(connection);
	}
	
	/**
	 * Sends a request to the URL and opens a connection to read the response.
	 * @throws IOException if an I/O error occurs while sending the request, or
	 *         if the URL returns a HTTP status that indicates error (4xx or 5xx).
	 * @throws ProtocolException if the URL's protocol does not accept the request 
	 *         method or request parameters.
	 */
	public URLConnection openConnection() throws IOException {
		return openConnection(getURL());
	}
	
	/**
	 * Sends a request to {@code url} and opens a connection to reads the response.
	 * The URL is passed as a parameter to handle redirects to URLs other than the
	 * original.
	 */
	private URLConnection openConnection(URL target) throws IOException {
		URLConnection connection = target.openConnection();
		prepareConnection(connection);
		
		if (method.hasRequestBody()) {
			sendPostData(connection);
		}
		
		HttpStatus status = readHttpStatus(connection);
		if (status.isClientError() || status.isServerError()) {
			throw new IOException(String.format("HTTP status %d for URL %s", 
					status.getCode(), toString()));
		} else if (status.isRedirection()) {
			return followRedirect(status, connection);
		} else {
			return connection;
		}
	}
	
	private void prepareConnection(URLConnection connection) throws ProtocolException {
		if (connection instanceof HttpURLConnection) {
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setRequestMethod(method.toString());
			httpConnection.setInstanceFollowRedirects(true);
			for (Tuple<String, String> header : requestHeaders) {
				httpConnection.setRequestProperty(header.getLeft(), header.getRight());
			}
		}
		
		if (connection instanceof HttpsURLConnection && !certificateVerification) {
			disableCertificateVerification((HttpsURLConnection) connection);
		}
	}

	private void disableCertificateVerification(HttpsURLConnection connection) {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new X509TrustManager[] { getNoOpTrustMaster() }, new SecureRandom());
			
			connection.setSSLSocketFactory(sslContext.getSocketFactory());
			connection.setHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String host, SSLSession session) {
					return true;
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Cannot disable HTTPS certificate verification", e);
		}
	}

	private X509TrustManager getNoOpTrustMaster() {
		return new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String auth)
					throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain, String auth)
					throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		};
	}

	/**
	 * Sends POST data through the connection. This method can only be called if
	 * the response hasn't been downloaded yet.
	 */
	private void sendPostData(URLConnection connection) throws IOException {
		connection.setDoOutput(true);
			
		try {
			Writer postWriter = new OutputStreamWriter(connection.getOutputStream(), requestCharset);
			postWriter.write(getRequestBody());
			postWriter.flush();
			postWriter.close();
		} catch (UnknownServiceException e) {
			throw new ProtocolException("URL protocol does not accept POST: " + toString());
		}
	}
	
	/**
	 * The default behavior of {@code HttpURLConnection} is to <i>sometimes</i>
	 * follow redirects, depending on the HTTP status and the protocol. This
	 * method will perform additional redirects if the HTTP status indicates that
	 * one should be performed.
	 */
	private URLConnection followRedirect(HttpStatus status, URLConnection connection) throws IOException {
		HttpResponse head = new HttpResponse(status, readResponseHeaders(connection), "");
		String location = head.getHeader(HttpHeaders.LOCATION);
		
		if (location == null || location.isEmpty()) {
			// HTTP status indicates redirect but no alternative location is provided.
			return connection;
		}
		
		try {
			URL redirectURL = new URL(location);
			return openConnection(redirectURL);
		} catch (MalformedURLException e) {
			throw new IOException("Malformed redirect location: " + location);
		}
	}

	/**
	 * Downloads the response headers and body from the specified connection. The
	 * connection is closed afterwards.
	 * @return {@code HttpResponse} containing the downloaded response.
	 * @throws IOException if an I/O error occurs while downloading the response.
	 */
	public HttpResponse readResponse(URLConnection connection) throws IOException {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		Map<String, String> headers = Collections.emptyMap();
		byte[] body = new byte[0];
		
		try {
			// The entire response has to be downloaded first. We cannot read
			// the HTTP status first, as that will call .getInuputStream()
			// internally, and the stream will block in case of Keep-Alive
			// connections.
			body = LoadUtils.readToByteArray(connection.getInputStream());
			status = readHttpStatus(connection);
			headers = readResponseHeaders(connection);
		} finally {
			if (connection instanceof HttpURLConnection) {
				((HttpURLConnection) connection).disconnect();
			}
		}
		
		return new HttpResponse(status, headers, body);
	}
	
	/**
	 * Downloads the HTTP status from the specified connection.
	 * @throws IOException if an I/O error occurs while downloading the response.
	 */
	private HttpStatus readHttpStatus(URLConnection connection) throws IOException {
		try {
			if (connection instanceof HttpURLConnection) {
				HttpURLConnection httpConnection  = (HttpURLConnection) connection;
				int statusCode = httpConnection.getResponseCode();
				return HttpStatus.parse(statusCode);
			} else {
				// This class has limited support for non-HTTP URLs.
				return HttpStatus.OK;
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
	private Map<String, String> readResponseHeaders(URLConnection connection) {
		Map<String, String> responseHeaders = new LinkedHashMap<String, String>();
		for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
			String header = entry.getKey() != null ? entry.getKey() : "";
			String value = MULTIPLE_HEADER_JOINER.join(entry.getValue()); 
			responseHeaders.put(header, value);
		}
		return responseHeaders;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(url.toString());
		if (!method.hasRequestBody() && !params.isEmpty()) {
			sb.append('?');
			sb.append(encodeRequestParameters());
		}
		return sb.toString();
	}

	/**
	 * Convenience factory method for creating {@link Method#GET} requests.
	 */
	public static URLLoader get(String url, Charset requestCharset) {
		return new URLLoader(url, Method.GET, requestCharset);
	}
	
	/**
	 * Convenience factory method for creating {@link Method#POST} requests.
	 */
	public static URLLoader post(String url, Charset requestCharset) {
		return new URLLoader(url, Method.POST, requestCharset);
	}
	
	/**
	 * Convenience factory method for creating {@link Method#PUT} requests.
	 */
	public static URLLoader put(String url, Charset requestCharset) {
		return new URLLoader(url, Method.PUT, requestCharset);
	}
	
	/**
	 * Convenience factory method for creating {@link Method#DELETE} requests.
	 */
	public static URLLoader delete(String url, Charset requestCharset) {
		return new URLLoader(url, Method.DELETE, requestCharset);
	}
}
