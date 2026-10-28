package dev.quantumlab.desktop;

import dev.quantumlab.simulation.SeriesPoint;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

/**
 * Two charts: observed versus theoretical probability per outcome, and the running frequency of
 * the first outcome converging on its theoretical value as trials accumulate.
 *
 * <p>Must be used on the JavaFX thread only.
 */
final class ChartsPane {

    private final BarChart<String, Number> bars;
    private final LineChart<Number, Number> convergence;
    private final HBox box;

    ChartsPane() {
        bars = new BarChart<>(new CategoryAxis(), percentAxis());
        bars.setTitle("Observed vs theory");
        bars.setAnimated(false);

        NumberAxis trialsAxis = new NumberAxis();
        trialsAxis.setLabel("Trials");
        trialsAxis.setForceZeroInRange(false);
        convergence = new LineChart<>(trialsAxis, percentAxis());
        convergence.setTitle("Convergence of the running frequency");
        convergence.setAnimated(false);
        convergence.setCreateSymbols(false);

        for (XYChart<?, ?> chart : new XYChart<?, ?>[] {bars, convergence}) {
            chart.setPrefHeight(240);
            chart.setMinHeight(200);
            HBox.setHgrow(chart, Priority.ALWAYS);
        }
        box = new HBox(10, bars, convergence);
    }

    Node node() {
        return box;
    }

    void show(ChartData d) {
        XYChart.Series<String, Number> observed = new XYChart.Series<>();
        observed.setName("Observed");
        XYChart.Series<String, Number> theory = new XYChart.Series<>();
        theory.setName("Theory");
        for (int k = 0; k < d.labels().size(); k++) {
            observed.getData().add(new XYChart.Data<>(d.labels().get(k), d.observed().get(k)));
            theory.getData().add(new XYChart.Data<>(d.labels().get(k), d.theoretical().get(k)));
        }
        bars.getData().clear();
        bars.getData().add(observed);
        bars.getData().add(theory);

        convergence.getData().clear();
        if (!d.convergence().isEmpty()) {
            XYChart.Series<Number, Number> running = new XYChart.Series<>();
            running.setName("Observed frequency of " + d.convergenceLabel());
            for (SeriesPoint p : d.convergence()) {
                running.getData().add(new XYChart.Data<>(p.x(), p.y()));
            }
            double first = d.convergence().get(0).x();
            double last = d.convergence().get(d.convergence().size() - 1).x();
            XYChart.Series<Number, Number> expected = new XYChart.Series<>();
            expected.setName("Theory");
            expected.getData().add(new XYChart.Data<>(first, d.convergenceTheory()));
            expected.getData().add(new XYChart.Data<>(last, d.convergenceTheory()));
            convergence.getData().add(running);
            convergence.getData().add(expected);
        }
    }

    private static NumberAxis percentAxis() {
        NumberAxis axis = new NumberAxis(0.0, 1.0, 0.25);
        axis.setAutoRanging(false);
        axis.setTickLabelFormatter(
                new StringConverter<Number>() {
                    @Override
                    public String toString(Number n) {
                        return Math.round(n.doubleValue() * 100.0) + " %";
                    }

                    @Override
                    public Number fromString(String s) {
                        return null; // axis labels are never parsed
                    }
                });
        return axis;
    }
}
