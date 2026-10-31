package dev.quantumlab.desktop;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.control.TableView;

/** Layout helpers shared by the screens' results tables. Must be used on the JavaFX thread only. */
final class Tables {

    private Tables() {}

    /**
     * Makes {@code table} exactly as tall as its rows (see {@link TableSizing}), so that it shows no
     * empty rows under the data and is never squeezed below the height of its data. The height
     * follows the table's item list, so call this once and keep filling it with {@code
     * getItems().setAll(...)}. Do not also call {@code setPrefHeight} or {@code setMinHeight} on it:
     * a bound property cannot be set.
     */
    static void fitToRows(TableView<?> table) {
        table.setFixedCellSize(TableSizing.ROW_HEIGHT);
        DoubleBinding height =
            Bindings.createDoubleBinding(
                () -> TableSizing.heightFor(table.getItems().size()), table.getItems());
        table.prefHeightProperty().bind(height);
        table.minHeightProperty().bind(height);
    }
}
