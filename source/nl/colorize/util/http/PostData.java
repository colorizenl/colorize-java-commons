//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import nl.colorize.util.stats.Tuple;
import nl.colorize.util.stats.TupleList;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents data that has been sent as part of an HTTP request and is
 * encoded using the {@code application/x-www-form-urlencoded} content type.
 * This is commonly used in URL query strings, and in "classic" HTML forms
 * as documented in sections 4.10.21.7 and 4.10.21.8 of the
 * <a href="https://html.spec.whatwg.org/multipage/">HTML specification</a>.
 * <p>
 * Post data parameters consist of name/value pairs. This class provides two
 * ways to retrieve parameter values: {@link #getRequiredParameter(String)}
 * (which will throw an exception if the parameter is not present), and
 * {@link #getOptionalParameter(String, String)} (which will return a default
 * value for missing parameters). Note that the parameter is considered
 * "missing" if it is either not present at all, or if the parameter name is
 * specified but its value is empty.
 */
public class PostData {

    private TupleList<String, String> params;

    private static final Splitter POST_DATA_SPLITTER = Splitter.on("&");
    private static final PostData EMPTY = new PostData(TupleList.empty());

    private PostData(TupleList<String, String> params) {
        this.params = params.immutable();
    }

    /**
     * Returns the value of the parameter with the specified name. If multiple
     * parameters match the requested name, the first occurrence will be
     * returned. Use {@link #getParameterValues(String)} if you want to
     * retrieve <em>all</em> matching parameters.
     *
     * @throws IllegalStateException if no parameter with that name exists.
     */
    public String getRequiredParameter(String name) {
        String value = findParameterValues(name)
            .findFirst()
            .orElse("");

        if (value.isEmpty()) {
            throw new IllegalStateException("Missing required post data: " + name);
        }

        return value;
    }

    /**
     * Returns either the value of the parameter with the specified
     * name, or {@code defaultValue} if no parameter with that name exists.
     */
    public String getOptionalParameter(String name, String defaultValue) {
        String value = findParameterValues(name)
            .findFirst()
            .orElse("");

        if (value.isEmpty()) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Returns all values for the parameter with the specified name. Returns
     * an empty list if no matching parameters exist. This can also return
     * <em>multiple</em> values, as it is allowed for multiple parameters to
     * have the same name.
     */
    public List<String> getParameterValues(String name) {
        return findParameterValues(name).toList();
    }

    private Stream<String> findParameterValues(String name) {
        return params.stream()
            .filter(param -> param.left().equals(name))
            .map(Tuple::right);
    }

    /**
     * Returns true if this {@link PostData} contains at least one parameter
     * with the specified name.
     */
    public boolean contains(String name) {
        return findParameterValues(name).findAny().isPresent();
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    /**
     * Creates a map that includes all parameters in this {@link PostData}. If
     * multiple parameters share the same name, the map will include the first
     * occurrence.
     *
     * @deprecated Using a map does not allow for an explicit iteration order,
     *             and it also does not allow multiple parameters to use the
     *             same name.
     */
    @Deprecated
    public Map<String, String> toMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Tuple<String, String> param : params) {
            if (!result.containsKey(param.left())) {
                result.put(param.left(), param.right());
            }
        }
        return result;
    }

    /**
     * Creates a new {@code PostData} instance by merging all parameters from
     * this instance with another one.
     *
     * @throws IllegalArgumentException if both instances contain a parameter
     *         with the same name.
     */
    public PostData merge(PostData other) {
        TupleList<String, String> mergedParams = params.concat(other.params);
        return new PostData(mergedParams);
    }

    /**
     * Encodes this {@link PostData} into a string that follows the
     * {@code application/x-www-form-urlencoded} notation. An example of such
     * a string is {@code a=2&b=3}. If this {@link PostData} contains no
     * parameters this will return an empty string.
     */
    public String encode(Charset charset) {
        return params.stream()
            .map(param -> encodeParam(param, charset))
            .collect(Collectors.joining("&"));
    }

    /**
     * Encodes this {@link PostData} into a string that follows the
     * {@code application/x-www-form-urlencoded} notation, using the UTF-8
     * character encoding. An example of such a string is {@code a=2&b=3}.
     * If this {@link PostData} contains no parameters this will return an
     * empty string.
     */
    public String encode() {
        return encode(StandardCharsets.UTF_8);
    }

    private String encodeParam(Tuple<String, String> param, Charset charset) {
        String encodedName = URLEncoder.encode(param.left(), charset);
        String encodedValue = URLEncoder.encode(param.right(), charset);

        if (encodedValue.isEmpty()) {
            return encodedName;
        }

        return encodedName + "=" + encodedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PostData other) {
            return toString().equals(other.toString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return encode(StandardCharsets.UTF_8);
    }

    /**
     * Creates a {@code PostData} instance from a number of key/value pairs.
     * When specifying multiple key/value pairs, the {@code rest} parameter
     * expects the order {@code key1, value1, key2, value2, ...}.
     *
     * @throws IllegalArgumentException if the number of parameters is not a
     *         multiple of two.
     */
    public static PostData create(String key, String value, String... rest) {
        Preconditions.checkArgument(rest.length % 2 == 0,
            "Invalid key/value pairs: " + Arrays.toString(rest));

        TupleList<String, String> params = new TupleList<>();
        params.add(key, value);
        for (int i = 0; i < rest.length; i += 2) {
            params.add(rest[i], rest[i + 1]);
        }

        return new PostData(params);
    }

    /**
     * Creates a {@code PostData} instance from existing key/value pairs. The
     * parameter order will be based on the map's iteration order.
     */
    public static PostData create(Map<String, ?> data) {
        TupleList<String, String> params = new TupleList<>();

        for (Map.Entry<String, ?> entry : data.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            params.add(entry.getKey(), value);
        }

        return new PostData(params);
    }

    /**
     * Parses {@code PostData} from the specified string. The input is expected
     * to follow the {@code application/x-www-form-urlencoded} notation, for
     * example {@code b=2&c=3}. If the encoded input string is empty, this will
     * return {@link #empty()}.
     */
    public static PostData parse(String encoded, Charset charset) {
        if (encoded.startsWith("?")) {
            encoded = encoded.substring(1);
        }

        if (encoded.isEmpty()) {
            return EMPTY;
        }

        TupleList<String, String> params = new TupleList<>();
        for (String param : POST_DATA_SPLITTER.split(encoded)) {
            params.add(parseParam(param, charset));
        }

        return new PostData(params);
    }

    /**
     * Parses {@code PostData} from the specified string using the UTF-8
     * character encoding. The input is expected to follow the
     * {@code application/x-www-form-urlencoded} notation, for example
     * {@code b=2&c=3}. If the encoded input string is empty, this will
     * return {@link #empty()}.
     */
    public static PostData parse(String encoded) {
        return parse(encoded, StandardCharsets.UTF_8);
    }

    private static Tuple<String, String> parseParam(String param, Charset charset) {
        if (!param.contains("=")) {
            return Tuple.of(param, "");
        }

        String rawName = param.substring(0, param.indexOf("="));
        String rawValue = param.substring(rawName.length() + 1);
        return Tuple.of(URLDecoder.decode(rawName, charset), URLDecoder.decode(rawValue, charset));
    }

    /**
     * Utility method that returns a {@link PostData} instance that indicates
     * no parameters have been specified. Encoding this instance will produce
     * an empty string.
     */
    public static PostData empty() {
        return EMPTY;
    }
}
