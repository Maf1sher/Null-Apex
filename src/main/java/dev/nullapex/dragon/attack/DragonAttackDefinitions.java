package dev.nullapex.dragon.attack;

import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Set;

/** Temporary profiles used by operator-only lifecycle tests. */
public final class DragonAttackDefinitions {
    public static final DragonAttackDefinition LIFECYCLE_TEST = new DragonAttackDefinition(
        "lifecycle_test",
        Set.of(DragonFightPhase.OPENING, DragonFightPhase.ESCALATION, DragonFightPhase.FINAL),
        40,
        20,
        20,
        200
    );

    private DragonAttackDefinitions() {
    }
}
