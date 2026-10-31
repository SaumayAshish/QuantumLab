package dev.quantumlab.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TableSizingTest {

    @Test
    @DisplayName("each row adds one row height to the header and border")
    void growsWithRows() {
        assertEquals(2 * 24.0 + 28.0 + 2.0, TableSizing.heightFor(2), 0.0);
        assertEquals(3 * 24.0 + 28.0 + 2.0, TableSizing.heightFor(3), 0.0);
        assertEquals(4 * 24.0 + 28.0 + 2.0, TableSizing.heightFor(4), 0.0);
        assertEquals(24.0, TableSizing.heightFor(5) - TableSizing.heightFor(4), 0.0);
    }

    @Test
    @DisplayName("an empty or one-row table is still two rows tall")
    void minimum() {
        double two = TableSizing.heightFor(2);
        assertEquals(two, TableSizing.heightFor(0), 0.0);
        assertEquals(two, TableSizing.heightFor(1), 0.0);
        assertEquals(78.0, two, 0.0);
    }

    @Test
    @DisplayName("a long table stops growing at twelve rows")
    void maximum() {
        double twelve = TableSizing.heightFor(12);
        assertEquals(12 * 24.0 + 30.0, twelve, 0.0);
        assertTrue(TableSizing.heightFor(11) < twelve);
        assertEquals(twelve, TableSizing.heightFor(13), 0.0);
        assertEquals(twelve, TableSizing.heightFor(100_000), 0.0);
    }

    @Test
    @DisplayName("a negative row count is rejected")
    void negative() {
        assertThrows(IllegalArgumentException.class, () -> TableSizing.heightFor(-1));
    }
}
