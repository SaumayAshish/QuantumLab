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
 * The tunnelling screen: the probability that a particle gets through one or two rectangular
 * barriers, as a function of its energy, next to what a classical particle would do.
 *
 * <p>This class only lays out controls and displays immutable views. All logic is in {@link
 * TunnellingPresenter}.
 */
public final class TunnellingView {

    private final TunnellingPresenter presenter = new TunnellingPresenter();
    private final Worker worker = new Worker("quantumlab-tunnelling-worker");

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final ComboBox<TunnellingPresenter.BarrierKind> kind = new ComboBox<>();
    private final TextField height = new TextField();
    private final TextField width = new TextField();
    private final TextField gap = new TextField();
    private final TextField maxEnergy = new TextField();
    private final TextField probeEnergy = new TextField();

    // outputs
    private final Label setupOut = new Label("–");
    private final Label probeEnergyOut = new Label("–");
    private final Label probeRegimeOut = new Label("–");
    private final Label probeClassicalOut = new Label("–");
    private final Label probeTransmissionOut = new Label("–");
    private final Label probeReflectionOut = new Label("–");
    private final Label probeDecayOut = new Label("–");
    private final Label probeEstimateOut = new Label("–");
    private final Label peakOut = new Label();
    private final Label noteOut = new Label();
    private final TableView<TunnellingPresenter.RowView> table = new TableView<>();
    private final Label statusOut = new Label();

    private final NumberAxis energyAxis = new NumberAxis();
    private final NumberAxis probabilityAxis = new NumberAxis();
    private final LineChart<Number, Number> chart;

    public TunnellingView() {
        energyAxis.setLabel("Energy E");
        energyAxis.setAutoRanging(false);
        energyAxis.setForceZeroInRange(true);
        probabilityAxis.setLabel("Transmission T");
        probabilityAxis.setAutoRanging(false);
        probabilityAxis.setLowerBound(0.0);
        probabilityAxis.setUpperBound(1.0);
        probabilityAxis.setTickUnit(0.1);
        chart = new LineChart<>(energyAxis, probabilityAxis);
        chart.setTitle("Probability of getting through");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(340);
        chart.setMinHeight(260);
        chart.setStyle("CHART_COLOR_1: #1f6fb4; CHART_COLOR_2: #e8590c;");

        kind.getItems().setAll(TunnellingPresenter.BarrierKind.values());
        kind.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(TunnellingPresenter.BarrierKind k) {
                    if (k == null) {
                        return "";
                    }
                    return k == TunnellingPresenter.BarrierKind.SINGLE
                        ? "Single barrier"
                        : "Double barrier";
                }

                @Override
                public TunnellingPresenter.BarrierKind fromString(String text) {
                    return null; // the box is not editable
                }
            });
        kind.valueProperty().addListener((obs, old, k) -> applyDefaults(k));
        kind.setValue(TunnellingPresenter.BarrierKind.SINGLE);

        table.setPlaceholder(new Label("No data"));

        root.setLeft(buildInputPane());
        root.setCenter(buildResultsPane());
        root.setBottom(buildFooter());

        onRun(); // never show an empty screen
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
        for (TextField f : new TextField[]{height, width, gap, maxEnergy, probeEnergy}) {
            f.setPrefColumnCount(10);
        }
        kind.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "Barriers", kind);
        addRow(form, 1, "Height V0", height);
        addRow(form, 2, "Width a", width);
        addRow(form, 3, "Gap", gap);
        addRow(form, 4, "Plot up to E", maxEnergy);
        addRow(form, 5, "Probe energy E", probeEnergy);

        GridPane probe = new GridPane();
        probe.setHgap(8);
        probe.setVgap(4);
        probe.getColumnConstraints().add(new ColumnConstraints(130));
        addRow(probe, 0, "Energy", probeEnergyOut);
        addRow(probe, 1, "Regime", probeRegimeOut);
        addRow(probe, 2, "Classical T", probeClassicalOut);
        addRow(probe, 3, "Quantum T", probeTransmissionOut);
        addRow(probe, 4, "Reflected R", probeReflectionOut);
        addRow(probe, 5, "Decay constant κ", probeDecayOut);
        addRow(probe, 6, "Thick-barrier estimate", probeEstimateOut);

        Label estimateNote =
            new Label("The estimate is only accurate when κa is much larger than 1.");
        estimateNote.setWrapText(true);
        estimateNote.setMaxWidth(320);

        setupOut.setWrapText(true);
        setupOut.setMaxWidth(320);

        VBox box =
            new VBox(
                10,
                heading("Barrier"),
                form,
                button("Compute", this::onRun),
                heading("Last computed"),
                setupOut,
                heading("At the probe energy"),
                probe,
                estimateNote);
        box.setPadding(new Insets(16));
        box.setPrefWidth(380);
        return box;
    }

    private Node buildResultsPane() {
        peakOut.setWrapText(true);
        noteOut.setWrapText(true);

        table.getColumns().add(column("Energy", 100, TunnellingPresenter.RowView::fraction));
        table.getColumns().add(column("E", 70, TunnellingPresenter.RowView::energy));
        table.getColumns().add(column("Regime", 130, TunnellingPresenter.RowView::regime));
        table.getColumns().add(column("Classical T", 90, TunnellingPresenter.RowView::classical));
        table.getColumns().add(column("Quantum T", 100, TunnellingPresenter.RowView::transmission));
        table.getColumns().add(column("Reflected R", 100, TunnellingPresenter.RowView::reflection));
        table.getColumns().add(column("Closed form T", 110, TunnellingPresenter.RowView::closedForm));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Tables.fitToRows(table);

        VBox box =
            new VBox(
                10,
                heading("Classical against quantum"),
                table,
                noteOut,
                peakOut,
                chart,
                new ExplanationPane(ExplanationText.forTunnellingScreen()).node());
        box.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private Node buildFooter() {
        Label scope = new Label(ScopeNote.forTunnellingScreen());
        scope.setWrapText(true);
        VBox box = new VBox(4, statusOut, scope);
        box.setPadding(new Insets(8, 16, 12, 16));
        return box;
    }

    // ---------------------------------------------------------------- actions

    private void onRun() {
        TunnellingPresenter.Request request =
            new TunnellingPresenter.Request(
                kind.getValue(),
                height.getText(),
                width.getText(),
                gap.getText(),
                maxEnergy.getText(),
                probeEnergy.getText());
        run(
            () -> {
                TunnellingPresenter.Result result = presenter.compute(request);
                return () -> {
                    show(result);
                    setStatus("Computed T(E) at " + result.curve().size() + " energies.", false);
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
     * Typical settings for each arrangement; the user can change them afterwards.
     */
    private void applyDefaults(TunnellingPresenter.BarrierKind k) {
        boolean single = k == TunnellingPresenter.BarrierKind.SINGLE;
        height.setText(single ? "2" : "5");
        width.setText(single ? "1.5" : "0.5");
        gap.setText("1");
        maxEnergy.setText(single ? "4" : "8");
        probeEnergy.setText(single ? "1" : "2");
        gap.setDisable(single);
    }

    private void show(TunnellingPresenter.Result r) {
        setupOut.setText(r.setup());
        TunnellingPresenter.ProbeView p = r.probeView();
        probeEnergyOut.setText(p.energy());
        probeRegimeOut.setText(p.regime());
        probeClassicalOut.setText(p.classical());
        probeTransmissionOut.setText(p.transmission());
        probeReflectionOut.setText(p.reflection());
        probeDecayOut.setText(p.decayConstant());
        probeEstimateOut.setText(p.thickEstimate());
        noteOut.setText(r.note());
        peakOut.setText(r.peak());
        table.getItems().setAll(r.rows());

        double last = r.curve().get(r.curve().size() - 1).x();
        energyAxis.setLowerBound(0.0);
        energyAxis.setUpperBound(last);
        energyAxis.setTickUnit(last / 10.0);

        chart.getData().clear();
        chart.getData().add(series("Quantum T(E)", r.curve()));
        if (!r.barrierTop().isEmpty()) {
            chart.getData().add(series("Barrier top V0", r.barrierTop()));
        }
        if (!r.probe().isEmpty()) {
            chart.getData().add(series("Probe energy", r.probe()));
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
