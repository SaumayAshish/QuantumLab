package dev.quantumlab.simulation;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A minimal pretty-printing JSON writer. QuantumLab only ever <em>emits</em> JSON, so a full
 * library would be an unnecessary dependency (ADR-0004).
 *
 * <p>Non-finite doubles are written as {@code null}, because JSON has no representation for NaN or
 * infinity. Strings are escaped per RFC 8259; non-ASCII characters are written as-is (the output is
 * UTF-8).
 */
final class JsonWriter {

    private final StringBuilder out = new StringBuilder();
    /** One entry per open container: whether it already holds at least one member. */
    private final Deque<Boolean> hasMembers = new ArrayDeque<>();

    private boolean afterKey;

    JsonWriter startObject() {
        separator();
        out.append('{');
        hasMembers.push(false);
        return this;
    }

    JsonWriter endObject() {
        closeContainer('}');
        return this;
    }

    JsonWriter startArray() {
        separator();
        out.append('[');
        hasMembers.push(false);
        return this;
    }

    JsonWriter endArray() {
        closeContainer(']');
        return this;
    }

    JsonWriter key(String name) {
        separator();
        out.append(quote(name)).append(": ");
        afterKey = true;
        return this;
    }

    JsonWriter value(String s) {
        separator();
        out.append(s == null ? "null" : quote(s));
        return this;
    }

    JsonWriter value(double d) {
        separator();
        out.append(Double.isFinite(d) ? Double.toString(d) : "null");
        return this;
    }

    JsonWriter value(long n) {
        separator();
        out.append(n);
        return this;
    }

    JsonWriter value(boolean b) {
        separator();
        out.append(b);
        return this;
    }

    JsonWriter nullValue() {
        separator();
        out.append("null");
        return this;
    }

    @Override
    public String toString() {
        if (!hasMembers.isEmpty()) {
            throw new IllegalStateException("Unclosed JSON container");
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- internals

    /** Emits the comma, newline and indentation that must precede the next value or key. */
    private void separator() {
        if (afterKey) {
            afterKey = false; // a value directly following its key needs no separator
            return;
        }
        if (hasMembers.isEmpty()) {
            return; // the root value
        }
        if (hasMembers.pop()) {
            out.append(',');
        }
        hasMembers.push(true);
        newline();
    }

    private void closeContainer(char close) {
        if (hasMembers.isEmpty()) {
            throw new IllegalStateException("No open JSON container");
        }
        boolean hadMembers = hasMembers.pop();
        if (hadMembers) {
            newline();
        }
        out.append(close);
    }

    private void newline() {
        out.append('\n');
        for (int i = 0; i < hasMembers.size(); i++) {
            out.append("  ");
        }
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
