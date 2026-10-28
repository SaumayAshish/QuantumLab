package dev.quantumlab.desktop;

import dev.quantumlab.simulation.QuantumLabInfo;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Application entry point. Views are thin; all experiment logic lives in quantumlab-simulation. */
public final class QuantumLabApp extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label(QuantumLabInfo.NAME);
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label tagline = new Label(QuantumLabInfo.TAGLINE);
        tagline.setWrapText(true);

        Label runtime =
                new Label(
                        "Version " + QuantumLabInfo.VERSION
                                + "  |  Java " + System.getProperty("java.version")
                                + "  |  JavaFX " + System.getProperty("javafx.version"));

        VBox root = new VBox(10, title, tagline, runtime);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));

        stage.setTitle(QuantumLabInfo.NAME);
        stage.setScene(new Scene(root, 720, 360));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
