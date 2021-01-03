//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.rest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import nl.colorize.util.http.Method;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of a {@code RestRequest}, suitable for testing, that
 * does not require a complete {@code HttpServletRequest}
 */
@VisibleForTesting
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
