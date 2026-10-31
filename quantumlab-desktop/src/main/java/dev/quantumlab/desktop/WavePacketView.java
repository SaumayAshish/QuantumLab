package dev.quantumlab.desktop;

import dev.quantumlab.simulation.SeriesPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The wave-packet screen: a Gaussian packet runs into a rectangular barrier and the probability
 * density is played back frame by frame, next to the probability of being left of, inside, and
 * beyond the barrier against time.
 *
 * <p>This class only lays out controls, plays back stored frames and displays immutable views. All
 * logic is in {@link WavePacketPresenter}; the calculation runs on a worker thread, the playback on
 * the JavaFX thread.
 */
public final class WavePacketView {

    private static final String SCOPE_NOTE =
        "A single particle in one dimension, solved numerically. The animation shows the"
            + " probability density |ψ|², not a path. Nothing is measured and no collapse is"
            + " shown. It is a separate model from the cat experiment.";

    private static final double FRAME_MILLIS = 40.0;

    private final WavePacketPresenter presenter = new WavePacketPresenter();
    private final Worker worker = new Worker("quantumlab-wavepacket-worker");

    private final BorderPane root = new BorderPane();
    private final List<Button> controls = new ArrayList<>();

    // inputs
    private final TextField height = new TextField("2");
    private final TextField width = new TextField("1");
    private final TextField centre = new TextField("-20");
    private final TextField sigma = new TextField("3");
    private final TextField waveNumber = new TextField("1.5");

    // outputs
    private final Label setupOut = new Label("–");
    private final Label timeOut = new Label("–");
    private final Label leftOut = new Label("–");
    private final Label insideOut = new Label("–");
    private final Label rightOut = new Label("–");
    private final Label totalOut = new Label("–");
    private final Label theoryOut = new Label("–");
    private final Label atMeanOut = new Label("–");
    private final Label normOut = new Label("–");
    private final Label summaryOut = new Label();
    private final Label noteOut = new Label();
    private final Label statusOut = new Label();

    // playback
    private final Slider slider = new Slider(0, WavePacketPresenter.FRAMES - 1, 0);
    private final Button playButton = new Button("Play");
    private final Button restartButton = new Button("Restart");
    private final Timeline timeline = new Timeline();

    // charts
    private final NumberAxis positionAxis = new NumberAxis();
    private final NumberAxis densityAxis = new NumberAxis();
    private final LineChart<Number, Number> densityChart;
    private final NumberAxis timeAxis = new NumberAxis();
    private final NumberAxis probabilityAxis = new NumberAxis();
    private final LineChart<Number, Number> probabilityChart;

    private WavePacketPresenter.Result current;
    private List<XYChart.Data<Number, Number>> densityPoints = List.of();
    private final List<XYChart.Data<Number, Number>> nowMarker = new ArrayList<>();

    public WavePacketView() {
        positionAxis.setLabel("Position x");
        positionAxis.setAutoRanging(false);
        positionAxis.setLowerBound(-WavePacketPresenter.HALF_WIDTH);
        positionAxis.setUpperBound(WavePacketPresenter.HALF_WIDTH);
        positionAxis.setTickUnit(20.0);
        densityAxis.setLabel("Probability density |ψ|²");
        densityAxis.setAutoRanging(false);
        densityAxis.setLowerBound(0.0);
        densityAxis.setUpperBound(0.5);
        densityAxis.setTickUnit(0.1);
        densityChart = new LineChart<>(positionAxis, densityAxis);
        densityChart.setTitle("The wave packet and the barrier (barrier drawn schematically)");
        densityChart.setAnimated(false);
        densityChart.setCreateSymbols(false);
        densityChart.setPrefHeight(320);
        densityChart.setMinHeight(240);

        timeAxis.setLabel("Time t");
        timeAxis.setAutoRanging(false);
        timeAxis.setLowerBound(0.0);
        timeAxis.setUpperBound(1.0);
        probabilityAxis.setLabel("Probability");
        probabilityAxis.setAutoRanging(false);
        probabilityAxis.setLowerBound(0.0);
        probabilityAxis.setUpperBound(1.0);
        probabilityAxis.setTickUnit(0.1);
        probabilityChart = new LineChart<>(timeAxis, probabilityAxis);
        probabilityChart.setTitle("Where the probability is, against time");
        probabilityChart.setAnimated(false);
        probabilityChart.setCreateSymbols(false);
        probabilityChart.setPrefHeight(300);
        probabilityChart.setMinHeight(220);

        slider.setBlockIncrement(1.0);
        slider.setMajorTickUnit(30.0);
        slider.setMinorTickCount(5);
        slider.setShowTickMarks(true);
        slider.valueProperty().addListener((obs, old, v) -> showFrame((int) Math.round(v.doubleValue())));
        HBox.setHgrow(slider, Priority.ALWAYS);

        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(FRAME_MILLIS), event -> advance()));
        timeline.setCycleCount(Timeline.INDEFINITE);

        root.setLeft(buildInputPane());
        root.setCenter(buildResultsPane());
        root.setBottom(buildFooter());

        onRun(); // never show an empty screen
    }

    public Parent root() {
        return root;
    }

    /** Stops the playback and the worker thread. Call when the window closes. */
    public void shutdown() {
        timeline.stop();
        worker.shutdown();
    }

    // ---------------------------------------------------------------- layout

    private Node buildInputPane() {
        for (TextField f : new TextField[] {height, width, centre, sigma, waveNumber}) {
            f.setPrefColumnCount(8);
        }
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        addRow(form, 0, "Barrier height V0", height);
        addRow(form, 1, "Barrier width a", width);
        addRow(form, 2, "Packet centre x0", centre);
        addRow(form, 3, "Packet width σ", sigma);
        addRow(form, 4, "Wave number k0", waveNumber);

        Label rules =
            new Label(
                "The barrier starts at x = 0 and its width is rounded to 0.1. The packet must"
                    + " start entirely left of it (x0 + 6σ ≤ 0), and k0 × σ must be at"
                    + " least 2.5.");
        rules.setWrapText(true);
        rules.setMaxWidth(320);

        GridPane now = new GridPane();
        now.setHgap(8);
        now.setVgap(4);
        now.getColumnConstraints().add(new ColumnConstraints(170));
        addRow(now, 0, "Time", timeOut);
        addRow(now, 1, "Left of barrier", leftOut);
        addRow(now, 2, "Inside barrier", insideOut);
        addRow(now, 3, "Beyond barrier", rightOut);
        addRow(now, 4, "Total", totalOut);

        GridPane theory = new GridPane();
        theory.setHgap(8);
        theory.setVgap(4);
        theory.getColumnConstraints().add(new ColumnConstraints(170));
        addRow(theory, 0, "Stationary prediction", theoryOut);
        addRow(theory, 1, "T at the mean energy", atMeanOut);
        addRow(theory, 2, "Largest norm error", normOut);

        setupOut.setWrapText(true);
        setupOut.setMaxWidth(340);

        VBox box =
            new VBox(
                10,
                heading("Barrier and packet"),
                form,
                rules,
                button("Run", this::onRun),
                heading("Last computed"),
                setupOut,
                heading("At the current frame"),
                now,
                heading("Against stationary theory"),
                theory);
        box.setPadding(new Insets(16));
        box.setPrefWidth(380);
        return box;
    }

    private Node buildResultsPane() {
        summaryOut.setWrapText(true);
        noteOut.setWrapText(true);

        playButton.setOnAction(event -> togglePlay());
        restartButton.setOnAction(event -> restart());
        controls.add(playButton);
        controls.add(restartButton);
        HBox playback = new HBox(10, playButton, restartButton, slider);

        VBox box =
            new VBox(
                10,
                heading("Wave packet meeting a barrier"),
                densityChart,
                playback,
                probabilityChart,
                summaryOut,
                noteOut,
                new ExplanationPane(ExplanationText.forWavePacketScreen()).node());
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
        WavePacketPresenter.Request request =
            new WavePacketPresenter.Request(
                height.getText(),
                width.getText(),
                centre.getText(),
                sigma.getText(),
                waveNumber.getText());
        run(
            () -> {
                WavePacketPresenter.Result result = presenter.compute(request);
                return () -> {
                    show(result);
                    setStatus("Computed " + result.frames().size() + " frames.", false);
                };
            });
    }

    /**
     * Runs {@code work} on the worker thread. It returns the UI update to apply, which then runs on
     * the JavaFX thread. Controls are disabled while the work runs, and a failed run leaves the
     * previous result on screen.
     */
    private void run(Supplier<Runnable> work) {
        timeline.pause();
        playButton.setText("Play");
        worker.run(work, message -> setStatus(message, true), this::setBusy);
    }

    // ---------------------------------------------------------------- playback

    private void togglePlay() {
        if (current == null) {
            return;
        }
        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.pause();
            playButton.setText("Play");
        } else {
            if ((int) Math.round(slider.getValue()) >= WavePacketPresenter.FRAMES - 1) {
                slider.setValue(0);
            }
            timeline.play();
            playButton.setText("Pause");
        }
    }

    private void restart() {
        if (current == null) {
            return;
        }
        slider.setValue(0);
        timeline.play();
        playButton.setText("Pause");
    }

    private void advance() {
        int next = (int) Math.round(slider.getValue()) + 1;
        if (next >= WavePacketPresenter.FRAMES) {
            timeline.pause();
            playButton.setText("Play");
            return;
        }
        slider.setValue(next);
    }

    // ---------------------------------------------------------------- display

    private void show(WavePacketPresenter.Result r) {
        current = r;
        setupOut.setText(r.setup());
        theoryOut.setText(r.theoryAverage());
        atMeanOut.setText(r.theoryAtMeanEnergy());
        normOut.setText(r.normCheck());
        summaryOut.setText(r.summary());
        noteOut.setText(r.note());

        densityAxis.setLowerBound(0.0);
        densityAxis.setUpperBound(r.maxDensity() * 1.1);
        densityAxis.setTickUnit(r.maxDensity() * 1.1 / 5.0);

        densityPoints = new ArrayList<>(r.positions().length);
        double[] first = r.frames().get(0).density();
        XYChart.Series<Number, Number> packet = new XYChart.Series<>();
        packet.setName("Probability density |ψ|²");
        for (int j = 0; j < r.positions().length; j++) {
            XYChart.Data<Number, Number> d = new XYChart.Data<>(r.positions()[j], first[j]);
            densityPoints.add(d);
            packet.getData().add(d);
        }
        densityChart.getData().clear();
        densityChart.getData().add(packet);
        densityChart.getData().add(series("Barrier (schematic)", r.barrierOutline()));

        timeAxis.setLowerBound(0.0);
        timeAxis.setUpperBound(r.endTime());
        timeAxis.setTickUnit(r.endTime() / 6.0);
        nowMarker.clear();
        nowMarker.add(new XYChart.Data<>(0.0, 0.0));
        nowMarker.add(new XYChart.Data<>(0.0, 1.0));
        XYChart.Series<Number, Number> now = new XYChart.Series<>();
        now.setName("Current frame");
        now.getData().addAll(nowMarker);
        probabilityChart.getData().clear();
        probabilityChart.getData().add(series("Left of barrier", r.leftOverTime()));
        probabilityChart.getData().add(series("Inside barrier", r.insideOverTime()));
        probabilityChart.getData().add(series("Beyond barrier", r.rightOverTime()));
        probabilityChart.getData().add(series("Stationary prediction", r.theoryLine()));
        probabilityChart.getData().add(now);

        slider.setValue(0);
        showFrame(0);
        timeline.play();
        playButton.setText("Pause");
    }

    private void showFrame(int index) {
        if (current == null) {
            return;
        }
        int i = Math.max(0, Math.min(WavePacketPresenter.FRAMES - 1, index));
        WavePacketPresenter.Frame frame = current.frames().get(i);
        double[] density = frame.density();
        for (int j = 0; j < densityPoints.size(); j++) {
            densityPoints.get(j).setYValue(density[j]);
        }
        for (XYChart.Data<Number, Number> d : nowMarker) {
            d.setXValue(frame.time());
        }
        WavePacketPresenter.FrameText text = WavePacketPresenter.describe(frame);
        timeOut.setText(text.time());
        leftOut.setText(text.left());
        insideOut.setText(text.inside());
        rightOut.setText(text.right());
        totalOut.setText(text.total());
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
