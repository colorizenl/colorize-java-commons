//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a HTTP response that is returned after sending a HTTP request to
 * a URL.
 * <p>
 * This class is similar to the {@code HttpResponse} class that is part of the
 * new HTTP client that was introduced in Java 11. However, it serves a slightly
 * different purpose, as {@code HttpResponse} can only be used when receiving
 * a response from the HTTP client. In contrast, this class can also be used
 * to build a HTTP response programmatically.
 */
public class URLResponse extends HttpMessage {

    private HttpStatus status;
    private List<Certificate> certificates;

    public URLResponse(HttpStatus status, String body, Charset encoding) {
        this(status, body.getBytes(encoding), encoding);
    }

    public URLResponse(HttpStatus status, byte[] body, Charset encoding) {
        super(encoding);

        this.status = status;
        this.certificates = new ArrayList<>();
        setBody(body);
    }

    public URLResponse(HttpStatus status) {
        super();
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void addCertificate(Certificate certificate) {
        certificates.add(certificate);
    }

    public List<Certificate> getCertificates() {
        return certificates;
    }
}
