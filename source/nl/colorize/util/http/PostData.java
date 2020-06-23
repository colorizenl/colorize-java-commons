//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import nl.colorize.util.Escape;
import nl.colorize.util.rest.BadRequestException;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents data that has been sent as part of a HTTP request, encoded using
 * the {@code application/x-www-form-urlencoded} content type.
 * <p>
 * Post data parameters consist of name/value pairs. This class provides two
 * ways to retrieve parameter values: {@link #getRequiredParameter(String)}
 * (which will throw an exception if the parameter is not present), and
 * {@link #getOptionalParameter(String, String)} (which will return a default
 * value for missing parameters). Note that the parameter is considered "missing"
 * if it is either not present at all, or if the parameter name is specified but
 * its value is empty.
 */
public class PostData {

    private Map<String, String> data;

    private static final Splitter POST_DATA_SPLITTER = Splitter.on("&");
    private static final PostData EMPTY = new PostData(Collections.emptyMap());

    private PostData(Map<String, String> data) {
        this.data = ImmutableMap.copyOf(data);
    }

    /**
     * Returns the value of the parameter with the specified name.
     * @throws BadRequestException if no parameter with that name exists.
     */
    public String getRequiredParameter(String name) {
        String value = data.get(name);
        if (value == null || value.isEmpty()) {
            throw new BadRequestException("Missing required post data: " + name);
        }
        return value;
    }

    /**
     * Returns either the value of the parameter with the specified
     * name, or {@code defaultValue} if no parameter with that name exists.
     */
    public String getOptionalParameter(String name, String defaultValue) {
        String value = data.get(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String encode(Charset charset) {
        StringBuilder buffer = new StringBuilder();

        for (Map.Entry<String,String> entry : data.entrySet()) {
            if (buffer.length() > 0) {
                buffer.append('&');
            }
            buffer.append(Escape.urlEncode(entry.getKey(), charset));
            if (!entry.getKey().isEmpty()) {
                buffer.append('=');
            }
            buffer.append(Escape.urlEncode(entry.getValue(), charset));
        }

        return buffer.toString();
    }

    @Override
    public String toString() {
        return encode(Charsets.UTF_8);
    }

    /**
     * Creates a {@code PostData} instance from existing key/value pairs.
     */
    public static PostData create(Map<String, String> data) {
        Map<String, String> processedData = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                value = "";
            }

            processedData.put(entry.getKey(), value);
        }

        return new PostData(processedData);
    }

    /**
     * Creates a {@code PostData} instance from a number of key/value pairs.
     * @throws IllegalArgumentException if the number of parameters is not a
     *         multiple of two.
     */
    public static PostData create(String key, String value, String... rest) {
        Preconditions.checkArgument(rest.length % 2 == 0,
            "Invalid key/value pairs: " + Arrays.toString(rest));

        Map<String, String> data = new LinkedHashMap<>();
        data.put(key, value);
        for (int i = 0; i < rest.length; i += 2) {
            data.put(rest[i], rest[i + 1]);
        }

        return new PostData(data);
    }

    /**
     * Parses {@code PostData} from the specified string. If the data is empty
     * this will also lead to an empty {@code PostData} instance being returned.
     */
    public static PostData parse(String encoded, Charset charset) {
        if (encoded.startsWith("?")) {
            encoded = encoded.substring(1);
        }

        if (encoded.isEmpty() || encoded.indexOf('=') == -1) {
            return EMPTY;
        }

        Map<String, String> data = new LinkedHashMap<>();
        for (String param : POST_DATA_SPLITTER.split(encoded)) {
            String paramName = param.substring(0, param.indexOf('='));
            String paramValue = param.substring(param.indexOf('=') + 1);
            data.put(Escape.urlDecode(paramName, charset), Escape.urlDecode(paramValue, charset));
        }

        return new PostData(data);
    }

    public static PostData empty() {
        return new PostData(Collections.emptyMap());
    }
}
