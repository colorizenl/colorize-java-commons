//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import nl.colorize.util.stats.Tuple;
import nl.colorize.util.stats.TupleList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Immutable representation of the request/response headers sent as part of an
 * HTTP request. Used in conjunction with {@link URLLoader}, which supports a
 * variety of HTTP clients depending on the platform.
 * <p>
 * HTTP headers are sometimes represented using a {@code Map<String, String>},
 * but this does not allow for the following:
 *
 * <ul>
 *   <li>Headers have an explicitly defined order.</li>
 *   <li>The same header can occur multiple times.</li>
 *   <li>Header names are case-insensitive.</li>
 * </ul>
 */
public class Headers implements Iterable<Tuple<String, String>> {

    private TupleList<String, String> entries;

    private static final CharMatcher NAME_MATCHER = CharMatcher.javaLetterOrDigit()
        .or(CharMatcher.anyOf("-_."));

    private static final CharMatcher VALUE_MATCHER = CharMatcher
        .anyOf("\r\n")
        .negate();

    /**
     * Creates a new {@link Headers} instance from the specified list of
     * HTTP header names/values. This class also defines a number of static
     * factory methods for creating a {@link Headers} instance from other
     * sources.
     *
     * @throws IllegalArgumentException if any of the header names and/or
     *         values are invalid. {@link #validateHeader(String, String)}
     *         is used to perform this validation.
     */
    public Headers(TupleList<String, String> entries) {
        for (Tuple<String, String> entry : entries) {
            validateHeader(entry.left(), entry.right());
        }

        this.entries = entries.immutable();
    }

    /**
     * Iterates over header names. Note that header names can appear multiple
     * times in the list, if there are multiple occurrences of that header.
     */
    public List<String> getHeaderNames() {
        return entries.stream()
            .map(Tuple::left)
            .toList();
    }

    /**
     * Returns the value of the header with the specified name. If the header
     * occurs multiple times, the first occurrence is returned.
     */
    public Optional<String> get(String name) {
        return entries.stream()
            .filter(header -> match(header, name))
            .map(Tuple::right)
            .findFirst();
    }

    /**
     * Returns all values for headers with the specified name. If the header is
     * not present, this will return an empty list.
     */
    public List<String> getAll(String name) {
        return entries.stream()
            .filter(header -> match(header, name))
            .map(Tuple::right)
            .toList();
    }

    @Override
    public Iterator<Tuple<String, String>> iterator() {
        return entries.iterator();
    }

    public void forEach(BiConsumer<String, String> callback) {
        entries.forEach(callback);
    }

    private boolean match(Tuple<String, String> header, String name) {
        return header.left().equalsIgnoreCase(name);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (Tuple<String, String> header : entries) {
            buffer.append(header.left());
            buffer.append(": ");
            buffer.append(header.right());
            buffer.append("\n");
        }
        return buffer.toString();
    }

    /**
     * Creates a {@link Headers} instance from the specified HTTP header
     * names and values. The {@code rest} parameter expects values in the
     * order {@code name1, value1, name2, value2, ...}.
     *
     * @throws IllegalArgumentException When an odd number of arguments
     *         is passed to the method.
     */
    public static Headers of(String name, String value, String... rest) {
        Preconditions.checkArgument(rest.length % 2 == 0,
            "Cannot create HTTP headers from incomplete name/value pairs");

        TupleList<String, String> headers = new TupleList<>();
        headers.add(name, value);
        for (int i = 0; i < rest.length; i += 2) {
            headers.add(rest[i], rest[i + 1]);
        }
        return new Headers(headers);
    }

    /**
     * Creates a {@link Headers} instance from a map containing the name/value
     * pairs of the HTTP headers. The order of the headers is based on the
     * map's iteration order.
     */
    public static Headers fromMap(Map<String, String> entries) {
        return new Headers(TupleList.fromMap(entries));
    }

    /**
     * Returns an empty {@link Headers} instance that indicates the HTTP
     * request or response does not contain any headers.
     */
    public static Headers none() {
        return new Headers(TupleList.empty());
    }

    /**
     * Helper method that validates if the specified name and value should
     * be allowed as a HTTP headers.
     *
     * @throws IllegalArgumentException if the validation fails for the
     *         header name and/or value.
     */
    public static void validateHeader(String name, String value) {
        Preconditions.checkArgument(NAME_MATCHER.matchesAllOf(name),
            "Invalid HTTP header name: " + name);

        Preconditions.checkArgument(VALUE_MATCHER.matchesAllOf(value),
            "Invalid HTTP header value: " + value);
    }
}
