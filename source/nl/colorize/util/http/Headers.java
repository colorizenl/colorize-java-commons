//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import nl.colorize.util.Tuple;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Headers that can be added to HTTP requests and responses. Multiple headers
 * with the same name can be added, and header names are not case sensitive.
 */
public class Headers {

    private Multimap<HeaderName, String> entries;

    public Headers() {
        this.entries = ArrayListMultimap.create();
    }

    public Headers(Map<String, String> entries) {
        this();
        add(entries);
    }

    /**
     * Adds the specified header, regardless of whether other headers with the
     * same name already exist.
     */
    public void add(String name, String value) {
        Preconditions.checkArgument(name != null && !name.isEmpty(), "Invalid HTTP header name: " + name);
        Preconditions.checkArgument(value != null, "Invalid HTTP header value: " + value);

        entries.put(new HeaderName(name), value);
    }

    public void add(Map<String, String> entries) {
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds the specified header, replacing any headers with the same name that
     * might already exist.
     */
    public void replace(String name, String value) {
        entries.removeAll(new HeaderName(name));
        add(name, value);
    }

    /**
     * Adds all entries in the specified other set of headers.
     */
    public void merge(Headers other) {
        entries.putAll(other.entries);
    }

    /**
     * Removes all existing headers, then adds all entries in the specified
     * other set of headers.
     */
    public void replace(Headers other) {
        entries.clear();
        entries.putAll(other.entries);
    }

    public void clear() {
        entries.clear();
    }

    /**
     * Returns the value for the header with the specified name. If multiple
     * headers with that name are present, the first one will be returned. If
     * the header is not present this will return {@code null}.
     */
    public String getValue(String name) {
        return getValue(name, null);
    }

    /**
     * Returns the value for the header with the specified name, or the default
     * value if no such header exists. If multiple headers with that name are
     * present, the first one will be returned.
     */
    public String getValue(String name, String defaultValue) {
        Collection<String> values = entries.get(new HeaderName(name));
        if (values.isEmpty()) {
            return defaultValue;
        }
        return values.iterator().next();
    }

    /**
     * Returns all values for headers with the specified name. If the header is
     * not present this will return an empty list.
     */
    public List<String> getValues(String name) {
        Collection<String> values = entries.get(new HeaderName(name));
        return ImmutableList.copyOf(values);
    }

    /**
     * Returns the names of all headers that have been added.
     */
    public Set<String> getNames() {
        Set<String> names = new LinkedHashSet<>();
        for (HeaderName headerName : entries.keySet()) {
            names.add(headerName.header);
        }
        return names;
    }

    /**
     * Returns a list of all header name/value pairs. This may include multiple
     * headers with the same name.
     */
    public List<Tuple<String, String>> getEntries() {
        return entries.entries().stream()
            .map(entry -> Tuple.of(entry.getKey().header, entry.getValue()))
            .collect(Collectors.toList());
    }

    public boolean has(String name) {
        return entries.containsKey(new HeaderName(name));
    }

    @Override
    public String toString() {
        return entries.entries().stream()
            .map(entry -> entry.getKey().header + ": " + entry.getValue())
            .collect(Collectors.joining("\n"));
    }

    /**
     * Used to represent a case-insensitive HTTP header name. The header names
     * do preserve their original case, so {@code Content-Type} will be considered
     * equal to {@code content-type}, but still keeps the uppercase letters in
     * its textual representation.
     */
    private static class HeaderName {

        private String header;
        private int hash;

        public HeaderName(String header) {
            this.header = header;
            this.hash = header.toLowerCase().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof HeaderName) {
                HeaderName other = (HeaderName) o;
                return other.header.equalsIgnoreCase(header);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return header;
        }
    }
}
