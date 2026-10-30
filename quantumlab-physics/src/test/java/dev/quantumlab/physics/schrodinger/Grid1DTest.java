package dev.quantumlab.physics.schrodinger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Grid1DTest {

    @Test
    @DisplayName("three unknowns on [0, 1] sit at 1/4, 1/2 and 3/4 with spacing 1/4")
    void positions() {
        Grid1D g = new Grid1D(0.0, 1.0, 3);
        assertEquals(0.25, g.spacing(), 1e-15);
        assertEquals(1.0, g.length(), 1e-15);
        assertEquals(0.25, g.x(0), 1e-15);
        assertEquals(0.5, g.x(1), 1e-15);
        assertEquals(0.75, g.x(2), 1e-15);
        assertEquals(3, g.points().length);
    }

    @Test
    @DisplayName("a grid on a symmetric interval is symmetric")
    void symmetric() {
        Grid1D g = new Grid1D(-5.0, 5.0, 100);
        for (int i = 0; i < 100; i++) {
            assertEquals(-g.x(99 - i), g.x(i), 1e-12);
        }
    }

    @Test
    @DisplayName("the walls are one spacing beyond the first and last unknown")
    void wallsAreOneSpacingAway() {
        Grid1D g = new Grid1D(2.0, 4.0, 9);
        assertEquals(2.0, g.x(0) - g.spacing(), 1e-12);
        assertEquals(4.0, g.x(8) + g.spacing(), 1e-12);
    }

    @Test
    @DisplayName("invalid grids and indices are rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(1.0, 1.0, 10));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(2.0, 1.0, 10));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(Double.NaN, 1.0, 10));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(0.0, Double.POSITIVE_INFINITY, 10));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(0.0, 1.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(0.0, 1.0, Grid1D.MAX_POINTS + 1));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(-Double.MAX_VALUE, Double.MAX_VALUE, 10));
        Grid1D g = new Grid1D(0.0, 1.0, 3);
        assertThrows(IllegalArgumentException.class, () -> g.x(-1));
        assertThrows(IllegalArgumentException.class, () -> g.x(3));
    }
}
