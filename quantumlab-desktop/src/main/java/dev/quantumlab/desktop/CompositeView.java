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
 * The composite-system screen (Mode D): a nucleus and an idealised detector as one two-qubit state,
 * compared with the classical mixture that has the same chances.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * CompositePresenter}.
 */
public final class CompositeView {

    private static final String SCOPE_NOTE =
        "Exact probabilities for two two-level systems. The detector is a two-level stand-in,"
            + " not a model of a real detector. Nothing evolves in time, nothing is sampled"
            + " or collapses, and it is a separate model from the cat: it does not describe"
            + " a cat.";

    private final CompositePresenter presenter = new CompositePresenter();
    private final ExecutorService worker =
        Executors.newSingleThreadExecutor(
            task -> {
                Thread t = new Thread(task, "quantumlab-composite-worker");
                t.setDaemon(true);
                return t;
            });

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final TextField notDecayed = new TextField("0.5");
    private final TextField phase = new TextField("0");
    private final ComboBox<CompositePresenter.BasisChoice> nucleusBasis = new ComboBox<>();
    private final ComboBox<CompositePresenter.BasisChoice> detectorBasis = new ComboBox<>();

    // outputs
    private final Label setupOut = new Label("–");
    private final Label basesOut = new Label("–");
    private final Label agreeEntangledOut = new Label("–");
    private final Label agreeMixtureOut = new Label("–");
    private final Label aloneOut = new Label();
    private final Label verdictBasesOut = new Label();
    private final Label concurrenceOut = new Label("–");
    private final Label entropyOut = new Label("–");
    private final Label purityOut = new Label("–");
    private final Label entanglementNoteOut = new Label();
    private final Label settingsOut = new Label();
    private final Label chshEntangledOut = new Label("–");
    private final Label chshMixtureSameOut = new Label("–");
    private final Label chshMixtureBestOut = new Label("–");
    private final Label chshMaximumOut = new Label("–");
    private final Label chshVerdictOut = new Label();
    private final TableView<CompositePresenter.RowView> table = new TableView<>();
    private final Label statusOut = new Label();

    private final NumberAxis angleAxis = new NumberAxis(0, 180, 20);
    private final NumberAxis correlationAxis = new NumberAxis(0, 1, 0.1);
    private final LineChart<Number, Number> chart;

    public CompositeView() {
        angleAxis.setLabel("Measurement axis, degrees from z");
        angleAxis.setAutoRanging(false);
        correlationAxis.setLabel("Correlation E");
        correlationAxis.setAutoRanging(false);
        chart = new LineChart<>(angleAxis, correlationAxis);
        chart.setTitle("Correlation along matching axes");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(320);
        chart.setMinHeight(240);
        chart.setStyle("CHART_COLOR_1: #1f6fb4; CHART_COLOR_2: #e8590c;");

        configureBasisBox(nucleusBasis);
        configureBasisBox(detectorBasis);

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
        nucleusBasis.setMaxWidth(Double.MAX_VALUE);
        detectorBasis.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "P(not decayed)", notDecayed);
        addRow(form, 1, "Relative phase φ (rad)", phase);
        addRow(form, 2, "Measure nucleus in", nucleusBasis);
        addRow(form, 3, "Measure detector in", detectorBasis);

        GridPane entanglement = new GridPane();
        entanglement.setHgap(8);
        entanglement.setVgap(4);
        entanglement.getColumnConstraints().add(new ColumnConstraints(130));
        addRow(entanglement, 0, "Concurrence C", concurrenceOut);
        addRow(entanglement, 1, "Entropy of either qubit", entropyOut);
        addRow(entanglement, 2, "Purity of either qubit", purityOut);

        GridPane chsh = new GridPane();
        chsh.setHgap(8);
        chsh.setVgap(4);
        chsh.getColumnConstraints().add(new ColumnConstraints(190));
        addRow(chsh, 0, "S, entangled state", chshEntangledOut);
        addRow(chsh, 1, "S, mixture, same axes", chshMixtureSameOut);
        addRow(chsh, 2, "S, mixture, best axes", chshMixtureBestOut);
        addRow(chsh, 3, "Largest S for this state", chshMaximumOut);

        for (Label l :
            new Label[] {setupOut, entanglementNoteOut, settingsOut, chshVerdictOut}) {
            l.setWrapText(true);
            l.setMaxWidth(340);
        }
        Label bounds = new Label("Classical bound 2. Quantum bound 2√2 ≈ 2.8284.");
        bounds.setWrapText(true);
        bounds.setMaxWidth(340);

        VBox box =
            new VBox(
                10,
                heading("State"),
                form,
                button("Compute", this::onRun),
                heading("Last computed"),
                setupOut,
                heading("Entanglement"),
                entanglement,
                entanglementNoteOut,
                heading("CHSH quantity S"),
                chsh,
                bounds,
                settingsOut,
                chshVerdictOut);
        box.setPadding(new Insets(16));
        box.setPrefWidth(380);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(400);
        return scroll;
    }

    private Node buildResultsPane() {
        basesOut.setWrapText(true);
        aloneOut.setWrapText(true);
        verdictBasesOut.setWrapText(true);
        verdictBasesOut.setStyle("-fx-font-weight: bold;");

        table.getColumns().add(column("Joint outcome", 250, CompositePresenter.RowView::outcome));
        table.getColumns().add(column("Entangled", 110, CompositePresenter.RowView::entangled));
        table.getColumns().add(column("Mixture", 110, CompositePresenter.RowView::mixture));
        table.getColumns()
            .add(column("Entangled − mixture", 150, CompositePresenter.RowView::difference));
        Tables.fitToRows(table);

        GridPane agree = new GridPane();
        agree.setHgap(8);
        agree.setVgap(4);
        agree.getColumnConstraints().add(new ColumnConstraints(260));
        addRow(agree, 0, "Outcomes agree, entangled state", agreeEntangledOut);
        addRow(agree, 1, "Outcomes agree, classical mixture", agreeMixtureOut);

        VBox box =
            new VBox(
                10,
                heading("Joint measurement statistics"),
                basesOut,
                table,
                agree,
                verdictBasesOut,
                aloneOut,
                chart,
                new ExplanationPane(ExplanationText.forCompositeScreen()).node());
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
        CompositePresenter.Request request =
            new CompositePresenter.Request(
                notDecayed.getText(),
                phase.getText(),
                nucleusBasis.getValue(),
                detectorBasis.getValue());
        run(
            () -> {
                CompositePresenter.Result result = presenter.compute(request);
                return () -> {
                    show(result);
                    setStatus("Computed exact probabilities for both descriptions.", false);
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

    private void show(CompositePresenter.Result r) {
        setupOut.setText(r.setup());
        basesOut.setText(r.bases());
        table.getItems().setAll(r.rows());
        agreeEntangledOut.setText(r.agreeEntangled());
        agreeMixtureOut.setText(r.agreeMixture());
        aloneOut.setText(r.alone());
        verdictBasesOut.setText(r.verdictBases());
        concurrenceOut.setText(r.concurrence());
        entropyOut.setText(r.entropy());
        purityOut.setText(r.purity());
        entanglementNoteOut.setText(r.entanglementNote());
        settingsOut.setText(r.settings());
        chshEntangledOut.setText(r.chshEntangled());
        chshMixtureSameOut.setText(r.chshMixtureSame());
        chshMixtureBestOut.setText(r.chshMixtureBest());
        chshMaximumOut.setText(r.chshMaximum());
        chshVerdictOut.setText(r.chshVerdict());

        chart.getData().clear();
        chart.getData().add(series("Entangled state", r.entangledCurve()));
        chart.getData().add(series("Classical mixture", r.mixtureCurve()));
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

    private static void configureBasisBox(ComboBox<CompositePresenter.BasisChoice> box) {
        box.getItems().setAll(CompositePresenter.BasisChoice.values());
        box.setConverter(
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
        box.setValue(CompositePresenter.BasisChoice.COMPUTATIONAL);
    }

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

    private static TableColumn<CompositePresenter.RowView, String> column(
        String title, double width, Function<CompositePresenter.RowView, String> value) {
        TableColumn<CompositePresenter.RowView, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }
}
