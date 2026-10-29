package dev.quantumlab.desktop;

import dev.quantumlab.simulation.QuantumLabInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/** Application entry point. Views are thin; all experiment logic lives in quantumlab-simulation. */
public final class QuantumLabApp extends Application {

    private ClassicalView classical;
    private MeasurementView measurement;

    @Override
    public void start(Stage stage) {
        classical = new ClassicalView();
        measurement = new MeasurementView();

        Tab modeA = new Tab("A · Classical decay", classical.root());
        modeA.setClosable(false);
        Tab modeBC = new Tab("B/C · Superposition and measurement", measurement.root());
        modeBC.setClosable(false);

        stage.setTitle(QuantumLabInfo.NAME + " " + QuantumLabInfo.VERSION);
        stage.setScene(new Scene(new TabPane(modeA, modeBC), 1100, 760));
        stage.show();
    }

    @Override
    public void stop() {
        if (classical != null) {
            classical.shutdown();
        }
        if (measurement != null) {
            measurement.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
