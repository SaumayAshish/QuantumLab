package dev.quantumlab.simulation;

import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.Reproducibility;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Exports an experiment's configuration and results as JSON (full record) or CSV (trial history).
 *
 * <p>The JSON carries everything needed to <em>replay</em> the experiment: configuration, seed,
 * generator algorithm, and software version. Exports are plain UTF-8 text written without any
 * dependency (ADR-0004).
 *
 * <p>The schema is versioned ({@value #SCHEMA_MEASUREMENT}, {@value #SCHEMA_CLASSICAL}); fields
 * may be added in later versions, but existing fields keep their meaning within a version.
 */
public final class ExperimentExporter {

    public static final String SCHEMA_MEASUREMENT = "quantumlab.quantum-measurement/1";
    public static final String SCHEMA_CLASSICAL = "quantumlab.classical-decay/1";

    /** Upper bound on trials listed individually in JSON; counts and statistics are always exact. */
    public static final int MAX_JSON_HISTORY = 10_000;

    private ExperimentExporter() {}

    // ---------------------------------------------------------------- JSON

    public static String toJson(MeasurementSession session) {
        QuantumMeasurementConfig c = session.config();
        QubitState state = session.preparedState();
        StatisticalSummary summary = session.summary();

        JsonWriter w = new JsonWriter().startObject();
        w.key("schema").value(SCHEMA_MEASUREMENT);
        writeGenerator(w);

        w.key("config").startObject();
        w.key("mode").value(c.modeId());
        w.key("seed").value(c.seed());
        w.key("basis").value(c.basis().name());
        w.key("alpha").startObject().key("re").value(c.alphaRe()).key("im").value(c.alphaIm()).endObject();
        w.key("beta").startObject().key("re").value(c.betaRe()).key("im").value(c.betaIm()).endObject();
        w.endObject();

        w.key("preparedState").startObject();
        w.key("normSquared").value(state.normSquared());
        OptionalDouble phase = state.relativePhase();
        w.key("relativePhaseRadians");
        if (phase.isPresent()) {
            w.value(phase.getAsDouble());
        } else {
            w.nullValue();
        }
        w.endObject();

        w.key("basisLabels").startArray();
        for (String label : session.labels()) {
            w.value(label);
        }
        w.endArray();

        writeSummary(w, summary);

        int shown = Math.min(session.trialCount(), MAX_JSON_HISTORY);
        w.key("historyTruncated").value(session.trialCount() > shown);
        w.key("history").startArray();
        for (int i = 0; i < shown; i++) {
            TrialRecord t = session.trial(i);
            w.startObject();
            w.key("trial").value(t.trialNumber());
            w.key("outcome").value(t.outcomeIndex());
            w.key("label").value(t.outcomeLabel());
            w.endObject();
        }
        w.endArray();

        writeDisclaimer(w);
        return w.endObject().toString() + "\n";
    }

    public static String toJson(ClassicalDecaySession session) {
        ClassicalDecayConfig c = session.config();
        StatisticalSummary summary = session.summary();

        JsonWriter w = new JsonWriter().startObject();
        w.key("schema").value(SCHEMA_CLASSICAL);
        writeGenerator(w);

        w.key("config").startObject();
        w.key("mode").value(c.modeId());
        w.key("seed").value(c.seed());
        w.key("decayRate").value(c.decayRate());
        w.key("observationTime").value(c.observationTime());
        w.key("decayProbability").value(c.decayProbability());
        w.key("halfLife").value(c.model().halfLife());
        w.endObject();

        writeSummary(w, summary);
        writeDisclaimer(w);
        return w.endObject().toString() + "\n";
    }

    // ---------------------------------------------------------------- CSV

    /**
     * One row per trial: {@code trial,outcome_index,outcome_label,theoretical_probability,
     * detector,trigger,cat}. The last three columns are empty when the measurement basis has no
     * defined apparatus consequence.
     */
    public static String historyCsv(MeasurementSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("trial,outcome_index,outcome_label,theoretical_probability,detector,trigger,cat\n");
        for (int i = 0; i < session.trialCount(); i++) {
            TrialRecord t = session.trial(i);
            sb.append(t.trialNumber()).append(',');
            sb.append(t.outcomeIndex()).append(',');
            sb.append(csvField(t.outcomeLabel())).append(',');
            sb.append(Double.toString(t.theoreticalProbability())).append(',');
            if (t.apparatus().isPresent()) {
                sb.append(t.apparatus().get().detector()).append(',');
                sb.append(t.apparatus().get().trigger()).append(',');
                sb.append(t.apparatus().get().cat());
            } else {
                sb.append(",,");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- files

    /** Writes {@code content} to {@code file} as UTF-8, replacing any existing file. */
    public static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- internals

    private static void writeGenerator(JsonWriter w) {
        w.key("generator").startObject();
        w.key("software").value(QuantumLabInfo.NAME);
        w.key("version").value(QuantumLabInfo.VERSION);
        w.key("java").value(System.getProperty("java.version", "unknown"));
        w.key("rng").value(Reproducibility.RNG_ALGORITHM);
        w.endObject();
    }

    private static void writeSummary(JsonWriter w, StatisticalSummary s) {
        w.key("trials").value(s.trials());
        w.key("statistics").startArray();
        for (OutcomeStatistics o : s.outcomes()) {
            w.startObject();
            w.key("label").value(o.label());
            w.key("count").value(o.count());
            w.key("observedFrequency").value(o.observed());
            w.key("theoreticalProbability").value(o.theoretical());
            w.key("standardError").value(o.standardError());
            w.key("zScore").value(o.zScore());
            w.endObject();
        }
        w.endArray();
        w.key("withinFiveSigma").value(s.withinFiveSigma());
    }

    private static void writeDisclaimer(JsonWriter w) {
        List<String> notes = ExplanationCatalog.general().doesNotRepresent();
        w.key("scopeNotes").startArray();
        for (String n : notes) {
            w.value(n);
        }
        w.endArray();
    }

    private static String csvField(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
