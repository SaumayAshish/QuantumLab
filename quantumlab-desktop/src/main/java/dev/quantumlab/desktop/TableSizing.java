package dev.quantumlab.desktop;

/** How tall a results table should be for a given number of rows. No JavaFX type, so it can be tested. */
final class TableSizing {

    static final double ROW_HEIGHT = 24.0;
    static final double HEADER_HEIGHT = 28.0;
    static final double BORDER = 2.0;
    static final int MIN_ROWS = 2;
    static final int MAX_ROWS = 12;

    private TableSizing() {}

    /** Height for {@code rows} rows, never less than two rows' worth and never more than twelve. */
    static double heightFor(int rows) {
        if (rows < 0) {
            throw new IllegalArgumentException("A table cannot have " + rows + " rows.");
        }
        int shown = Math.min(MAX_ROWS, Math.max(MIN_ROWS, rows));
        return shown * ROW_HEIGHT + HEADER_HEIGHT + BORDER;
    }
}
