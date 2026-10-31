package dev.quantumlab.desktop;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;

/**
 * One background thread for one screen. {@link #run} does the slow work off the JavaFX thread and
 * then applies the screen update on it, disabling the controls in between. Every screen used to
 * carry its own copy of this.
 *
 * <p>Call {@link #run} from the JavaFX thread only.
 */
final class Worker {

    private final ExecutorService executor;

    Worker(String threadName) {
        executor =
            Executors.newSingleThreadExecutor(
                task -> {
                    Thread t = new Thread(task, threadName);
                    t.setDaemon(true);
                    return t;
                });
    }

    /**
     * Runs {@code work} on the worker thread. It returns the UI update to apply, which then runs on
     * the JavaFX thread. {@code busy} is told {@code true} now and {@code false} after the update.
     * A failure becomes an error message passed to {@code showError}, and the previous result stays
     * on screen.
     */
    void run(Supplier<Runnable> work, Consumer<String> showError, Consumer<Boolean> busy) {
        busy.accept(true);
        executor.submit(
            () -> {
                Runnable ui;
                try {
                    ui = work.get();
                } catch (RuntimeException e) {
                    String message = FailureMessage.of(e);
                    ui = () -> showError.accept(message);
                }
                Runnable update = ui;
                Platform.runLater(
                    () -> {
                        update.run();
                        busy.accept(false);
                    });
            });
    }

    void shutdown() {
        executor.shutdownNow();
    }
}
