//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.mock;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Tuple;
import nl.colorize.util.http.HttpStatus;
import nl.colorize.util.http.Method;
import nl.colorize.util.http.URLResponse;
import nl.colorize.util.rest.RestRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple implementation of an embedded HTTP server that can be used for testing.
 * When running, the server will open a socket on the requested port and will 
 * listen for requests.
 * <p>
 * This server is purely intended for testing and lacks support for 
 * several features that can be expected from real HTTP servers beyond simple
 * request/response interaction, such as sessions or multipart requests or HTTPS
 * connections. Although this class is thread safe and can be accessed from 
 * multiple threads. Handling requests is done from a single thread, meaning that 
 * the server is only able to handle one request at a time.
 */
public class SimpleHttpServer {
    
    private ServerSocket serverSocket;
    private AtomicBoolean running;
    private Map<String, URLResponse> expected;
    
    private static final String PROTOCOL = "HTTP/1.1";
    private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("(\\w+)\\s+(\\S+)\\s+(\\S+)");
    private static final Pattern HEADER_PATTERN = Pattern.compile("([\\w-]+)[:]\\s*(\\S.*)");
    private static final Charset CHARSET = Charsets.UTF_8;
    private static final int READ_TIMEOUT = 5000;
    private static final long INTERVAL = 100;
    private static final Logger LOGGER = LogHelper.getLogger(SimpleHttpServer.class);

    public SimpleHttpServer() {
        running = new AtomicBoolean(false);
        expected = new ConcurrentHashMap<>();
    }
    
    public void start(final int port) {
        if (serverSocket != null) {
            throw new IllegalStateException("HTTP server is already running");
        }
        
        LOGGER.info("Starting simple HTTP server on port " + port);

        Thread serverThread = new Thread(() -> listenForRequests(port), "SimpleHttpServer");
        serverThread.start();
    }
    
    public void stop(boolean blockUntilShutdown) {
        LOGGER.info("Stopping simple HTTP server");
        LoadUtils.closeAndIgnore(serverSocket);
        
        if (blockUntilShutdown) {
            while (running.get());
        }
    }
    
    private void listenForRequests(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
                Thread.sleep(INTERVAL);
            }
        } catch (Exception e) {
            if (serverSocket == null || !serverSocket.isClosed()) {
                LOGGER.log(Level.WARNING, "Exception while handling request", e);
            }
        } finally {
            LoadUtils.closeQuietly(serverSocket);
            running.set(false);
        }
    }
    
    private void handleRequest(Socket client) throws IOException {
        client.setSoTimeout(READ_TIMEOUT);
        InputStreamReader in = new InputStreamReader(client.getInputStream(), CHARSET);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), CHARSET));
        
        RestRequest request = parseRequest(in);
        URLResponse response = handleRequest(request);
        sendResponse(response, out);
        
        out.close();
        in.close();
        client.close();
    }

    private RestRequest parseRequest(InputStreamReader in) throws IOException {
        Tuple<Method, String> requestLine = parseRequestLine(readLine(in));
        Map<String, String> headers = readHeaders(in);
        
        String body = null;
        if (requestLine.getLeft().hasRequestBody()) {
            body = readBody(in, headers);
        }

        MockRestRequest request = new MockRestRequest(requestLine.getLeft(), requestLine.getRight(), body);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }

        return request;
    }

    private Map<String, String> readHeaders(InputStreamReader in) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        while (true) {
            String line = readLine(in);
            if (line.isEmpty()) {
                break;
            }
            parseHeaderLine(line, headers);
        }
        return headers;
    }
    
    private String readBody(InputStreamReader in, Map<String, String> headers) throws IOException {
        long contentLength = Long.parseLong(headers.get(HttpHeaders.CONTENT_LENGTH));
        if (contentLength <= 0) {
            throw new IOException("Unknown request body length");
        }
        
        StringBuilder body = new StringBuilder();
        for (long i = 0; i < contentLength; i++) {
            int c = in.read();
            if (c == -1) {
                break;
            }
            body.append((char) c);
        }
        
        return body.toString();
    }

    private String readLine(InputStreamReader in) throws IOException {
        StringBuilder buffer = new StringBuilder();
        while (true) {
            int c = in.read();
            if (c == -1 || ((char) c) == '\n') {
                break;
            }
            buffer.append((char) c);
        }
        return CharMatcher.is('\r').trimTrailingFrom(buffer.toString());
    }
    
    private Tuple<Method, String> parseRequestLine(String line) throws IOException {
        Matcher matcher = REQUEST_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IOException("Invalid request line: " + line);
        }
        
        if (!PROTOCOL.equals(matcher.group(3))) {
            throw new IOException("Protocol not supported: " + matcher.group(3));
        }
        
        return Tuple.of(Method.parse(matcher.group(1)), matcher.group(2)); 
    }
    
    private void parseHeaderLine(String line, Map<String, String> headers) throws IOException {
        Matcher matcher = HEADER_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IOException("Invalid header: " + line);
        }
        headers.put(matcher.group(1), matcher.group(2));
    }

    private URLResponse handleRequest(RestRequest request) {
        URLResponse expectedResponse = expected.get(request.getPath());
        if (expectedResponse != null) {
            return expectedResponse;
        } else {
            return createDefaultResponse(request);
        }
    }
    
    private URLResponse createDefaultResponse(RestRequest request) {
        URLResponse response = new URLResponse(HttpStatus.OK, serialize(request), CHARSET);
        response.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain;charset=" + CHARSET.displayName());
        return response;
    }

    private void sendResponse(URLResponse response, PrintWriter out) {
        out.print(serialize(response));
    }
    
    protected String serialize(RestRequest request) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(request.getMethod() + " " + request.getPath() + " " + PROTOCOL + "\r\n");
        for (String header : request.getHeaderNames()) {
            for (String value : request.getHeaderValues(header)) {
                buffer.append(header + ": " + value + "\r\n");
            }
        }
        buffer.append("\r\n");
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            buffer.append(request.getBody());
        }
        return buffer.toString();
    }
    
    protected String serialize(URLResponse response) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(PROTOCOL + " " + response.getStatus().getCode() + " " + 
            response.getStatus().getDescription() + "\r\n");

        for (String header : response.getHeaderNames()) {
            for (String value : response.getHeaderValues(header)) {
                buffer.append(header + ": " + value + "\r\n");
            }
        }

        buffer.append("\r\n");
        buffer.append(response.getBody());
        return buffer.toString();
    }

    /**
     * Specifies a predefined response that should be returned by the server for
     * requests sent to the specified path.
     */
    public void expect(String forPath, URLResponse response) {
        expected.put(forPath, response);
    }
    
    public void expect(String forPath, HttpStatus status, String contentType, String body) {
        URLResponse response = new URLResponse(status, body, CHARSET);
        response.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
        expected.put(forPath, response);
    }
    
    public void expect(String forPath, HttpStatus status, String body) {
        URLResponse response = new URLResponse(status, body, CHARSET);
        expected.put(forPath, response);
    }
    
    public void expect(String forPath, String body) {
        expect(forPath, HttpStatus.OK, body);
    }
    
    public void expect(String forPath, String contentType, String body) {
        expect(forPath, HttpStatus.OK, contentType, body);
    }
    
    public void clearExpected() {
        expected.clear();
    }
}
