package dev.quantumlab.desktop;

import dev.quantumlab.simulation.QuantumLabInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Application entry point. Views are thin; all experiment logic lives in quantumlab-simulation. */
public final class QuantumLabApp extends Application {

    private MeasurementView view;

    @Override
    public void start(Stage stage) {
        view = new MeasurementView();
        stage.setTitle(QuantumLabInfo.NAME + " " + QuantumLabInfo.VERSION);
        stage.setScene(new Scene(view.root(), 1100, 720));
        stage.show();
    }

    @Override
    public void stop() {
        if (view != null) {
            view.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
