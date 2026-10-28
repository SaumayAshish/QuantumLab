package dev.quantumlab.physics;

/**
 * The classical record of one run of the idealised apparatus: nucleus → detector → trigger → cat.
 *
 * <p>The compact constructor enforces that the four fields form a consistent causal chain (decay
 * ⇒ count ⇒ release ⇒ dead, and nothing otherwise), so an impossible combination such as "no
 * decay, but the cat is dead" cannot be represented.
 *
 * @param nucleusDecayed whether the nucleus decayed during the observation window
 * @param detector what the detector registered
 * @param trigger whether the mechanism was released
 * @param cat the cat's fate
 */
public record ApparatusOutcome(
        boolean nucleusDecayed, DetectorStatus detector, TriggerStatus trigger, CatState cat) {

    public ApparatusOutcome {
        boolean counted = detector == DetectorStatus.COUNT;
        boolean released = trigger == TriggerStatus.RELEASED;
        boolean dead = cat == CatState.DEAD;
        if (nucleusDecayed != counted || counted != released || released != dead) {
            throw new IllegalArgumentException(
                    "Inconsistent apparatus chain: decayed=" + nucleusDecayed + ", detector="
                            + detector + ", trigger=" + trigger + ", cat=" + cat);
        }
    }

    /** The apparatus before anything has happened: intact nucleus, quiet detector, armed, alive. */
    public static ApparatusOutcome initial() {
        return new ApparatusOutcome(
                false, DetectorStatus.NO_COUNT, TriggerStatus.ARMED, CatState.ALIVE);
    }

    /** The deterministic consequence chain triggered by the nucleus's fate. */
    public static ApparatusOutcome consequenceOf(boolean nucleusDecayed) {
        return nucleusDecayed
                ? new ApparatusOutcome(
                true, DetectorStatus.COUNT, TriggerStatus.RELEASED, CatState.DEAD)
                : initial();
    }
}
