package dev.nullapex.dragon.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HealthThresholdPhasePolicyTest {
    private final HealthThresholdPhasePolicy policy = new HealthThresholdPhasePolicy();

    @Test
    void advancesAtInclusiveHealthThresholds() {
        assertEquals(
            DragonFightPhase.ESCALATION,
            this.policy.nextPhase(DragonFightPhase.OPENING, new DragonFightProgressionMetrics(0.70))
        );
        assertEquals(
            DragonFightPhase.FINAL,
            this.policy.nextPhase(DragonFightPhase.ESCALATION, new DragonFightProgressionMetrics(0.35))
        );
    }

    @Test
    void canSkipToTheHighestPhaseReachedByOneDamageEvent() {
        assertEquals(
            DragonFightPhase.FINAL,
            this.policy.nextPhase(DragonFightPhase.OPENING, new DragonFightProgressionMetrics(0.20))
        );
    }

    @Test
    void healingDoesNotMoveTheFightBackToAnEarlierPhase() {
        assertEquals(
            DragonFightPhase.ESCALATION,
            this.policy.nextPhase(DragonFightPhase.ESCALATION, new DragonFightProgressionMetrics(0.95))
        );
        assertEquals(
            DragonFightPhase.FINAL,
            this.policy.nextPhase(DragonFightPhase.FINAL, new DragonFightProgressionMetrics(0.95))
        );
    }

    @Test
    void rejectsInvalidThresholdOrdering() {
        assertThrows(IllegalArgumentException.class, () -> new HealthThresholdPhasePolicy(0.35, 0.70));
    }

    @Test
    void clampsHealthRatioToItsValidRange() {
        assertEquals(1.0, new DragonFightProgressionMetrics(1.5).healthRatio());
        assertEquals(0.0, new DragonFightProgressionMetrics(-0.5).healthRatio());
        assertThrows(IllegalArgumentException.class, () -> new DragonFightProgressionMetrics(Double.NaN));
    }
}
