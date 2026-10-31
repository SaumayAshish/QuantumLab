package dev.quantumlab.desktop;

/**
 * A logarithmic horizontal axis built from an ordinary linear one: points are placed at
 * {@code log10(x)} and the tick at position {@code k} is labelled with {@code 10^k}. This is plain
 * arithmetic with no JavaFX type, so it can be tested.
 */
final class LogAxis {

    private LogAxis() {}

    /** Where the value {@code x} sits on the axis: {@code log10(x)}. */
    static double position(double x) {
        if (!(x > 0.0) || Double.isInfinite(x)) {
            throw new IllegalArgumentException("A logarithmic axis needs a positive, finite value, got " + x);
        }
        return Math.log10(x);
    }

    /**
     * The label for the tick at axis position {@code exponent}: the value {@code 10^exponent} when
     * the position is a whole number, and nothing in between.
     */
    static String label(double exponent) {
        double k = Math.rint(exponent);
        if (Math.abs(exponent - k) > 1e-9) {
            return "";
        }
        if (k >= 0.0 && k <= 6.0) {
            return Long.toString(Math.round(Math.pow(10.0, k)));
        }
        return "1e" + Math.round(k);
    }

    /**
     * Axis limits {@code {lower, upper}} in axis positions that enclose {@code first} to {@code last}
     * at whole powers of ten, spanning at least one decade.
     */
    static double[] bounds(double first, double last) {
        if (last < first) {
            throw new IllegalArgumentException("The last value " + last + " is below the first " + first);
        }
        double lower = Math.floor(position(first));
        double upper = Math.max(Math.ceil(position(last)), lower + 1.0);
        return new double[] {lower, upper};
    }
}
