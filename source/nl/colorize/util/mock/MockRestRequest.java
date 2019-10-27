//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.mock;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import nl.colorize.util.http.Method;
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
    private String body;

    private static final Splitter PATH_SPLITTER = Splitter.on("/").omitEmptyStrings();

    public MockRestRequest(Method method, String path, String body) {
        super(null, method, path);

        this.method = method;
        this.body = body;

        bindPath(PATH_SPLITTER.splitToList(path), Collections.emptyMap());
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public boolean hasBody() {
        return body != null && body.length() > 0;
    }

    @Override
    public void bindPath(List<String> pathComponents, Map<String, String> pathParameters) {
        super.bindPath(pathComponents, pathParameters);
    }
}
