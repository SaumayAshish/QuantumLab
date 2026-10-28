package dev.quantumlab.physics;

import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Seeded random-number generation, in one place so that "reproducible" has a single definition.
 *
 * <p>QuantumLab uses {@link SplittableRandom} (the SplitMix64 algorithm). Its algorithm is part of
 * the JDK specification, so a given seed yields the same stream across JDK versions and platforms —
 * unlike {@code RandomGenerator.getDefault()}, whose algorithm may change between releases.
 * See ADR-0003.
 */
public final class Reproducibility {

    /** Recorded in exported experiments so a result can be traced to its generator. */
    public static final String RNG_ALGORITHM = "java.util.SplittableRandom (SplitMix64)";

    private Reproducibility() {}

    /** A new generator whose entire output stream is determined by {@code seed}. */
    public static RandomGenerator newGenerator(long seed) {
        return new SplittableRandom(seed);
    }

    /**
     * A seed for runs that did not request a fixed one. The seed is still recorded and displayed, so
     * even an "unseeded" run can be replayed exactly by entering the seed it reported.
     */
    public static long freshSeed() {
        return ThreadLocalRandom.current().nextLong();
    }
}
