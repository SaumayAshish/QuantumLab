
package dev.quantumlab.desktop;

import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.StandardBasis;
import dev.quantumlab.simulation.ExperimentExporter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

/**
 * The measurement screen (modes B and C): prepare a qubit state, measure it repeatedly, and compare
 * the tally with the Born rule.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * LabPresenter}, which is confined to a single worker thread: nothing but typed text goes in and
 * nothing but immutable views comes out, so the UI thread never touches the simulation.
 */
public final class MeasurementView {

    private static final int HISTORY_LINES = 200;
    private static final int CHART_POINTS = 300;
    private static final String DEFAULT_SEED = "12345";
    private static final String SCOPE_NOTE =
        "Each trial prepares a fresh copy of the state and measures it once, sampling the Born"
            + " rule. This models measurement statistics; it does not explain the"
            + " measurement problem, and consciousness plays no role.";

    private record Preset(String label, QubitState state) {
    }

    private static final List<Preset> PRESETS =
        List.of(
            new Preset("|0⟩", QubitState.basis0()),
            new Preset("|1⟩", QubitState.basis1()),
            new Preset("|+⟩", QubitState.plus()),
            new Preset("|−⟩", QubitState.minus()),
            new Preset("|+i⟩", QubitState.plusI()),
            new Preset("|−i⟩", QubitState.minusI()));

    private final LabPresenter presenter = new LabPresenter();
    private final ExecutorService worker =
        Executors.newSingleThreadExecutor(
            task -> {
                Thread t = new Thread(task, "quantumlab-worker");
                t.setDaemon(true);
                return t;
            });

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final TextField alphaRe = new TextField();
    private final TextField alphaIm = new TextField();
    private final TextField betaRe = new TextField();
    private final TextField betaIm = new TextField();
    private final ComboBox<StandardBasis> basis = new ComboBox<>();
    private final TextField seed = new TextField(DEFAULT_SEED);
    private final TextField trials = new TextField("1000");

    // outputs
    private final Label alphaOut = new Label("–");
    private final Label betaOut = new Label("–");
    private final Label p0Out = new Label("–");
    private final Label p1Out = new Label("–");
    private final Label phaseOut = new Label("–");
    private final Label basisOut = new Label("–");
    private final Label noteOut = new Label();
    private final Label trialsOut = new Label("No trials yet.");
    private final Label verdictOut = new Label();
    private final TableView<ResultsView.Row> table = new TableView<>();
    private final ListView<String> history = new ListView<>();
    private final Label statusOut = new Label();
    private final ChartsPane charts = new ChartsPane();
    private final ExplanationPane about = new ExplanationPane();

    public MeasurementView() {
        configureBasisChooser();
        fill(QubitState.plus());
        basis.setValue(StandardBasis.HADAMARD);

        root.setLeft(buildInputPane());
        root.setCenter(buildResultsPane());
        root.setBottom(buildFooter());

        onPrepare(); // never show an empty screen
    }

    public Parent root() {
        return root;
    }

    /**
     * Stops the worker thread. Call when the window closes.
     */
    public void shutdown() {
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------- layout

    private void configureBasisChooser() {
        basis.getItems().setAll(StandardBasis.values());
        basis.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(StandardBasis b) {
                    if (b == null) {
                        return "";
                    }
                    return switch (b) {
                        case COMPUTATIONAL -> "Z: computational {|0⟩, |1⟩}";
                        case HADAMARD -> "X: Hadamard {|+⟩, |−⟩}";
                        case CIRCULAR -> "Y: circular {|+i⟩, |−i⟩}";
                    };
                }

                @Override
                public StandardBasis fromString(String s) {
                    return null; // the chooser is not editable
                }
            });
    }

    private Node buildInputPane() {
        FlowPane presets = new FlowPane(6, 6);
        for (Preset p : PRESETS) {
            Button b = button(p.label(), () -> fill(p.state()));
            presets.getChildren().add(b);
        }

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "α (|0⟩, not decayed)", complexFields(alphaRe, alphaIm));
        addRow(form, 1, "β (|1⟩, decayed)", complexFields(betaRe, betaIm));
        addRow(form, 2, "Measure in", basis);
        addRow(form, 3, "Seed", seed);
        seed.setPrefColumnCount(12);

        Button prepare = button("Prepare state", this::onPrepare);

        GridPane out = new GridPane();
        out.setHgap(8);
        out.setVgap(4);
        addRow(out, 0, "α", alphaOut);
        addRow(out, 1, "β", betaOut);
        addRow(out, 2, "P(|0⟩)", p0Out);
        addRow(out, 3, "P(|1⟩)", p1Out);
        addRow(out, 4, "Relative phase", phaseOut);
        addRow(out, 5, "In this basis", basisOut);
        noteOut.setWrapText(true);

        trials.setPrefColumnCount(8);
        HBox run =
            new HBox(
                8,
                new Label("Trials"),
                trials,
                button("Measure", this::onMeasure),
                button("Reset trials", this::onReset));
        run.setAlignment(Pos.CENTER_LEFT);

        VBox box =
            new VBox(
                10,
                heading("Prepare a state"),
                presets,
                form,
                prepare,
                heading("Prepared state"),
                out,
                noteOut,
                heading("Run"),
                run,
                button("Export JSON…", this::onExport));
        box.setPadding(new Insets(16));
        box.setPrefWidth(480);
        return box;
    }

    private Node buildResultsPane() {
        table.getColumns().add(column("Outcome", 110, ResultsView.Row::label));
        table.getColumns().add(column("Count", 90, ResultsView.Row::count));
        table.getColumns().add(column("Observed", 100, ResultsView.Row::observed));
        table.getColumns().add(column("Theory", 100, ResultsView.Row::theoretical));
        table.getColumns().add(column("Std. error", 100, ResultsView.Row::standardError));
        table.getColumns().add(column("Deviation", 100, ResultsView.Row::sigma));
        Tables.fitToRows(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No data"));

        verdictOut.setWrapText(true);
        VBox.setVgrow(history, Priority.ALWAYS);

        VBox box =
            new VBox(
                10,
                heading("Repeated trials"),
                trialsOut,
                table,
                verdictOut,
                charts.node(),
                heading("History (latest trials, newest last)"),
                history,
                about.node());
        box.setPadding(new Insets(16));
        return box;
    }

    private Node buildFooter() {
        Label scope = new Label(SCOPE_NOTE);
        scope.setWrapText(true);
        VBox box = new VBox(4, statusOut, scope);
        box.setPadding(new Insets(8, 16, 12, 16));
        return box;
    }

    // ---------------------------------------------------------------- actions

    private void onPrepare() {
        LabPresenter.Request request =
            new LabPresenter.Request(
                alphaRe.getText(),
                alphaIm.getText(),
                betaRe.getText(),
                betaIm.getText(),
                basis.getValue(),
                seed.getText());
        run(
            () -> {
                StateView state = presenter.prepare(request);
                ResultsView results = presenter.results();
                List<String> lines = presenter.history(HISTORY_LINES);
                ChartData chart = presenter.chartData(CHART_POINTS);
                return () -> {
                    showState(state);
                    showResults(results, lines, chart);
                    setStatus("State prepared. Seed " + request.seed().trim() + ".", false);
                };
            });
    }

    private void onMeasure() {
        String count = trials.getText();
        run(
            () -> {
                ResultsView results = presenter.measure(count);
                List<String> lines = presenter.history(HISTORY_LINES);
                ChartData chart = presenter.chartData(CHART_POINTS);
                return () -> {
                    showResults(results, lines, chart);
                    setStatus("Ran trials; " + results.trials() + " in total.", false);
                };
            });
    }

    private void onReset() {
        run(
            () -> {
                ResultsView results = presenter.reset();
                ChartData chart = presenter.chartData(CHART_POINTS);
                return () -> {
                    showResults(results, List.of(), chart);
                    setStatus("Trials cleared; the generator is back at its seed.", false);
                };
            });
    }

    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export experiment");
        chooser.setInitialFileName("quantumlab-experiment.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file =
            chooser.showSaveDialog(
                root.getScene() == null ? null : root.getScene().getWindow());
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        run(
            () -> {
                try {
                    ExperimentExporter.write(path, presenter.exportJson());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return () -> setStatus("Exported to " + path, false);
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
                } catch (UncheckedIOException e) {
                    String message = "Could not write the file: " + e.getCause().getMessage();
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

    private void showState(StateView s) {
        alphaOut.setText(s.alpha());
        betaOut.setText(s.beta());
        p0Out.setText(s.probability0());
        p1Out.setText(s.probability1());
        phaseOut.setText(s.relativePhase());
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < s.basisLabels().size(); k++) {
            if (k > 0) {
                sb.append("    ");
            }
            sb.append(s.basisLabels().get(k)).append("  ").append(s.basisProbabilities().get(k));
        }
        basisOut.setText(sb.toString());
        noteOut.setText(s.note());
    }

    private void showResults(ResultsView r, List<String> lines, ChartData chart) {
        trialsOut.setText(
            r.trials() == 0
                ? "No trials yet."
                : r.trials() + " trials (" + r.remaining() + " more allowed).");
        table.getItems().setAll(r.rows());
        verdictOut.setText(r.trials() == 0 ? "" : r.verdict());
        history.getItems().setAll(lines);
        if (!lines.isEmpty()) {
            history.scrollTo(lines.size() - 1);
        }
        charts.show(chart);
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

    private void fill(QubitState state) {
        LabPresenter.Request r = LabPresenter.Request.fromState(state, basis.getValue(), seed.getText());
        alphaRe.setText(r.alphaRe());
        alphaIm.setText(r.alphaIm());
        betaRe.setText(r.betaRe());
        betaIm.setText(r.betaIm());
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

    private static HBox complexFields(TextField re, TextField im) {
        re.setPrefColumnCount(11);
        im.setPrefColumnCount(11);
        HBox box = new HBox(6, re, new Label("+"), im, new Label("i"));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static void addRow(GridPane grid, int row, String label, Node content) {
        grid.add(new Label(label), 0, row);
        grid.add(content, 1, row);
    }

    private static TableColumn<ResultsView.Row, String> column(
        String title, double width, Function<ResultsView.Row, String> value) {
        TableColumn<ResultsView.Row, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }
}
