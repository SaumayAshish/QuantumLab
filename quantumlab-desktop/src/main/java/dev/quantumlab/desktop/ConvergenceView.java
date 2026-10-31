package dev.quantumlab.desktop;

import static dev.quantumlab.desktop.Tables.column;

import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * The convergence screen: refine the grid step by step and watch the energy error shrink, on a
 * chart of log10 |error| against log10 h where a second-order scheme is a line of slope 2.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * ConvergencePresenter}.
 */
public final class ConvergenceView {

    private final ConvergencePresenter presenter = new ConvergencePresenter();
    private final Worker worker = new Worker("quantumlab-convergence-worker");

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final ComboBox<SchrodingerPresenter.PotentialKind> potential = new ComboBox<>();
    private final TextField xMin = new TextField();
    private final TextField xMax = new TextField();
    private final TextField omega = new TextField();
    private final TextField level = new TextField();
    private final TextField firstGrid = new TextField();
    private final TextField steps = new TextField();

    // outputs
    private final Label titleOut = new Label("–");
    private final Label referenceOut = new Label("–");
    private final Label summaryOut = new Label();
    private final Label noteOut = new Label();
    private final TableView<ConvergencePresenter.RowView> table = new TableView<>();
    private final Label statusOut = new Label();
    private final LineChart<Number, Number> chart;

    public ConvergenceView() {
        NumberAxis x = new NumberAxis();
        x.setLabel("log10 h");
        x.setForceZeroInRange(false);
        NumberAxis y = new NumberAxis();
        y.setLabel("log10 |error|");
        y.setForceZeroInRange(false);
        chart = new LineChart<>(x, y);
        chart.setTitle("Error against grid spacing (log–log)");
        chart.setAnimated(false);
        chart.setPrefHeight(320);
        chart.setMinHeight(240);
        chart.setStyle("CHART_COLOR_1: #1f6fb4; CHART_COLOR_2: #e8590c;");

        potential.getItems().setAll(SchrodingerPresenter.PotentialKind.values());
        potential.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(SchrodingerPresenter.PotentialKind kind) {
                    if (kind == null) {
                        return "";
                    }
                    return kind == SchrodingerPresenter.PotentialKind.INFINITE_WELL
                        ? "Infinite square well"
                        : "Harmonic oscillator";
                }

                @Override
                public SchrodingerPresenter.PotentialKind fromString(String text) {
                    return null; // the box is not editable
                }
            });
        potential.valueProperty().addListener((obs, old, kind) -> applyDefaults(kind));
        potential.setValue(SchrodingerPresenter.PotentialKind.INFINITE_WELL);

        root.setLeft(buildInputPane());
        root.setCenter(buildResultsPane());
        root.setBottom(buildFooter());

        onRun(); // never show an empty screen
    }

    public Parent root() {
        return root;
    }

    /** Stops the worker thread. Call when the window closes. */
    public void shutdown() {
        worker.shutdown();
    }

    // ---------------------------------------------------------------- layout

    private Node buildInputPane() {
        for (TextField f : new TextField[] {xMin, xMax, omega, level, firstGrid, steps}) {
            f.setPrefColumnCount(10);
        }
        potential.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "Potential", potential);
        addRow(form, 1, "Left wall", xMin);
        addRow(form, 2, "Right wall", xMax);
        addRow(form, 3, "Frequency ω", omega);
        addRow(form, 4, "Level n", level);
        addRow(form, 5, "First grid", firstGrid);
        addRow(form, 6, "Refinements", steps);

        GridPane last = new GridPane();
        last.setHgap(8);
        last.setVgap(4);
        last.getColumnConstraints().add(new ColumnConstraints(85));
        titleOut.setWrapText(true);
        titleOut.setMaxWidth(260);
        addRow(last, 0, "Studied", titleOut);
        addRow(last, 1, "Exact E", referenceOut);

        VBox box =
            new VBox(
                10,
                heading("Study"),
                form,
                button("Run study", this::onRun),
                heading("Last run"),
                last);
        box.setPadding(new Insets(16));
        box.setPrefWidth(360);
        return box;
    }

    private Node buildResultsPane() {
        summaryOut.setWrapText(true);
        noteOut.setWrapText(true);

        table.getColumns().add(column("Grid points", 90, ConvergencePresenter.RowView::points));
        table.getColumns().add(column("Spacing h", 100, ConvergencePresenter.RowView::spacing));
        table.getColumns().add(column("Numerical E", 120, ConvergencePresenter.RowView::numerical));
        table.getColumns().add(column("Error", 110, ConvergencePresenter.RowView::error));
        table.getColumns().add(column("Observed order", 120, ConvergencePresenter.RowView::order));
        Tables.fitToRows(table);
        table.setPlaceholder(new Label("No data"));

        VBox box =
            new VBox(
                10,
                heading("Grid refinement (each row halves h)"),
                table,
                summaryOut,
                noteOut,
                chart,
                new ExplanationPane(ExplanationText.forSchrodingerScreen()).node());
        box.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private Node buildFooter() {
        Label scope = new Label(ScopeNote.forMeasurementScreen());
        scope.setWrapText(true);
        VBox box = new VBox(4, statusOut, scope);
        box.setPadding(new Insets(8, 16, 12, 16));
        return box;
    }

    // ---------------------------------------------------------------- actions

    private void onRun() {
        ConvergencePresenter.Request request =
            new ConvergencePresenter.Request(
                potential.getValue(),
                xMin.getText(),
                xMax.getText(),
                omega.getText(),
                level.getText(),
                firstGrid.getText(),
                steps.getText());
        run(
            () -> {
                ConvergencePresenter.Result result = presenter.study(request);
                return () -> {
                    show(result);
                    setStatus("Solved " + result.rows().size() + " grids.", false);
                };
            });
    }

    /**
     * Runs {@code work} on the worker thread. It returns the UI update to apply, which then runs on
     * the JavaFX thread. Controls are disabled while the work runs.
     */
    private void run(Supplier<Runnable> work) {
        worker.run(work, message -> setStatus(message, true), this::setBusy);
    }

    // ---------------------------------------------------------------- display

    /** Typical settings for each potential; the user can change them afterwards. */
    private void applyDefaults(SchrodingerPresenter.PotentialKind kind) {
        boolean well = kind == SchrodingerPresenter.PotentialKind.INFINITE_WELL;
        xMin.setText(well ? "0" : "-10");
        xMax.setText(well ? "1" : "10");
        level.setText(well ? "1" : "0");
        firstGrid.setText(well ? "15" : "99");
        steps.setText(well ? "6" : "4");
        omega.setText("1");
        omega.setDisable(well);
    }

    private void show(ConvergencePresenter.Result r) {
        titleOut.setText(r.title());
        referenceOut.setText(r.reference());
        summaryOut.setText(r.summary());
        noteOut.setText(r.note());
        table.getItems().setAll(r.rows());

        chart.getData().clear();
        chart.getData().add(series("Error", r.errors()));
        if (!r.guide().isEmpty()) {
            chart.getData().add(series("Slope 2 (h² law)", r.guide()));
        }
    }

    private void setStatus(String message, boolean error) {
        statusOut.setText(message);
        statusOut.setStyle(error ? "-fx-text-fill: #b00020;" : "");
    }

    private void setBusy(boolean busy) {
        for (Button b : controls) {
            b.setDisable(busy);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(event -> action.run());
        controls.add(b);
        return b;
    }

    private static Label heading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        return l;
    }

    private static void addRow(GridPane grid, int row, String label, Node content) {
        grid.add(new Label(label), 0, row);
        grid.add(content, 1, row);
    }

    private static XYChart.Series<Number, Number> series(String name, List<SeriesPoint> points) {
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(name);
        for (SeriesPoint p : points) {
            s.getData().add(new XYChart.Data<>(p.x(), p.y()));
        }
        return s;
    }
}
