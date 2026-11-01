package dev.quantumlab.desktop;

import static dev.quantumlab.desktop.Tables.column;

import dev.quantumlab.physics.StandardBasis;
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
 * The interference screen: the probability of an outcome against the relative phase of one qubit,
 * next to the classical mixture with the same chances.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * InterferencePresenter}.
 */
public final class InterferenceView {

    private static final String CHART_COLOURS = "CHART_COLOR_1: #1f6fb4; CHART_COLOR_2: #e8590c;";

    private final InterferencePresenter presenter = new InterferencePresenter();
    private final Worker worker = new Worker("quantumlab-interference-worker");

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final TextField notDecayed = new TextField("0.5");
    private final ComboBox<StandardBasis> basis = new ComboBox<>();

    // outputs
    private final Label setupOut = new Label("–");
    private final Label basisOut = new Label("–");
    private final Label heightOut = new Label("–");
    private final Label expectedOut = new Label("–");
    private final Label verdictOut = new Label();
    private final Label captionOut = new Label();
    private final TableView<InterferencePresenter.RowView> table = new TableView<>();
    private final Label statusOut = new Label();

    private final NumberAxis angleAxis = new NumberAxis(-180, 180, 45);
    private final NumberAxis probabilityAxis = new NumberAxis(0, 1, 0.1);
    private final LineChart<Number, Number> chart;

    public InterferenceView() {
        angleAxis.setLabel("Relative phase φ, degrees");
        angleAxis.setAutoRanging(false);
        probabilityAxis.setLabel("Probability");
        probabilityAxis.setAutoRanging(false);
        chart = new LineChart<>(angleAxis, probabilityAxis);
        chart.setTitle("Probability against the relative phase");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(320);
        chart.setMinHeight(260);
        chart.setStyle(CHART_COLOURS);

        basis.getItems().setAll(StandardBasis.values());
        basis.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(StandardBasis b) {
                    if (b == null) {
                        return "";
                    }
                    return switch (b) {
                        case COMPUTATIONAL -> "Computational (Z)";
                        case HADAMARD -> "Hadamard (X)";
                        case CIRCULAR -> "Circular (Y)";
                    };
                }

                @Override
                public StandardBasis fromString(String text) {
                    return null; // the box is not editable
                }
            });
        basis.setValue(StandardBasis.HADAMARD);

        table.setPlaceholder(new Label("No data"));

        root.setLeft(buildInputPane());
        root.setCenter(buildResultsPane());
        root.setBottom(buildFooter());

        onCompute(); // never show an empty screen
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
        notDecayed.setPrefColumnCount(10);
        basis.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "P(not decayed)", notDecayed);
        addRow(form, 1, "Measure nucleus in", basis);

        GridPane numbers = new GridPane();
        numbers.setHgap(8);
        numbers.setVgap(4);
        numbers.getColumnConstraints().add(new ColumnConstraints(170));
        addRow(numbers, 0, "Fringe height (max − min)", heightOut);
        addRow(numbers, 1, "Closed form 2|α||β|", expectedOut);

        for (Label l : new Label[] {setupOut, verdictOut}) {
            l.setWrapText(true);
            l.setMaxWidth(340);
        }

        VBox box =
            new VBox(
                10,
                heading("State and basis"),
                form,
                button("Compute", this::onCompute),
                heading("Last computed"),
                setupOut,
                heading("The fringe"),
                numbers,
                verdictOut);
        box.setPadding(new Insets(16));
        box.setPrefWidth(380);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(400);
        return scroll;
    }

    private Node buildResultsPane() {
        basisOut.setWrapText(true);
        captionOut.setWrapText(true);

        table.getColumns().add(column("Outcome", 160, InterferencePresenter.RowView::outcome));
        table.getColumns().add(column("φ = 0°", 100, InterferencePresenter.RowView::atZero));
        table.getColumns().add(column("φ = 90°", 100, InterferencePresenter.RowView::atQuarter));
        table.getColumns().add(column("φ = 180°", 100, InterferencePresenter.RowView::atHalf));
        table.getColumns()
            .add(column("Classical mixture", 140, InterferencePresenter.RowView::mixture));
        Tables.fitToRows(table);

        VBox box =
            new VBox(
                10,
                heading("Probabilities"),
                basisOut,
                table,
                chart,
                captionOut,
                new ExplanationPane(ExplanationText.forInterferenceScreen()).node());
        box.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private Node buildFooter() {
        Label scope = new Label(ScopeNote.forInterferenceScreen());
        scope.setWrapText(true);
        VBox box = new VBox(4, statusOut, scope);
        box.setPadding(new Insets(8, 16, 12, 16));
        return box;
    }

    // ---------------------------------------------------------------- actions

    private void onCompute() {
        InterferencePresenter.Request request =
            new InterferencePresenter.Request(notDecayed.getText(), basis.getValue());
        run(
            () -> {
                InterferencePresenter.Result result = presenter.compute(request);
                return () -> {
                    show(result);
                    setStatus("Computed exact probabilities.", false);
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

    private void show(InterferencePresenter.Result r) {
        setupOut.setText(r.setup());
        basisOut.setText(r.basis());
        table.getItems().setAll(r.rows());
        heightOut.setText(r.height());
        expectedOut.setText(r.expectedHeight());
        verdictOut.setText(r.verdict());
        captionOut.setText(
            "Probability of " + r.plotted() + " for the state |α||0⟩ + e^{iφ}|β||1⟩, against φ. "
                + "The other outcome's curve is 1 minus this one.");

        chart.getData().clear();
        chart.getData().add(series("Coherent superposition", r.superposition()));
        chart.getData().add(series("Classical mixture", r.mixture()));
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
