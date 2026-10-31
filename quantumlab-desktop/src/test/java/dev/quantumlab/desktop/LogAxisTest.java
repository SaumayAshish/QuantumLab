package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogAxisTest {

    @Test
    @DisplayName("positions are base-10 logarithms, exact at powers of ten")
    void position() {
        assertEquals(0.0, LogAxis.position(1.0), 0.0);
        assertEquals(1.0, LogAxis.position(10.0), 0.0);
        assertEquals(3.0, LogAxis.position(1000.0), 0.0);
        assertEquals(Math.log10(50.0), LogAxis.position(50.0), 1e-15);
        assertEquals(1.69897000433602, LogAxis.position(50.0), 1e-13);
    }

    @Test
    @DisplayName("positions need a positive, finite value")
    void positionValidation() {
        assertThrows(IllegalArgumentException.class, () -> LogAxis.position(0.0));
        assertThrows(IllegalArgumentException.class, () -> LogAxis.position(-5.0));
        assertThrows(IllegalArgumentException.class, () -> LogAxis.position(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> LogAxis.position(Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("whole positions are labelled with the value they stand for")
    void labels() {
        assertEquals("1", LogAxis.label(0.0));
        assertEquals("10", LogAxis.label(1.0));
        assertEquals("100", LogAxis.label(2.0));
        assertEquals("1000", LogAxis.label(3.0));
        assertEquals("10000", LogAxis.label(4.0));
        assertEquals("1000000", LogAxis.label(6.0));
        assertEquals("1e7", LogAxis.label(7.0));
        assertEquals("1e-1", LogAxis.label(-1.0));
    }

    @Test
    @DisplayName("positions between powers of ten get no label, and rounding noise does not hide one")
    void labelsBetween() {
        assertEquals("", LogAxis.label(0.5));
        assertEquals("", LogAxis.label(2.3));
        assertEquals("100", LogAxis.label(2.0 + 1e-12));
        assertEquals("100", LogAxis.label(2.0 - 1e-12));
        // 2.0005 is 0.0005 above a power of ten: close, but a position of its own, so no label
        assertEquals("", LogAxis.label(2.0005));
        assertEquals("", LogAxis.label(1.9995));
    }

    @Test
    @DisplayName("bounds enclose the data at whole powers of ten")
    void bounds() {
        assertArrayEquals(new double[] {0.0, 4.0}, LogAxis.bounds(1.0, 10_000.0), 0.0);
        assertArrayEquals(new double[] {0.0, 5.0}, LogAxis.bounds(1.0, 10_001.0), 0.0);
        assertArrayEquals(new double[] {1.0, 2.0}, LogAxis.bounds(50.0, 70.0), 0.0);
        assertArrayEquals(new double[] {0.0, 3.0}, LogAxis.bounds(1.0, 500.0), 0.0);
    }

    @Test
    @DisplayName("bounds span at least one decade, even for one point")
    void boundsMinimumDecade() {
        assertArrayEquals(new double[] {0.0, 1.0}, LogAxis.bounds(1.0, 1.0), 0.0);
        assertArrayEquals(new double[] {1.0, 2.0}, LogAxis.bounds(10.0, 10.0), 0.0);
        assertArrayEquals(new double[] {2.0, 3.0}, LogAxis.bounds(100.0, 100.0), 0.0);
    }

    @Test
    @DisplayName("bounds reject a last value below the first")
    void boundsValidation() {
        assertThrows(IllegalArgumentException.class, () -> LogAxis.bounds(100.0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> LogAxis.bounds(0.0, 10.0));
    }
}
