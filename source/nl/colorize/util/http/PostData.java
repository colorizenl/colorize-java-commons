//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.net.URLDecoder;
import java.net.URLEncoder;
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
     *
     * @throws IllegalStateException if no parameter with that name exists.
     */
    public String getRequiredParameter(String name) {
        String value = data.get(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing required post data: " + name);
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

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public String encode(Charset charset) {
        StringBuilder buffer = new StringBuilder();

        for (Map.Entry<String,String> entry : data.entrySet()) {
            if (buffer.length() > 0) {
                buffer.append('&');
            }
            buffer.append(URLEncoder.encode(entry.getKey(), charset));
            if (!entry.getKey().isEmpty()) {
                buffer.append('=');
            }
            buffer.append(URLEncoder.encode(entry.getValue(), charset));
        }

        return buffer.toString();
    }

    /**
     * Creates a new {@code PostData} instance by merging all parameters from
     * this instance with another one.
     *
     * @throws IllegalArgumentException if both instances contain a parameter
     *         with the same name.
     */
    public PostData merge(PostData other) {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.putAll(data);

        for (Map.Entry<String, String> entry : other.data.entrySet()) {
            Preconditions.checkArgument(!merged.containsKey(entry.getKey()),
                "Duplicate entry: " + entry.getKey());
            merged.put(entry.getKey(), entry.getValue());
        }

        return new PostData(merged);
    }

    /**
     * Serializes this {@code PostData} to a JSON map.
     */
    public String toJSON() {
        if (data.isEmpty()) {
            return "{}";
        }

        Gson gson = new Gson();
        return gson.toJson(data);
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
     *
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
            data.put(URLDecoder.decode(paramName, charset), URLDecoder.decode(paramValue, charset));
        }

        return new PostData(data);
    }

    /**
     * Parses {@code PostData} from a JSON map. This can be used in environments
     * that mix traditional form-encoded requests with JSON-based requests.
     */
    public static PostData parseJSON(String json) {
        if (json == null || json.isEmpty()) {
            return empty();
        }

        Gson gson = new Gson();
        TypeToken<Map<String, Object>> jsonMapType = new TypeToken<>() {};
        Map<String, Object> jsonMap = gson.fromJson(json, jsonMapType.getType());
        Map<String, String> data = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : null;
            data.put(entry.getKey(), value);
        }

        return create(data);
    }

    public static PostData empty() {
        return new PostData(Collections.emptyMap());
    }
}
