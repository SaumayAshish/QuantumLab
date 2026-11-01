package dev.quantumlab.desktop;

import dev.quantumlab.simulation.QuantumLabInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/**
 * Application entry point. Views are thin; all experiment logic lives in quantumlab-simulation.
 */
public final class QuantumLabApp extends Application {

    private ClassicalView classical;
    private MeasurementView measurement;
    private InterferenceView interference;
    private SchrodingerView schrodinger;
    private ConvergenceView convergence;
    private TunnellingView tunnelling;
    private WavePacketView wavePacket;
    private CompositeView composite;
    private DecoherenceView decoherence;

    @Override
    public void start(Stage stage) {
        classical = new ClassicalView();
        measurement = new MeasurementView();
        interference = new InterferenceView();
        schrodinger = new SchrodingerView();
        convergence = new ConvergenceView();
        tunnelling = new TunnellingView();
        wavePacket = new WavePacketView();
        composite = new CompositeView();
        decoherence = new DecoherenceView();

        Tab modeA = new Tab("A · Classical decay", classical.root());
        modeA.setClosable(false);
        Tab modeBC = new Tab("B/C · Superposition and measurement", measurement.root());
        modeBC.setClosable(false);
        Tab fringe = new Tab("Interference", interference.root());
        fringe.setClosable(false);
        Tab well = new Tab("Quantum well (1D)", schrodinger.root());
        well.setClosable(false);
        Tab conv = new Tab("Convergence", convergence.root());
        conv.setClosable(false);
        Tab tunnel = new Tab("Tunnelling", tunnelling.root());
        tunnel.setClosable(false);
        Tab packet = new Tab("Wave packet", wavePacket.root());
        packet.setClosable(false);
        Tab modeD = new Tab("D · Composite system", composite.root());
        modeD.setClosable(false);
        Tab modeE = new Tab("E · Decoherence", decoherence.root());
        modeE.setClosable(false);

        stage.setTitle(QuantumLabInfo.NAME + " " + QuantumLabInfo.VERSION);
        stage.setScene(
            new Scene(
                new TabPane(modeA, modeBC, fringe, well, conv, tunnel, packet, modeD, modeE),
                1100,
                760));
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
        if (interference != null) {
            interference.shutdown();
        }
        if (schrodinger != null) {
            schrodinger.shutdown();
        }
        if (convergence != null) {
            convergence.shutdown();
        }
        if (tunnelling != null) {
            tunnelling.shutdown();
        }
        if (wavePacket != null) {
            wavePacket.shutdown();
        }
        if (composite != null) {
            composite.shutdown();
        }
        if (decoherence != null) {
            decoherence.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
