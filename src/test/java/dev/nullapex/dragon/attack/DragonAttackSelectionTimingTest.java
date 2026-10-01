package dev.nullapex.dragon.attack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.nullapex.dragon.fight.DragonFightPhase;
import org.junit.jupiter.api.Test;

class DragonAttackSelectionTimingTest {
    @Test
    void attackIntervalsBecomeShorterInLaterFightPhases() {
        assertEquals(new DragonAttackSelectionTiming.TickRange(360, 480),
            DragonAttackSelectionTiming.attackInterval(DragonFightPhase.OPENING));
        assertEquals(new DragonAttackSelectionTiming.TickRange(300, 360),
            DragonAttackSelectionTiming.attackInterval(DragonFightPhase.ESCALATION));
        assertEquals(new DragonAttackSelectionTiming.TickRange(240, 300),
            DragonAttackSelectionTiming.attackInterval(DragonFightPhase.FINAL));
    }

    @Test
    void samplesBothInclusiveEndpointsAndUsesAShortRetryRange() {
        DragonAttackSelectionTiming.TickRange finalInterval =
            DragonAttackSelectionTiming.attackInterval(DragonFightPhase.FINAL);
        assertEquals(240, finalInterval.ticksForOffset(0));
        assertEquals(300, finalInterval.ticksForOffset(finalInterval.size() - 1));

        DragonAttackSelectionTiming.TickRange retry = DragonAttackSelectionTiming.retryInterval();
        assertEquals(20, retry.ticksForOffset(0));
        assertEquals(40, retry.ticksForOffset(retry.size() - 1));
    }
}
