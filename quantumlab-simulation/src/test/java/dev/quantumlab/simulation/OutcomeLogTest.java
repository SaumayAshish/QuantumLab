package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutcomeLogTest {

    @Test
    @DisplayName("add, count, tally and outcomeAt agree (including past the initial capacity)")
    void basics() {
        OutcomeLog log = new OutcomeLog(2, 100_000);
        for (int i = 0; i < 5000; i++) {
            log.add(i % 3 == 0 ? 1 : 0);
        }
        assertEquals(5000, log.count());
        assertArrayEquals(new long[] {3333, 1667}, log.tallyCopy());
        assertEquals(1, log.outcomeAt(0));
        assertEquals(0, log.outcomeAt(1));
        assertEquals(1, log.outcomeAt(4998));
    }

    @Test
    @DisplayName("an out-of-range outcome is rejected")
    void outcomeRange() {
        OutcomeLog log = new OutcomeLog(2, 10);
        assertThrows(IllegalArgumentException.class, () -> log.add(2));
        assertThrows(IllegalArgumentException.class, () -> log.add(-1));
    }

    @Test
    @DisplayName("the capacity limit is enforced and clear() restores it")
    void capacity() {
        OutcomeLog log = new OutcomeLog(2, 3);
        log.add(0);
        log.add(1);
        log.add(0);
        assertEquals(0, log.remaining());
        assertThrows(IllegalStateException.class, () -> log.add(0));
        log.clear();
        assertEquals(3, log.remaining());
        assertEquals(0, log.count());
        assertArrayEquals(new long[] {0, 0}, log.tallyCopy());
    }

    @Test
    @DisplayName("outcomeAt validates its index")
    void indexRange() {
        OutcomeLog log = new OutcomeLog(2, 10);
        log.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> log.outcomeAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> log.outcomeAt(-1));
    }

    @Test
    @DisplayName("running frequency matches a hand calculation: 1,0,1,1 -> 1, 1/2, 2/3, 3/4")
    void runningFrequencyByHand() {
        OutcomeLog log = new OutcomeLog(2, 10);
        for (int o : new int[] {1, 0, 1, 1}) {
            log.add(o);
        }
        List<SeriesPoint> f = log.runningFrequency(1, 10);
        assertEquals(4, f.size());
        assertEquals(1.0, f.get(0).y(), 1e-15);
        assertEquals(0.5, f.get(1).y(), 1e-15);
        assertEquals(2.0 / 3.0, f.get(2).y(), 1e-15);
        assertEquals(0.75, f.get(3).y(), 1e-15);
        assertEquals(4.0, f.get(3).x(), 0.0);
    }

    @Test
    @DisplayName("sampleTargets: every n when few, otherwise geometric, increasing, ending at n")
    void sampleTargets() {
        assertArrayEquals(
                new long[] {1, 2, 3, 4, 5},
                java.util.Arrays.stream(OutcomeLog.sampleTargets(5, 10)).asLongStream().toArray());

        int[] t = OutcomeLog.sampleTargets(1_000_000, 200);
        assertEquals(1, t[0]);
        assertEquals(1_000_000, t[t.length - 1]);
        assertTrue(t.length <= 200);
        for (int i = 1; i < t.length; i++) {
            assertTrue(t[i] > t[i - 1], "not strictly increasing at " + i);
        }
        // Geometric spacing: many early points, so convergence from the noisy start is visible.
        int below1000 = 0;
        for (int x : t) {
            if (x <= 1000) {
                below1000++;
            }
        }
        assertTrue(below1000 >= 40, "expected the early trials to be well represented");
    }

    @Test
    @DisplayName("maxPoints below 2 is rejected")
    void maxPointsValidation() {
        OutcomeLog log = new OutcomeLog(2, 10);
        log.add(0);
        assertThrows(IllegalArgumentException.class, () -> log.runningFrequency(0, 1));
    }

    @Test
    @DisplayName("an outcome count outside 1..127 is rejected")
    void outcomeCountValidation() {
        assertThrows(IllegalArgumentException.class, () -> new OutcomeLog(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new OutcomeLog(200, 10));
    }
}
