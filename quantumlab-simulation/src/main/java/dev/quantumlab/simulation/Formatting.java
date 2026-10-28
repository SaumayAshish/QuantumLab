package dev.quantumlab.simulation;

import dev.quantumlab.math.Complex;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Display formatting for the UI, kept here so it is testable. All formatting uses {@link
 * Locale#ROOT}, so a decimal point is always {@code .} regardless of the user's locale.
 */
public final class Formatting {

    private Formatting() {}

    /** {@code a + bi} / {@code a − bi} with a fixed number of decimals, using a true minus sign. */
    public static String complex(Complex z, int decimals) {
        String re = fixed(z.re(), decimals);
        String im = fixed(Math.abs(z.im()), decimals);
        // Avoid printing "-0.0000" for tiny negative values that round to zero.
        boolean imNegative = z.im() < 0 && !isZeroAt(z.im(), decimals);
        return re + (imNegative ? " − " : " + ") + im + "i";
    }

    public static String fixed(double x, int decimals) {
        if (isZeroAt(x, decimals)) {
            x = 0.0;
        }
        return String.format(Locale.ROOT, "%." + decimals + "f", x).replace('-', '−');
    }

    /** A probability as a percentage, e.g. {@code 0.5 → "50.00 %"}. */
    public static String percent(double probability) {
        return String.format(Locale.ROOT, "%.2f %%", probability * 100.0);
    }

    /** A relative phase in radians and degrees, or "undefined" when an amplitude is zero. */
    public static String phase(OptionalDouble radians) {
        if (radians.isEmpty()) {
            return "undefined (an amplitude is zero)";
        }
        double r = radians.getAsDouble();
        return fixed(r, 4) + " rad (" + fixed(Math.toDegrees(r), 1) + "°)";
    }

    /** A signed standard-score such as {@code "+0.42 σ"}. */
    public static String sigma(double z) {
        double v = Math.abs(z) < 0.005 ? 0.0 : z;
        return String.format(Locale.ROOT, "%+.2f σ", v).replace('-', '−');
    }

    private static boolean isZeroAt(double x, int decimals) {
        return Math.abs(x) < 0.5 * Math.pow(10.0, -decimals);
    }
}
