package dev.quantumlab.desktop;

import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;

/** A collapsible panel explaining what the screen models and, equally, what it does not. */
final class ExplanationPane {

    private final TitledPane pane;

    /** The panel for the measurement screen. */
    ExplanationPane() {
        this(ExplanationText.forMeasurementScreen());
    }

    /** A panel with one tab per explanation, in the order given. */
    ExplanationPane(List<ModeExplanation> explanations) {
        TabPane tabs = new TabPane();
        for (ModeExplanation e : explanations) {
            TextArea text = new TextArea(ExplanationText.format(e));
            text.setEditable(false);
            text.setWrapText(true);
            Tab tab = new Tab(e.title(), text);
            tab.setClosable(false);
            tabs.getTabs().add(tab);
        }
        tabs.setPrefHeight(280);
        pane = new TitledPane("About this experiment: what it models and what it does not", tabs);
        pane.setExpanded(false);
    }

    Node node() {
        return pane;
    }
}
