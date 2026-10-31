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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * The one-dimensional Schrödinger screen: pick a potential, solve for its lowest energy levels on a
 * grid, compare each level with the analytic value, and plot the wavefunction of the selected level.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * SchrodingerPresenter}, which is confined to a single worker thread, as in the other views.
 */
public final class SchrodingerView {

    private final SchrodingerPresenter presenter = new SchrodingerPresenter();
    private final Worker worker = new Worker("quantumlab-schrodinger-worker");

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final ComboBox<SchrodingerPresenter.PotentialKind> potential = new ComboBox<>();
    private final TextField xMin = new TextField();
    private final TextField xMax = new TextField();
    private final TextField gridPoints = new TextField();
    private final TextField omega = new TextField();
    private final TextField levels = new TextField();

    // outputs
    private final Label potentialOut = new Label("–");
    private final Label domainOut = new Label("–");
    private final Label pointsOut = new Label("–");
    private final Label spacingOut = new Label("–");
    private final Label noteOut = new Label();
    private final TableView<SchrodingerPresenter.LevelRow> table = new TableView<>();
    private final Label waveInfo = new Label("Solve to see the levels.");
    private final Label statusOut = new Label();

    private final NumberAxis amplitudeX = positionAxis();
    private final NumberAxis densityX = positionAxis();
    private final LineChart<Number, Number> amplitudeChart;
    private final LineChart<Number, Number> densityChart;

    /**
     * True while the table is being filled, so that selecting row 0 does not trigger a reload.
     */
    private boolean populating;

    public SchrodingerView() {
        amplitudeChart = chart("Wavefunction ψ(x)", amplitudeX, "ψ");
        densityChart = chart("Probability density |ψ(x)|²", densityX, "|ψ|²");

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

        table.getSelectionModel()
            .selectedIndexProperty()
            .addListener(
                (obs, old, index) -> {
                    if (!populating && index != null && index.intValue() >= 0) {
                        onSelect(index.intValue());
                    }
                });

        root.setLeft(buildInputPane());
        root.setCenter(buildResultsPane());
        root.setBottom(buildFooter());

        onSolve(); // never show an empty screen
    }

    public Parent root() {
        return root;
    }

    /**
     * Stops the worker thread. Call when the window closes.
     */
    public void shutdown() {
        worker.shutdown();
    }

    // ---------------------------------------------------------------- layout

    private Node buildInputPane() {
        for (TextField f : new TextField[]{xMin, xMax, gridPoints, omega, levels}) {
            f.setPrefColumnCount(10);
        }
        potential.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "Potential", potential);
        addRow(form, 1, "Left wall", xMin);
        addRow(form, 2, "Right wall", xMax);
        addRow(form, 3, "Grid points", gridPoints);
        addRow(form, 4, "Frequency ω", omega);
        addRow(form, 5, "Levels", levels);

        GridPane setup = new GridPane();
        setup.setHgap(8);
        setup.setVgap(4);
        setup.getColumnConstraints().add(new ColumnConstraints(85));
        potentialOut.setWrapText(true);
        potentialOut.setMaxWidth(300);
        addRow(setup, 0, "Potential", potentialOut);
        addRow(setup, 1, "Domain", domainOut);
        addRow(setup, 2, "Grid points", pointsOut);
        addRow(setup, 3, "Spacing h", spacingOut);

        VBox box =
            new VBox(
                10,
                heading("Problem"),
                form,
                button("Solve", this::onSolve),
                heading("Last solved"),
                setup);
        box.setPadding(new Insets(16));
        box.setPrefWidth(360);
        return box;
    }

    private Node buildResultsPane() {
        noteOut.setWrapText(true);
        waveInfo.setWrapText(true);

        table.getColumns().add(column("Level", 70, SchrodingerPresenter.LevelRow::label));
        table.getColumns().add(column("Numerical E", 120, SchrodingerPresenter.LevelRow::numerical));
        table.getColumns().add(column("Reference E", 120, SchrodingerPresenter.LevelRow::reference));
        table.getColumns().add(column("Error", 110, SchrodingerPresenter.LevelRow::error));
        table.getColumns()
            .add(column("Relative error", 120, SchrodingerPresenter.LevelRow::relativeError));
        Tables.fitToRows(table);
        table.setPlaceholder(new Label("No data"));

        HBox charts = new HBox(10, amplitudeChart, densityChart);

        VBox box =
            new VBox(
                10,
                heading("Energy levels (ħ = m = 1)"),
                table,
                noteOut,
                heading("Selected level (click a row)"),
                waveInfo,
                charts,
                new ExplanationPane(ExplanationText.forSchrodingerScreen()).node());
        box.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private Node buildFooter() {
        Label scope = new Label(ScopeNote.forSchrodingerScreen());
        scope.setWrapText(true);
        VBox box = new VBox(4, statusOut, scope);
        box.setPadding(new Insets(8, 16, 12, 16));
        return box;
    }

    // ---------------------------------------------------------------- actions

    private void onSolve() {
        SchrodingerPresenter.Request request =
            new SchrodingerPresenter.Request(
                potential.getValue(),
                xMin.getText(),
                xMax.getText(),
                gridPoints.getText(),
                omega.getText(),
                levels.getText());
        run(
            () -> {
                SchrodingerPresenter.SolutionView solution = presenter.solve(request);
                SchrodingerPresenter.WaveView wave = presenter.wave(0);
                return () -> {
                    populating = true;
                    try {
                        table.getItems().setAll(solution.levels());
                        table.getSelectionModel().select(0);
                    } finally {
                        populating = false;
                    }
                    showSetup(solution.setup());
                    showWave(wave);
                    setStatus(
                        "Solved "
                            + solution.levels().size()
                            + " levels on "
                            + solution.setup().gridPoints()
                            + " grid points.",
                        false);
                };
            });
    }

    private void onSelect(int index) {
        run(
            () -> {
                SchrodingerPresenter.WaveView wave = presenter.wave(index);
                return () -> {
                    showWave(wave);
                    setStatus("Showing " + wave.label() + ".", false);
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

    /**
     * Typical settings for each potential; the user can change them afterwards.
     */
    private void applyDefaults(SchrodingerPresenter.PotentialKind kind) {
        boolean well = kind == SchrodingerPresenter.PotentialKind.INFINITE_WELL;
        xMin.setText(well ? "0" : "-10");
        xMax.setText(well ? "1" : "10");
        gridPoints.setText(well ? "500" : "1000");
        levels.setText("6");
        omega.setText("1");
        omega.setDisable(well);
    }

    private void showSetup(SchrodingerPresenter.SetupView s) {
        potentialOut.setText(s.potential());
        domainOut.setText(s.domain());
        pointsOut.setText(s.gridPoints());
        spacingOut.setText(s.spacing());
        noteOut.setText(s.note());
    }

    private void showWave(SchrodingerPresenter.WaveView w) {
        waveInfo.setText(
            w.label()
                + "   E = "
                + w.energy()
                + "   nodes: "
                + w.nodes()
                + "   ∑|ψ|²h = "
                + w.normalisation());

        double left = w.amplitude().get(0).x();
        double right = w.amplitude().get(w.amplitude().size() - 1).x();
        fixRange(amplitudeX, left, right);
        fixRange(densityX, left, right);

        amplitudeChart.getData().clear();
        amplitudeChart.getData().add(series("Numerical", w.amplitude()));
        if (!w.exact().isEmpty()) {
            amplitudeChart.getData().add(series("Exact", w.exact()));
        }
        densityChart.getData().clear();
        densityChart.getData().add(series("Numerical", w.density()));
    }

    private void setStatus(String message, boolean error) {
        statusOut.setText(message);
        statusOut.setStyle(error ? "-fx-text-fill: #b00020;" : "");
    }

    private void setBusy(boolean busy) {
        for (Button b : controls) {
            b.setDisable(busy);
        }
        table.setDisable(busy);
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

    private static NumberAxis positionAxis() {
        NumberAxis axis = new NumberAxis();
        axis.setLabel("x");
        axis.setAutoRanging(false);
        axis.setForceZeroInRange(false);
        return axis;
    }

    private static void fixRange(NumberAxis axis, double lower, double upper) {
        axis.setLowerBound(lower);
        axis.setUpperBound(upper);
        axis.setTickUnit((upper - lower) / 10.0);
    }

    private static LineChart<Number, Number> chart(String title, NumberAxis x, String yLabel) {
        NumberAxis y = new NumberAxis();
        y.setLabel(yLabel);
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(280);
        chart.setMinHeight(220);
        HBox.setHgrow(chart, Priority.ALWAYS);
        return chart;
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
