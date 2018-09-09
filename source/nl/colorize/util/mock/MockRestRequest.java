//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.mock;

import com.google.common.base.Splitter;
import nl.colorize.util.http.Method;
import nl.colorize.util.rest.PostData;
import nl.colorize.util.rest.RestRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of a {@code RestRequest}, suitable for testing, that
 * does not require a complete {@code HttpServletRequest}
 */
public class MockRestRequest extends RestRequest {

    private Method method;
    private Map<String, String> headers;
    private String body;

    private static final Splitter PATH_SPLITTER = Splitter.on("/").omitEmptyStrings();

    public MockRestRequest(Method method, String path, String body) {
        super(null, path, body);

        this.method = method;
        this.headers = new LinkedHashMap<>();
        this.body = body;

        bindPath(PATH_SPLITTER.splitToList(path), Collections.emptyMap());
    }

    @Override
    public Method getMethod() {
        return method;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public void addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public void bindPath(List<String> pathComponents, Map<String, String> pathParameters) {
        super.bindPath(pathComponents, pathParameters);
    }

    @Override
    public void bindPostData(PostData postData) {
        super.bindPostData(postData);
    }
}