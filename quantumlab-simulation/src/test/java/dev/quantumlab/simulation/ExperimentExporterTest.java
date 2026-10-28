package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.quantumlab.physics.QubitState;
import dev.quantumlab.physics.Reproducibility;
import dev.quantumlab.physics.StandardBasis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExperimentExporterTest {

    private static MeasurementSession ranSession(StandardBasis basis, long seed, int trials) {
        MeasurementSession s =
                new MeasurementSession(
                        QuantumMeasurementConfig.of(QubitState.fromBloch(1.2, 0.8), basis, seed));
        s.measure(trials);
        return s;
    }

    private static Map<String, Object> parse(String json) {
        return MiniJson.object(MiniJson.parse(json));
    }

    // ------------------------------------------------------------ measurement JSON

    @Test
    @DisplayName("the measurement export is well-formed JSON with schema and generator metadata")
    void wellFormedWithMetadata() {
        Map<String, Object> doc = parse(ExperimentExporter.toJson(ranSession(StandardBasis.HADAMARD, 9, 500)));
        assertEquals(ExperimentExporter.SCHEMA_MEASUREMENT, doc.get("schema"));
        Map<String, Object> gen = MiniJson.object(doc.get("generator"));
        assertEquals("QuantumLab", gen.get("software"));
        assertEquals(QuantumLabInfo.VERSION, gen.get("version"));
        assertEquals(Reproducibility.RNG_ALGORITHM, gen.get("rng"));
        assertNotNull(gen.get("java"));
    }

    @Test
    @DisplayName("the exported config reproduces the experiment: replaying it gives identical counts")
    void exportIsReplayable() {
        MeasurementSession original = ranSession(StandardBasis.CIRCULAR, 777, 20_000);
        Map<String, Object> doc = parse(ExperimentExporter.toJson(original));
        Map<String, Object> cfg = MiniJson.object(doc.get("config"));

        // Rebuild a config purely from the exported file...
        QuantumMeasurementConfig rebuilt =
                new QuantumMeasurementConfig(
                        MiniJson.number(MiniJson.object(cfg.get("alpha")).get("re")),
                        MiniJson.number(MiniJson.object(cfg.get("alpha")).get("im")),
                        MiniJson.number(MiniJson.object(cfg.get("beta")).get("re")),
                        MiniJson.number(MiniJson.object(cfg.get("beta")).get("im")),
                        StandardBasis.valueOf((String) cfg.get("basis")),
                        (long) MiniJson.number(cfg.get("seed")));
        assertEquals(original.config(), rebuilt);

        // ...and run it again: the tallies must match what was exported.
        MeasurementSession replay = new MeasurementSession(rebuilt);
        replay.measure((int) MiniJson.number(doc.get("trials")));
        List<Object> stats = MiniJson.array(doc.get("statistics"));
        for (int k = 0; k < 2; k++) {
            Map<String, Object> o = MiniJson.object(stats.get(k));
            assertEquals(MiniJson.number(o.get("count")), (double) replay.counts()[k], 0.0);
        }
    }

    @Test
    @DisplayName("seeds beyond double precision survive export (seed is written as an integer)")
    void largeSeedExactness() {
        long seed = 9_007_199_254_740_993L; // 2^53 + 1: not representable as a double
        String json = ExperimentExporter.toJson(ranSession(StandardBasis.COMPUTATIONAL, seed, 10));
        assertTrue(json.contains("\"seed\": " + seed), "seed must be written verbatim as an integer");
    }

    @Test
    @DisplayName("counts add up to the number of trials, and history matches the session")
    void countsAndHistory() {
        MeasurementSession s = ranSession(StandardBasis.COMPUTATIONAL, 5, 1234);
        Map<String, Object> doc = parse(ExperimentExporter.toJson(s));
        assertEquals(1234.0, MiniJson.number(doc.get("trials")), 0.0);

        List<Object> stats = MiniJson.array(doc.get("statistics"));
        double total = 0;
        for (Object o : stats) {
            total += MiniJson.number(MiniJson.object(o).get("count"));
        }
        assertEquals(1234.0, total, 0.0);

        List<Object> history = MiniJson.array(doc.get("history"));
        assertEquals(1234, history.size());
        assertEquals(Boolean.FALSE, doc.get("historyTruncated"));
        for (int i = 0; i < history.size(); i++) {
            Map<String, Object> h = MiniJson.object(history.get(i));
            assertEquals(i + 1.0, MiniJson.number(h.get("trial")), 0.0);
            assertEquals(s.outcomeAt(i), MiniJson.number(h.get("outcome")), 0.0);
        }
    }

    @Test
    @DisplayName("a very long history is truncated in JSON, but counts stay exact")
    void historyTruncation() {
        MeasurementSession s = ranSession(StandardBasis.COMPUTATIONAL, 5, ExperimentExporter.MAX_JSON_HISTORY + 500);
        Map<String, Object> doc = parse(ExperimentExporter.toJson(s));
        assertEquals(Boolean.TRUE, doc.get("historyTruncated"));
        assertEquals(ExperimentExporter.MAX_JSON_HISTORY, MiniJson.array(doc.get("history")).size());
        assertEquals(ExperimentExporter.MAX_JSON_HISTORY + 500.0, MiniJson.number(doc.get("trials")), 0.0);
    }

    @Test
    @DisplayName("relative phase is exported, and is null when it is undefined")
    void relativePhase() {
        MeasurementSession defined =
                new MeasurementSession(
                        QuantumMeasurementConfig.of(QubitState.plusI(), StandardBasis.COMPUTATIONAL, 1));
        Map<String, Object> d1 = MiniJson.object(parse(ExperimentExporter.toJson(defined)).get("preparedState"));
        assertEquals(Math.PI / 2, MiniJson.number(d1.get("relativePhaseRadians")), 1e-12);

        MeasurementSession undefined =
                new MeasurementSession(
                        QuantumMeasurementConfig.of(QubitState.basis0(), StandardBasis.COMPUTATIONAL, 1));
        Map<String, Object> d2 = MiniJson.object(parse(ExperimentExporter.toJson(undefined)).get("preparedState"));
        assertTrue(d2.containsKey("relativePhaseRadians"));
        assertNull(d2.get("relativePhaseRadians"));
    }

    @Test
    @DisplayName("statistics are exported with the theory they are compared against")
    void statisticsContent() {
        MeasurementSession s = ranSession(StandardBasis.HADAMARD, 3, 50_000);
        Map<String, Object> doc = parse(ExperimentExporter.toJson(s));
        List<Object> stats = MiniJson.array(doc.get("statistics"));
        double[] born = s.theoreticalProbabilities();
        for (int k = 0; k < 2; k++) {
            Map<String, Object> o = MiniJson.object(stats.get(k));
            assertEquals(s.basis().label(k), o.get("label"));
            assertEquals(born[k], MiniJson.number(o.get("theoreticalProbability")), 0.0);
            assertEquals(s.summary().outcomes().get(k).zScore(), MiniJson.number(o.get("zScore")), 0.0);
        }
        assertEquals(Boolean.TRUE, doc.get("withinFiveSigma"));
    }

    @Test
    @DisplayName("every export carries the scope notes")
    void scopeNotes() {
        Map<String, Object> doc = parse(ExperimentExporter.toJson(ranSession(StandardBasis.COMPUTATIONAL, 1, 5)));
        List<Object> notes = MiniJson.array(doc.get("scopeNotes"));
        assertFalse(notes.isEmpty());
        assertTrue(notes.stream().anyMatch(n -> ((String) n).contains("measurement problem")));
    }

    @Test
    @DisplayName("exporting the same session twice gives byte-identical output")
    void deterministicOutput() {
        MeasurementSession s = ranSession(StandardBasis.HADAMARD, 4, 300);
        assertEquals(ExperimentExporter.toJson(s), ExperimentExporter.toJson(s));
    }

    @Test
    @DisplayName("an empty session exports cleanly")
    void emptySession() {
        MeasurementSession s =
                new MeasurementSession(
                        QuantumMeasurementConfig.of(QubitState.plus(), StandardBasis.COMPUTATIONAL, 1));
        Map<String, Object> doc = parse(ExperimentExporter.toJson(s));
        assertEquals(0.0, MiniJson.number(doc.get("trials")), 0.0);
        assertTrue(MiniJson.array(doc.get("history")).isEmpty());
    }

    // ------------------------------------------------------------ classical JSON

    @Test
    @DisplayName("the classical export is well-formed and carries the decay law's parameters")
    void classicalExport() {
        ClassicalDecaySession s = new ClassicalDecaySession(new ClassicalDecayConfig(0.7, 1.0, 12));
        s.run(5000);
        Map<String, Object> doc = parse(ExperimentExporter.toJson(s));
        assertEquals(ExperimentExporter.SCHEMA_CLASSICAL, doc.get("schema"));
        Map<String, Object> cfg = MiniJson.object(doc.get("config"));
        assertEquals("classical-decay", cfg.get("mode"));
        assertEquals(0.7, MiniJson.number(cfg.get("decayRate")), 0.0);
        assertEquals(s.config().decayProbability(), MiniJson.number(cfg.get("decayProbability")), 0.0);
        assertEquals(5000.0, MiniJson.number(doc.get("trials")), 0.0);
    }

    @Test
    @DisplayName("a stable nucleus exports its infinite half-life as null, not as invalid JSON")
    void infiniteHalfLife() {
        ClassicalDecaySession s = new ClassicalDecaySession(new ClassicalDecayConfig(0.0, 1.0, 1));
        Map<String, Object> cfg = MiniJson.object(parse(ExperimentExporter.toJson(s)).get("config"));
        assertTrue(cfg.containsKey("halfLife"));
        assertNull(cfg.get("halfLife"));
    }

    // ------------------------------------------------------------ CSV and files

    @Test
    @DisplayName("CSV has a header and one row per trial, with apparatus columns only in the Z basis")
    void csv() {
        MeasurementSession z = ranSession(StandardBasis.COMPUTATIONAL, 5, 40);
        String[] zLines = ExperimentExporter.historyCsv(z).split("\n");
        assertEquals(41, zLines.length);
        assertEquals(
                "trial,outcome_index,outcome_label,theoretical_probability,detector,trigger,cat",
                zLines[0]);
        String[] row = zLines[1].split(",", -1);
        assertEquals(7, row.length);
        assertEquals("1", row[0]);
        assertTrue(row[4].equals("NO_COUNT") || row[4].equals("COUNT"));

        MeasurementSession x = ranSession(StandardBasis.HADAMARD, 5, 10);
        String[] xRow = ExperimentExporter.historyCsv(x).split("\n")[1].split(",", -1);
        assertEquals(7, xRow.length);
        assertEquals("", xRow[4]);
        assertEquals("", xRow[5]);
        assertEquals("", xRow[6]);
    }

    @Test
    @DisplayName("files are written as UTF-8 and read back unchanged (labels contain |0>, sigma, etc.)")
    void fileRoundTrip() throws IOException {
        Path dir = Files.createTempDirectory("quantumlab-test");
        try {
            Path file = dir.resolve("experiment.json");
            String json = ExperimentExporter.toJson(ranSession(StandardBasis.HADAMARD, 8, 100));
            ExperimentExporter.write(file, json);
            assertEquals(json, Files.readString(file, StandardCharsets.UTF_8));
            assertTrue(json.contains("|+⟩"));
            // Overwrites rather than appends.
            ExperimentExporter.write(file, "{}");
            assertEquals("{}", Files.readString(file, StandardCharsets.UTF_8));
        } finally {
            try (var files = Files.walk(dir)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }
}
