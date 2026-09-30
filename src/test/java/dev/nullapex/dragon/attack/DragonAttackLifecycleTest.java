package dev.nullapex.dragon.attack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DragonAttackLifecycleTest {
    @Test
    void advancesThroughWindupActiveRecoveryAndIdleAtTheConfiguredTicks() {
        DragonAttackLifecycle lifecycle = new DragonAttackLifecycle();
        DragonAttackDefinition definition = definition(Set.of(DragonFightPhase.OPENING), 2, 3, 1);

        assertEquals(
            DragonAttackLifecycle.StartResult.STARTED,
            lifecycle.tryStart(definition, DragonFightPhase.OPENING)
        );
        assertEquals(DragonAttackStage.WINDUP, lifecycle.stage());
        assertEquals(2, lifecycle.remainingStageTicks());

        lifecycle.tick();
        assertEquals(DragonAttackStage.WINDUP, lifecycle.stage());
        assertEquals(1, lifecycle.remainingStageTicks());

        lifecycle.tick();
        assertEquals(DragonAttackStage.ACTIVE, lifecycle.stage());
        assertEquals(3, lifecycle.remainingStageTicks());

        lifecycle.tick();
        lifecycle.tick();
        lifecycle.tick();
        assertEquals(DragonAttackStage.RECOVERY, lifecycle.stage());
        assertEquals(1, lifecycle.remainingStageTicks());

        lifecycle.tick();
        assertEquals(DragonAttackStage.IDLE, lifecycle.stage());
        assertNull(lifecycle.activeAttackId());
        assertEquals(0, lifecycle.remainingStageTicks());
    }

    @Test
    void rejectsConcurrentStartsAndAttacksOutsideTheirAllowedFightPhases() {
        DragonAttackLifecycle lifecycle = new DragonAttackLifecycle();
        DragonAttackDefinition openingAttack = definition(Set.of(DragonFightPhase.OPENING), 1, 1, 1);

        assertEquals(
            DragonAttackLifecycle.StartResult.PHASE_NOT_ALLOWED,
            lifecycle.tryStart(openingAttack, DragonFightPhase.FINAL)
        );
        assertEquals(DragonAttackStage.IDLE, lifecycle.stage());

        assertEquals(
            DragonAttackLifecycle.StartResult.STARTED,
            lifecycle.tryStart(openingAttack, DragonFightPhase.OPENING)
        );
        assertEquals(
            DragonAttackLifecycle.StartResult.ALREADY_ACTIVE,
            lifecycle.tryStart(openingAttack, DragonFightPhase.OPENING)
        );
    }

    @Test
    void cancelReturnsTheLifecycleToIdle() {
        DragonAttackLifecycle lifecycle = new DragonAttackLifecycle();
        lifecycle.tryStart(definition(Set.of(DragonFightPhase.OPENING), 5, 5, 5), DragonFightPhase.OPENING);

        lifecycle.cancel();

        assertEquals(DragonAttackStage.IDLE, lifecycle.stage());
        assertNull(lifecycle.activeAttackId());
        assertEquals(0, lifecycle.remainingStageTicks());
    }

    private static DragonAttackDefinition definition(
        Set<DragonFightPhase> allowedPhases,
        int windupTicks,
        int activeTicks,
        int recoveryTicks
    ) {
        return new DragonAttackDefinition("test", allowedPhases, windupTicks, activeTicks, recoveryTicks, 0);
    }
}
