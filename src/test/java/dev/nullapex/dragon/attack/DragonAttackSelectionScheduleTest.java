package dev.nullapex.dragon.attack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.nullapex.dragon.fight.DragonFightPhase;
import org.junit.jupiter.api.Test;

class DragonAttackSelectionScheduleTest {
    @Test
    void initialAttackIsHeldUntilItsRandomizedDeadline() {
        DragonAttackSelectionSchedule schedule = new DragonAttackSelectionSchedule();

        assertFalse(schedule.isDue(DragonFightPhase.ESCALATION, 100L, () -> 225));
        assertFalse(schedule.isDue(DragonFightPhase.ESCALATION, 324L, () -> 999));
        assertTrue(schedule.isDue(DragonFightPhase.ESCALATION, 325L, () -> 999));
    }

    @Test
    void phaseChangeRecalculatesDeadlineFromLastSuccessfulAttack() {
        DragonAttackSelectionSchedule schedule = new DragonAttackSelectionSchedule();
        schedule.onAttackStarted(DragonFightPhase.ESCALATION, 100L, 285);

        assertFalse(schedule.isDue(DragonFightPhase.FINAL, 150L, () -> 205));
        assertFalse(schedule.isDue(DragonFightPhase.FINAL, 304L, () -> 999));
        assertTrue(schedule.isDue(DragonFightPhase.FINAL, 305L, () -> 999));
    }

    @Test
    void failedStartUsesRetryDelayAndCooldownCanDeferAnAttempt() {
        DragonAttackSelectionSchedule schedule = new DragonAttackSelectionSchedule();
        schedule.onAttackStarted(DragonFightPhase.FINAL, 100L, 205);
        schedule.deferUntil(350L);

        assertFalse(schedule.isDue(DragonFightPhase.FINAL, 349L, () -> 999));
        assertTrue(schedule.isDue(DragonFightPhase.FINAL, 350L, () -> 999));

        schedule.retryAt(350L, 20);
        assertFalse(schedule.isDue(DragonFightPhase.FINAL, 369L, () -> 999));
        assertTrue(schedule.isDue(DragonFightPhase.FINAL, 370L, () -> 999));
    }

    @Test
    void phaseWithNoEligibleAttacksWaitsForPhaseChange() {
        DragonAttackSelectionSchedule schedule = new DragonAttackSelectionSchedule();
        schedule.isDue(DragonFightPhase.OPENING, 100L, () -> 270);
        schedule.deferForPhaseChange();

        assertFalse(schedule.isDue(DragonFightPhase.OPENING, 1_000L, () -> 270));
        assertFalse(schedule.isDue(DragonFightPhase.ESCALATION, 1_000L, () -> 225));
        assertTrue(schedule.isDue(DragonFightPhase.ESCALATION, 1_225L, () -> 999));
    }
}
