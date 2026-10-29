package dev.quantumlab.desktop;

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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * The classical decay screen (mode A): a nucleus with a definite state, a detector, a trigger and a
 * cat, each following the one before. Repeating the run only shows how often each fate occurs.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * ClassicalPresenter}, which is confined to a single worker thread, exactly as in {@link
 * MeasurementView}.
 */
public final class ClassicalView {

    private static final String DEFAULT_RATE = "0.693147"; // ln 2, so the half-life is 1
    private static final String DEFAULT_TIME = "1";
    private static final String DEFAULT_SEED = "12345";
    private static final String SCOPE_NOTE =
            "Every link in the chain has a definite state at every moment. The randomness is"
                    + " ignorance of when the nucleus decays, not a superposition: this screen never"
                    + " shows a cat that is both alive and dead.";

    private final ClassicalPresenter presenter = new ClassicalPresenter();
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor(
                    task -> {
                        Thread t = new Thread(task, "quantumlab-classical-worker");
                        t.setDaemon(true);
                        return t;
                    });

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final TextField rate = new TextField(DEFAULT_RATE);
    private final TextField time = new TextField(DEFAULT_TIME);
    private final TextField seed = new TextField(DEFAULT_SEED);
    private final TextField runs = new TextField("1000");

    // outputs
    private final Label halfLifeOut = new Label("–");
    private final Label decayOut = new Label("–");
    private final Label survivalOut = new Label("–");
    private final Label nucleusOut = new Label("–");
    private final Label detectorOut = new Label("–");
    private final Label triggerOut = new Label("–");
    private final Label catOut = new Label("–");
    private final Label drawOut = new Label("No run yet.");
    private final Label runsOut = new Label("No runs yet.");
    private final Label verdictOut = new Label();
    private final TableView<ResultsView.Row> table = new TableView<>();
    private final Label statusOut = new Label();

    public ClassicalView() {
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

    private Node buildInputPane() {
        rate.setPrefColumnCount(12);
        time.setPrefColumnCount(12);
        seed.setPrefColumnCount(12);
        runs.setPrefColumnCount(8);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "Decay rate λ", rate);
        addRow(form, 1, "Observation time t", time);
        addRow(form, 2, "Seed", seed);

        GridPane law = new GridPane();
        law.setHgap(8);
        law.setVgap(4);
        addRow(law, 0, "Half-life ln 2 / λ", halfLifeOut);
        addRow(law, 1, "P(decayed by t)", decayOut);
        addRow(law, 2, "P(not decayed by t)", survivalOut);

        HBox batch =
                new HBox(8, new Label("Runs"), runs, button("Run", this::onRunMany));
        batch.setAlignment(Pos.CENTER_LEFT);

        VBox box =
                new VBox(
                        10,
                        heading("Decay law"),
                        form,
                        button("Prepare experiment", this::onPrepare),
                        heading("Prepared law"),
                        law,
                        heading("Run"),
                        button("Run once", this::onRunOnce),
                        batch,
                        button("Reset runs", this::onReset),
                        button("Export JSON…", this::onExport));
        box.setPadding(new Insets(16));
        box.setPrefWidth(400);
        return box;
    }

    private Node buildResultsPane() {
        HBox chain =
                new HBox(
                        10,
                        link("Nucleus", nucleusOut),
                        arrow(),
                        link("Detector", detectorOut),
                        arrow(),
                        link("Trigger", triggerOut),
                        arrow(),
                        link("Cat", catOut));
        chain.setAlignment(Pos.CENTER_LEFT);

        table.getColumns().add(column("Outcome", 110, ResultsView.Row::label));
        table.getColumns().add(column("Count", 90, ResultsView.Row::count));
        table.getColumns().add(column("Observed", 100, ResultsView.Row::observed));
        table.getColumns().add(column("Theory", 100, ResultsView.Row::theoretical));
        table.getColumns().add(column("Std. error", 100, ResultsView.Row::standardError));
        table.getColumns().add(column("Deviation", 100, ResultsView.Row::sigma));
        table.setPrefHeight(130);
        table.setPlaceholder(new Label("No data"));

        drawOut.setWrapText(true);
        verdictOut.setWrapText(true);

        VBox box =
                new VBox(
                        10,
                        heading("Apparatus (state after the latest run)"),
                        chain,
                        drawOut,
                        heading("Repeated runs"),
                        runsOut,
                        table,
                        verdictOut,
                        new ExplanationPane(ExplanationText.forClassicalScreen()).node());
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
        ClassicalPresenter.Request request =
                new ClassicalPresenter.Request(rate.getText(), time.getText(), seed.getText());
        run(
                () -> {
                    ClassicalPresenter.SetupView setup = presenter.prepare(request);
                    ResultsView results = presenter.results();
                    ClassicalPresenter.ApparatusView apparatus = presenter.apparatus();
                    String draw = presenter.lastDraw();
                    return () -> {
                        showSetup(setup);
                        showResults(results);
                        showApparatus(apparatus, draw);
                        setStatus("Experiment prepared. Seed " + request.seed().trim() + ".", false);
                    };
                });
    }

    private void onRunOnce() {
        run(
                () -> {
                    ClassicalPresenter.ApparatusView apparatus = presenter.runOnce();
                    String draw = presenter.lastDraw();
                    ResultsView results = presenter.results();
                    return () -> {
                        showApparatus(apparatus, draw);
                        showResults(results);
                        setStatus("One run; " + results.trials() + " in total.", false);
                    };
                });
    }

    private void onRunMany() {
        String count = runs.getText();
        run(
                () -> {
                    ResultsView results = presenter.run(count);
                    ClassicalPresenter.ApparatusView apparatus = presenter.apparatus();
                    String draw = presenter.lastDraw();
                    return () -> {
                        showApparatus(apparatus, draw);
                        showResults(results);
                        setStatus("Ran the experiment; " + results.trials() + " runs in total.", false);
                    };
                });
    }

    private void onReset() {
        run(
                () -> {
                    ResultsView results = presenter.reset();
                    ClassicalPresenter.ApparatusView apparatus = presenter.apparatus();
                    String draw = presenter.lastDraw();
                    return () -> {
                        showApparatus(apparatus, draw);
                        showResults(results);
                        setStatus("Runs cleared; the generator is back at its seed.", false);
                    };
                });
    }

    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export experiment");
        chooser.setInitialFileName("quantumlab-classical-decay.json");
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

    private void showSetup(ClassicalPresenter.SetupView s) {
        halfLifeOut.setText(s.halfLife());
        decayOut.setText(s.decayProbability());
        survivalOut.setText(s.survivalProbability());
    }

    private void showApparatus(ClassicalPresenter.ApparatusView a, String draw) {
        nucleusOut.setText(a.nucleus());
        detectorOut.setText(a.detector());
        triggerOut.setText(a.trigger());
        catOut.setText(a.cat());
        drawOut.setText(draw);
    }

    private void showResults(ResultsView r) {
        runsOut.setText(
                r.trials() == 0
                        ? "No runs yet."
                        : r.trials() + " runs (" + r.remaining() + " more allowed).");
        table.getItems().setAll(r.rows());
        verdictOut.setText(r.trials() == 0 ? "" : r.verdict());
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

    /**
     * One link of the chain: a small caption above its current state.
     */
    private static Node link(String caption, Label state) {
        state.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        return new VBox(2, new Label(caption), state);
    }

    private static Label arrow() {
        return new Label("→");
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
