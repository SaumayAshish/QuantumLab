package dev.quantumlab.desktop;

import dev.quantumlab.simulation.ModeExplanation;
import java.util.List;

/**
 * The short scope note shown in each screen's footer. It is built from the tested explanation
 * catalog, never typed into a view: the first limitation of the screen's own explanation, then the
 * first limitation of the general scope, both verbatim.
 */
public final class ScopeNote {

    static final String PREFIX = "Scope: ";

    private ScopeNote() {
    }

    /**
     * @param explanations the screen's explanations, most specific first and general scope last
     */
    public static String of(List<ModeExplanation> explanations) {
        if (explanations.isEmpty()) {
            throw new IllegalArgumentException("No explanations given");
        }
        StringBuilder sb = new StringBuilder(PREFIX).append(firstLimit(explanations.get(0)));
        if (explanations.size() > 1) {
            sb.append(' ').append(firstLimit(explanations.get(explanations.size() - 1)));
        }
        return sb.toString();
    }

    public static String forSchrodingerScreen() {
        return of(ExplanationText.forSchrodingerScreen());
    }

    public static String forTunnellingScreen() {
        return of(ExplanationText.forTunnellingScreen());
    }

    public static String forWavePacketScreen() {
        return of(ExplanationText.forWavePacketScreen());
    }

    public static String forCompositeScreen() {
        return of(ExplanationText.forCompositeScreen());
    }

    public static String forDecoherenceScreen() {
        return of(ExplanationText.forDecoherenceScreen());
    }

    public static String forInterferenceScreen() {
        return of(ExplanationText.forInterferenceScreen());
    }

    public static String forClassicalScreen() {
        return of(ExplanationText.forClassicalScreen());
    }

    public static String forMeasurementScreen() {
        return of(ExplanationText.forMeasurementScreen());
    }

    private static String firstLimit(ModeExplanation e) {
        if (e.doesNotRepresent().isEmpty()) {
            throw new IllegalArgumentException(e.title() + " states no limitation");
        }
        return e.doesNotRepresent().get(0);
    }
}
