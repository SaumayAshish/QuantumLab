package dev.quantumlab.desktop;

import dev.quantumlab.simulation.MeasurementSession;
import java.util.regex.Pattern;

/**
 * Turns text from input fields into numbers, with messages a user can act on. Only plain decimal
 * notation is accepted: no {@code NaN}, no {@code Infinity}, no hexadecimal floats, no type
 * suffixes such as {@code 1d}, and no decimal comma (which would be ambiguous with a thousands
 * separator).
 */
final class InputParser {

    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");

    private InputParser() {}

    /** Parses a finite decimal number; a true minus sign (−) is accepted as well as a hyphen. */
    static double number(String text, String field) {
        String t = clean(text);
        if (t.isEmpty()) {
            throw new InvalidInputException(field + " is empty.");
        }
        if (!DECIMAL.matcher(t).matches()) {
            // The hint must suit whole-number fields too (grid points, levels), which are read
            // with this method and then checked for a whole value.
            throw new InvalidInputException(
                field
                    + " is not a number: \""
                    + t
                    + "\" (digits only, with a decimal point if needed, e.g. 12 or 0.5).");
        }
        double v = Double.parseDouble(t);
        if (!Double.isFinite(v)) {
            throw new InvalidInputException(field + " is too large to represent.");
        }
        return v;
    }

    /** Parses a generator seed: any whole number that fits in 64 bits. */
    static long seed(String text) {
        String t = clean(text);
        if (t.isEmpty()) {
            throw new InvalidInputException("Seed is empty.");
        }
        if (!INTEGER.matcher(t).matches()) {
            throw new InvalidInputException("Seed must be a whole number, got \"" + t + "\".");
        }
        try {
            return Long.parseLong(t.startsWith("+") ? t.substring(1) : t);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Seed does not fit in 64 bits: \"" + t + "\".");
        }
    }

    /** Parses a number of trials in {@code 1 .. MeasurementSession.MAX_TRIALS}. */
    static int trialCount(String text) {
        String t = clean(text);
        if (t.isEmpty()) {
            throw new InvalidInputException("Number of trials is empty.");
        }
        if (!t.matches("\\d+")) {
            throw new InvalidInputException(
                "Number of trials must be a positive whole number, got \"" + t + "\".");
        }
        if (t.length() > 9) { // longer than MAX_TRIALS and could overflow
            throw new InvalidInputException(tooMany());
        }
        long n = Long.parseLong(t);
        if (n < 1) {
            throw new InvalidInputException("Number of trials must be at least 1.");
        }
        if (n > MeasurementSession.MAX_TRIALS) {
            throw new InvalidInputException(tooMany());
        }
        return (int) n;
    }

    private static String tooMany() {
        return "At most " + MeasurementSession.MAX_TRIALS + " trials are allowed per experiment.";
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim().replace('−', '-');
    }
}
