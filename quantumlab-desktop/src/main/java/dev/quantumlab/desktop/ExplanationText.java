package dev.quantumlab.desktop;

import dev.quantumlab.simulation.ExplanationCatalog;
import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;

/**
 * Turns the tested {@link ExplanationCatalog} into display text. The limitations are rendered with
 * the same prominence as the capabilities, and every catalog line is shown verbatim, so the screen
 * cannot say less than the catalog does.
 */
public final class ExplanationText {

    static final String MODELS_HEADING = "What it models";
    static final String DOES_NOT_MODEL_HEADING = "What it does not model";
    private static final String NONE = "• (none stated)";

    private ExplanationText() {
    }

    /**
     * The explanations relevant to the one-dimensional Schrödinger screen, scope last.
     */
    public static List<ModeExplanation> forSchrodingerScreen() {
        return List.of(ExplanationCatalog.schrodinger1d(), ExplanationCatalog.general());
    }
    /** The explanations relevant to the tunnelling screen, scope last. */
    public static List<ModeExplanation> forTunnellingScreen() {
        return List.of(ExplanationCatalog.tunnelling(), ExplanationCatalog.general());
    }

    /**
     * The explanations relevant to the classical decay screen, most specific first, scope last.
     */
    public static List<ModeExplanation> forClassicalScreen() {
        return List.of(ExplanationCatalog.classical(), ExplanationCatalog.general());
    }

    /**
     * The explanations relevant to the measurement screen, most specific first, scope last.
     */
    public static List<ModeExplanation> forMeasurementScreen() {
        return List.of(
            ExplanationCatalog.measurement(),
            ExplanationCatalog.superposition(),
            ExplanationCatalog.general());
    }

    public static String format(ModeExplanation e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.title()).append("\n\n");
        sb.append(e.summary()).append("\n\n");
        sb.append("Mathematics\n").append(e.mathematics()).append("\n\n");
        appendSection(sb, MODELS_HEADING, e.represents());
        sb.append('\n');
        appendSection(sb, DOES_NOT_MODEL_HEADING, e.doesNotRepresent());
        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, String heading, List<String> items) {
        sb.append(heading).append('\n');
        if (items.isEmpty()) {
            sb.append(NONE).append('\n');
            return;
        }
        for (String item : items) {
            sb.append("• ").append(item).append('\n');
        }
    }
}
