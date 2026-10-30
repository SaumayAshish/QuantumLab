package dev.quantumlab.desktop;

import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * The decoherence screen (Mode E): one qubit, a chosen dephasing rate and time, and what that does
 * to the statistics, the interference fringe and the purity.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * DecoherencePresenter}.
 */
public final class DecoherenceView {

    private static final String SCOPE_NOTE =
        "Exact probabilities for one two-level system with an assumed dephasing rate. The rate"
            + " is an input, not derived from a real environment. Nothing is sampled, no"
            + " single outcome or collapse is shown, and it is a separate model from the"
            + " cat: it does not describe a cat.";

    private static final String CHART_COLOURS =
        "CHART_COLOR_1: #1f6fb4; CHART_COLOR_2: #e8590c; CHART_COLOR_3: #2a9d4b;";

    private final DecoherencePresenter presenter = new DecoherencePresenter();
    private final ExecutorService worker =
        Executors.newSingleThreadExecutor(
            task -> {
                Thread t = new Thread(task, "quantumlab-decoherence-worker");
                t.setDaemon(true);
                return t;
            });

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final TextField notDecayed = new TextField("0.5");
    private final TextField phase = new TextField("0");
    private final TextField rate = new TextField("0.5");
    private final TextField time = new TextField("2");
    private final ComboBox<CompositePresenter.BasisChoice> basis = new ComboBox<>();

    // outputs
    private final Label setupOut = new Label("–");
    private final Label basisOut = new Label("–");
    private final Label zChancesOut = new Label();
    private final Label factorOut = new Label("–");
    private final Label coherenceTimeOut = new Label();
    private final Label visibilityInitialOut = new Label("–");
    private final Label visibilityNowOut = new Label("–");
    private final Label purityInitialOut = new Label("–");
    private final Label purityNowOut = new Label("–");
    private final Label entropyInitialOut = new Label("–");
    private final Label entropyNowOut = new Label("–");
    private final Label blochOut = new Label("–");
    private final Label verdictOut = new Label();
    private final TableView<DecoherencePresenter.RowView> table = new TableView<>();
    private final Label statusOut = new Label();

    private final NumberAxis timeAxis = new NumberAxis(0, 10, 1);
    private final NumberAxis fractionAxis = new NumberAxis(0, 1, 0.1);
    private final LineChart<Number, Number> timeChart;
    private final NumberAxis angleAxis = new NumberAxis(0, 360, 45);
    private final NumberAxis probabilityAxis = new NumberAxis(0, 1, 0.1);
    private final LineChart<Number, Number> fringeChart;

    public DecoherenceView() {
        timeAxis.setLabel("Time t");
        timeAxis.setAutoRanging(false);
        fractionAxis.setLabel("Visibility, purity, entropy (bits)");
        fractionAxis.setAutoRanging(false);
        timeChart = new LineChart<>(timeAxis, fractionAxis);
        timeChart.setTitle("Coherence lost over time");
        timeChart.setAnimated(false);
        timeChart.setCreateSymbols(false);
        timeChart.setPrefHeight(300);
        timeChart.setMinHeight(240);
        timeChart.setStyle(CHART_COLOURS);

        angleAxis.setLabel("Analyser angle φ, degrees");
        angleAxis.setAutoRanging(false);
        probabilityAxis.setLabel("Probability of (|0⟩ + e^{iφ}|1⟩)/√2");
        probabilityAxis.setAutoRanging(false);
        fringeChart = new LineChart<>(angleAxis, probabilityAxis);
        fringeChart.setTitle("Interference fringe");
        fringeChart.setAnimated(false);
        fringeChart.setCreateSymbols(false);
        fringeChart.setPrefHeight(300);
        fringeChart.setMinHeight(240);
        fringeChart.setStyle(CHART_COLOURS);

        basis.getItems().setAll(CompositePresenter.BasisChoice.values());
        basis.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(CompositePresenter.BasisChoice b) {
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
                public CompositePresenter.BasisChoice fromString(String text) {
                    return null; // the box is not editable
                }
            });
        basis.setValue(CompositePresenter.BasisChoice.HADAMARD);

        table.setPlaceholder(new Label("No data"));

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
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------- layout

    private Node buildInputPane() {
        notDecayed.setPrefColumnCount(10);
        phase.setPrefColumnCount(10);
        rate.setPrefColumnCount(10);
        time.setPrefColumnCount(10);
        basis.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "P(not decayed)", notDecayed);
        addRow(form, 1, "Relative phase φ (rad)", phase);
        addRow(form, 2, "Dephasing rate γ", rate);
        addRow(form, 3, "Time t", time);
        addRow(form, 4, "Measure nucleus in", basis);

        GridPane numbers = new GridPane();
        numbers.setHgap(8);
        numbers.setVgap(4);
        numbers.getColumnConstraints().add(new ColumnConstraints(150));
        addRow(numbers, 0, "Coherence left e^(−γt)", factorOut);
        addRow(numbers, 1, "Fringe visibility, t = 0", visibilityInitialOut);
        addRow(numbers, 2, "Fringe visibility, now", visibilityNowOut);
        addRow(numbers, 3, "Purity, t = 0", purityInitialOut);
        addRow(numbers, 4, "Purity, now", purityNowOut);
        addRow(numbers, 5, "Entropy, t = 0", entropyInitialOut);
        addRow(numbers, 6, "Entropy, now", entropyNowOut);
        addRow(numbers, 7, "Bloch vector, now", blochOut);

        for (Label l : new Label[] {setupOut, coherenceTimeOut, verdictOut}) {
            l.setWrapText(true);
            l.setMaxWidth(340);
        }

        VBox box =
            new VBox(
                10,
                heading("State and dephasing"),
                form,
                button("Compute", this::onRun),
                heading("Last computed"),
                setupOut,
                heading("The qubit"),
                numbers,
                coherenceTimeOut,
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
        zChancesOut.setWrapText(true);

        table.getColumns().add(column("Outcome", 200, DecoherencePresenter.RowView::outcome));
        table.getColumns().add(column("At t = 0", 120, DecoherencePresenter.RowView::initial));
        table.getColumns().add(column("At time t", 120, DecoherencePresenter.RowView::now));
        table.getColumns().add(column("Long-time limit", 140, DecoherencePresenter.RowView::limit));
        table.setPrefHeight(130);
        table.setMinHeight(110);

        VBox box =
            new VBox(
                10,
                heading("Measurement statistics"),
                basisOut,
                table,
                zChancesOut,
                timeChart,
                fringeChart,
                new ExplanationPane(ExplanationText.forDecoherenceScreen()).node());
        box.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private Node buildFooter() {
        Label scope = new Label(SCOPE_NOTE);
        scope.setWrapText(true);
        VBox box = new VBox(4, statusOut, scope);
        box.setPadding(new Insets(8, 16, 12, 16));
        return box;
    }

    // ---------------------------------------------------------------- actions

    private void onRun() {
        DecoherencePresenter.Request request =
            new DecoherencePresenter.Request(
                notDecayed.getText(),
                phase.getText(),
                rate.getText(),
                time.getText(),
                basis.getValue());
        run(
            () -> {
                DecoherencePresenter.Result result = presenter.compute(request);
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
        setBusy(true);
        worker.submit(
            () -> {
                Runnable ui;
                try {
                    ui = work.get();
                } catch (InvalidInputException e) {
                    String message = e.getMessage();
                    ui = () -> setStatus(message, true);
                } catch (RuntimeException e) {
                    String message = "Unexpected error: " + e;
                    ui = () -> setStatus(message, true);
                }
                Runnable update = ui;
                Platform.runLater(
                    () -> {
                        update.run();
                        setBusy(false);
                    });
            });
    }

    // ---------------------------------------------------------------- display

    private void show(DecoherencePresenter.Result r) {
        setupOut.setText(r.setup());
        basisOut.setText(r.basis());
        table.getItems().setAll(r.rows());
        zChancesOut.setText(r.zChances());
        factorOut.setText(r.factor());
        coherenceTimeOut.setText(r.coherenceTime());
        visibilityInitialOut.setText(r.visibilityInitial());
        visibilityNowOut.setText(r.visibilityNow());
        purityInitialOut.setText(r.purityInitial());
        purityNowOut.setText(r.purityNow());
        entropyInitialOut.setText(r.entropyInitial());
        entropyNowOut.setText(r.entropyNow());
        blochOut.setText(r.bloch());
        verdictOut.setText(r.verdict());

        timeAxis.setUpperBound(r.timeSpan());
        timeAxis.setTickUnit(r.timeSpan() / 10.0);
        timeChart.getData().clear();
        timeChart.getData().add(series("Fringe visibility", r.visibilityCurve()));
        timeChart.getData().add(series("Purity", r.purityCurve()));
        timeChart.getData().add(series("Entropy (bits)", r.entropyCurve()));

        fringeChart.getData().clear();
        fringeChart.getData().add(series("At t = 0", r.fringeInitial()));
        fringeChart.getData().add(series("At time t", r.fringeNow()));
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

    private static TableColumn<DecoherencePresenter.RowView, String> column(
        String title, double width, Function<DecoherencePresenter.RowView, String> value) {
        TableColumn<DecoherencePresenter.RowView, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }
}
