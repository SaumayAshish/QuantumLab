package dev.quantumlab.service;

import java.util.regex.Pattern;

/** Strict number parsing: plain decimals and plain whole numbers, and nothing else. */
final class Numbers {

    private static final Pattern PLAIN = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");
    private static final Pattern WHOLE = Pattern.compile("[+-]?\\d+");

    private Numbers() {}

    static double parse(String text, String field) {
        if (text == null) {
            throw new BadRequestException(field + " is required");
        }
        String trimmed = text.trim();
        if (!PLAIN.matcher(trimmed).matches()) {
            throw new BadRequestException(
                field + " must be a plain decimal number, got '" + text + "'");
        }
        double value = Double.parseDouble(trimmed);
        if (!Double.isFinite(value)) {
            throw new BadRequestException(field + " is too large, got '" + text + "'");
        }
        return value;
    }

    static long parseLong(String text, String field) {
        if (text == null) {
            throw new BadRequestException(field + " is required");
        }
        String trimmed = text.trim();
        if (!WHOLE.matcher(trimmed).matches()) {
            throw new BadRequestException(field + " must be a whole number, got '" + text + "'");
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            throw new BadRequestException(field + " is too large, got '" + text + "'");
        }
    }
}
