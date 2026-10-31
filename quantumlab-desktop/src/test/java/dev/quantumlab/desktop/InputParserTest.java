package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.simulation.MeasurementSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InputParserTest {

    @Test
    @DisplayName("plain decimals, signs, exponents, whitespace and a true minus sign are accepted")
    void numberAccepts() {
        assertEquals(0.5, InputParser.number("0.5", "x"), 0.0);
        assertEquals(-0.5, InputParser.number("-0.5", "x"), 0.0);
        assertEquals(-0.5, InputParser.number("−0.5", "x"), 0.0); // U+2212
        assertEquals(0.5, InputParser.number("+.5", "x"), 0.0);
        assertEquals(3.0, InputParser.number("3.", "x"), 0.0);
        assertEquals(1.0e-4, InputParser.number("  1E-4 ", "x"), 0.0);
        assertEquals(0.7071067811865476, InputParser.number("0.7071067811865476", "x"), 0.0);
    }

    @Test
    @DisplayName("anything that is not a plain finite decimal is rejected")
    void numberRejects() {
        String[] bad = {
            "", "   ", "abc", "NaN", "Infinity", "-Infinity", "0x1p3", "1d", "1f", "1,5", "1 2", "--1", "1e", "."
        };
        for (String s : bad) {
            assertThrows(InvalidInputException.class, () -> InputParser.number(s, "x"), "should reject \"" + s + "\"");
        }
        assertThrows(InvalidInputException.class, () -> InputParser.number(null, "x"));
    }

    @Test
    @DisplayName("a number too large for a double is rejected rather than becoming infinity")
    void numberOverflow() {
        assertThrows(InvalidInputException.class, () -> InputParser.number("1e999", "x"));
    }

    @Test
    @DisplayName("error messages name the field that is wrong")
    void messageNamesField() {
        InvalidInputException e =
            assertThrows(InvalidInputException.class, () -> InputParser.number("oops", "β (real part)"));
        assertTrue(e.getMessage().contains("β (real part)"));
        assertTrue(e.getMessage().contains("oops"));
    }

    @Test
    @DisplayName("the not-a-number hint suits whole-number fields too, not only decimals")
    void notANumberHintSuitsWholeNumberFields() {
        InvalidInputException e =
            assertThrows(InvalidInputException.class, () -> InputParser.number("abc", "Grid points"));
        String message = e.getMessage();
        assertTrue(message.contains("Grid points"), message);
        assertTrue(message.contains("abc"), message);
        assertTrue(message.contains("e.g. 12 or 0.5"), message);
        assertFalse(message.contains("use a decimal point"), message);
    }

    @Test
    @DisplayName("seeds accept any 64-bit integer, including negative and extreme ones")
    void seedAccepts() {
        assertEquals(0L, InputParser.seed("0"));
        assertEquals(12345L, InputParser.seed(" 12345 "));
        assertEquals(-7L, InputParser.seed("-7"));
        assertEquals(-7L, InputParser.seed("−7"));
        assertEquals(42L, InputParser.seed("+42"));
        assertEquals(Long.MAX_VALUE, InputParser.seed("9223372036854775807"));
        assertEquals(Long.MIN_VALUE, InputParser.seed("-9223372036854775808"));
    }

    @Test
    @DisplayName("seeds reject blanks, decimals, text and values beyond 64 bits")
    void seedRejects() {
        String[] bad = {"", " ", "1.5", "abc", "1e3", "9223372036854775808", "-9223372036854775809"};
        for (String s : bad) {
            assertThrows(InvalidInputException.class, () -> InputParser.seed(s), "should reject \"" + s + "\"");
        }
        assertThrows(InvalidInputException.class, () -> InputParser.seed(null));
    }

    @Test
    @DisplayName("trial counts run from 1 to the session limit")
    void trialCounts() {
        assertEquals(1, InputParser.trialCount("1"));
        assertEquals(1000, InputParser.trialCount(" 1000 "));
        assertEquals(MeasurementSession.MAX_TRIALS, InputParser.trialCount(Integer.toString(MeasurementSession.MAX_TRIALS)));

        String[] bad = {
            "", "0", "-5", "1.5", "ten", "1e3",
            Integer.toString(MeasurementSession.MAX_TRIALS + 1), "99999999999999999999"
        };
        for (String s : bad) {
            assertThrows(InvalidInputException.class, () -> InputParser.trialCount(s), "should reject \"" + s + "\"");
        }
        assertThrows(InvalidInputException.class, () -> InputParser.trialCount(null));
    }
}
