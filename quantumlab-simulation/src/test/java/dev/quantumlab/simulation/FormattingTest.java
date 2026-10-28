package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.math.Complex;
import java.util.Locale;
import java.util.OptionalDouble;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormattingTest {

    @Test
    @DisplayName("complex numbers use a true minus sign and fixed decimals")
    void complexFormatting() {
        assertEquals("0.7071 + 0.0000i", Formatting.complex(Complex.of(0.70710678, 0), 4));
        assertEquals("0.50 − 0.25i", Formatting.complex(Complex.of(0.5, -0.25), 2));
        assertEquals("−0.50 + 0.25i", Formatting.complex(Complex.of(-0.5, 0.25), 2));
    }

    @Test
    @DisplayName("values that round to zero never print as negative zero")
    void noNegativeZero() {
        assertEquals("0.00 + 0.00i", Formatting.complex(Complex.of(-1e-9, -1e-9), 2));
        assertEquals("0.0000", Formatting.fixed(-0.0, 4));
        assertEquals("0.0000", Formatting.fixed(-1e-12, 4));
    }

    @Test
    @DisplayName("percentages")
    void percentages() {
        assertEquals("50.00 %", Formatting.percent(0.5));
        assertEquals("0.00 %", Formatting.percent(0.0));
        assertEquals("100.00 %", Formatting.percent(1.0));
        assertEquals("29.90 %", Formatting.percent(0.299));
    }

    @Test
    @DisplayName("relative phase in radians and degrees, or undefined")
    void phase() {
        assertEquals("1.5708 rad (90.0°)", Formatting.phase(OptionalDouble.of(Math.PI / 2)));
        assertEquals("−0.7854 rad (−45.0°)", Formatting.phase(OptionalDouble.of(-Math.PI / 4)));
        assertTrue(Formatting.phase(OptionalDouble.empty()).startsWith("undefined"));
    }

    @Test
    @DisplayName("standard scores carry an explicit sign")
    void sigma() {
        assertEquals("+0.40 σ", Formatting.sigma(0.4));
        assertEquals("−1.23 σ", Formatting.sigma(-1.234));
        assertEquals("+0.00 σ", Formatting.sigma(0.0));
        assertEquals("+0.00 σ", Formatting.sigma(-0.001));
    }

    @Test
    @DisplayName("output does not depend on the default locale")
    void localeIndependent() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY); // would print "0,50" with the default locale
            assertEquals("0.50", Formatting.fixed(0.5, 2));
            assertEquals("50.00 %", Formatting.percent(0.5));
            assertEquals("+0.40 σ", Formatting.sigma(0.4));
        } finally {
            Locale.setDefault(saved);
        }
    }
}
