package dev.quantumlab.simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small strict JSON parser used only by tests, to check that exports are well-formed and to read
 * values back. Objects become {@code Map<String,Object>}, arrays {@code List<Object>}, numbers
 * {@code Double}, plus {@code String}, {@code Boolean} and {@code null}.
 */
final class MiniJson {

    private final String s;
    private int pos;

    private MiniJson(String s) {
        this.s = s;
    }

    static Object parse(String text) {
        MiniJson p = new MiniJson(text);
        p.skipWhitespace();
        Object v = p.value();
        p.skipWhitespace();
        if (p.pos != text.length()) {
            throw p.error("trailing content");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> array(Object o) {
        return (List<Object>) o;
    }

    static double number(Object o) {
        return (Double) o;
    }

    // ---------------------------------------------------------------- grammar

    private Object value() {
        if (pos >= s.length()) {
            throw error("unexpected end");
        }
        char c = s.charAt(pos);
        return switch (c) {
            case '{' -> objectValue();
            case '[' -> arrayValue();
            case '"' -> stringValue();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> numberValue();
        };
    }

    private Map<String, Object> objectValue() {
        Map<String, Object> m = new LinkedHashMap<>();
        pos++; // {
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return m;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("expected string key");
            }
            String key = stringValue();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            if (m.containsKey(key)) {
                throw error("duplicate key " + key);
            }
            m.put(key, value());
            skipWhitespace();
            if (peek() == ',') {
                pos++;
            } else {
                expect('}');
                return m;
            }
        }
    }

    private List<Object> arrayValue() {
        List<Object> list = new ArrayList<>();
        pos++; // [
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(value());
            skipWhitespace();
            if (peek() == ',') {
                pos++;
            } else {
                expect(']');
                return list;
            }
        }
    }

    private String stringValue() {
        StringBuilder sb = new StringBuilder();
        pos++; // opening quote
        while (true) {
            if (pos >= s.length()) {
                throw error("unterminated string");
            }
            char c = s.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c < 0x20) {
                throw error("unescaped control character");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = s.charAt(pos++);
            switch (e) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw error("bad escape \\" + e);
            }
        }
    }

    private Double numberValue() {
        int start = pos;
        while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw error("unexpected character '" + s.charAt(pos) + "'");
        }
        return Double.valueOf(s.substring(start, pos));
    }

    private Object literal(String word, Object result) {
        if (!s.startsWith(word, pos)) {
            throw error("expected " + word);
        }
        pos += word.length();
        return result;
    }

    // ---------------------------------------------------------------- helpers

    private char peek() {
        if (pos >= s.length()) {
            throw error("unexpected end");
        }
        return s.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw error("expected '" + c + "' but found '" + peek() + "'");
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("JSON error at " + pos + ": " + msg);
    }
}
