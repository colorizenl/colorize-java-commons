//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import nl.colorize.util.Tuple;
import nl.colorize.util.TupleList;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Immutable representation of the HTTP headers sent as part of HTTP request
 * and response messages. Used in conjunction with {@link URLLoader}, which
 * supports/uses a variety of HTTP clients depending on the platform.
 * <p>
 * HTTP headers are sometimes represented using a {@code Map<String, String>},
 * but this does not allow for the following:
 * <ul>
 *   <li>Headers have an explicitly defined order.</li>
 *   <li>The same header can occur multiple times.</li>
 *   <li>Header names are case-insensitive.</li>
 * </ul>
 * <em>Implementation note:</em> The headers are stored using an (immutable)
 * list of tuples. This allows the class to respect the header properties
 * listed above. In practice the performance impact is neglectable, as nearly
 * all requests and responses only contain a small number of headers.
 */
public class Headers {

    private TupleList<String, String> entries;

    private static CharMatcher NAME_MATCHER = CharMatcher.javaLetterOrDigit().or(CharMatcher.anyOf("-"));
    private static CharMatcher VALUE_MATCHER = CharMatcher.anyOf("\r\n").negate();

    public Headers(TupleList<String, String> entries) {
        for (Tuple<String, String> entry : entries) {
            Preconditions.checkArgument(NAME_MATCHER.matchesAllOf(entry.getKey()),
                "Invalid HTTP header name: " + entry.getKey());

            Preconditions.checkArgument(VALUE_MATCHER.matchesAllOf(entry.getValue()),
                "Invalid HTTP header value: " + entry.getValue());
        }

        this.entries = entries.immutable();
    }

    @SafeVarargs
    public Headers(Tuple<String, String>... entries) {
        this(TupleList.of(entries).immutable());
    }

    public Headers() {
        this(TupleList.empty());
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

    public void forEach(BiConsumer<String, String> callback) {
        entries.forEach(callback);
    }

    private boolean match(Tuple<String, String> header, String name) {
        return header.left().equalsIgnoreCase(name);
    }

    /**
     * Returns a new {@link Headers} instance consisting of all existing headers
     * plus the specified new one.
     *
     * @deprecated Use {@link #concat(String, String)} instead. This method has
     *             been renamed to {@code concat} to make it more explicit that
     *             the original instance is not modified.
     */
    @Deprecated
    public Headers append(String name, String value) {
        return concat(name, value);
    }

    /**
     * Returns a new {@link Headers} instance that consists of this list of
     * headers plus the specified additional header concatenated to the end of
     * the list.
     */
    public Headers concat(String name, String value) {
        TupleList<String, String> result = TupleList.create();
        result.addAll(entries);
        result.add(name, value);
        return new Headers(result);
    }

    /**
     * Returns a new {@link Headers} instance that replaces all occurrences of
     * the header with the specified name.
     */
    public Headers replace(String name, String value) {
        List<Tuple<String, String>> filtered = entries.stream()
            .filter(header -> !match(header, name))
            .toList();

        TupleList<String, String> result = TupleList.create();
        result.addAll(filtered);
        result.add(name, value);
        return new Headers(result);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        entries.forEach((name, value) -> buffer.append(name + ": " + value + "\n"));
        return buffer.toString();
    }
}
