package dev.nullapex.dragon.attack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DragonAttackSelectorTest {
    @Test
    void filtersCandidatesByFightPhaseAndCooldown() {
        DragonAttackDefinition opening = definition("opening", Set.of(DragonFightPhase.OPENING));
        DragonAttackDefinition ready = definition("ready", Set.of(DragonFightPhase.ESCALATION, DragonFightPhase.FINAL));
        DragonAttackDefinition coolingDown = definition("cooling", Set.of(DragonFightPhase.ESCALATION));
        DragonAttackDefinition expiredCooldown = definition("expired", Set.of(DragonFightPhase.ESCALATION));

        List<DragonAttackDefinition> phaseEligible = DragonAttackSelector.phaseEligibleAttackDefinitions(
            List.of(opening, ready, coolingDown, expiredCooldown), DragonFightPhase.ESCALATION
        );
        List<DragonAttackDefinition> eligible = DragonAttackSelector.eligibleAttackDefinitions(
            phaseEligible, Map.of("cooling", 150L, "expired", 90L), 100L
        );

        assertEquals(Set.of("ready", "cooling", "expired"), phaseEligible.stream()
            .map(DragonAttackDefinition::id)
            .collect(Collectors.toSet()));
        assertEquals(Set.of("ready", "expired"), eligible.stream()
            .map(DragonAttackDefinition::id)
            .collect(Collectors.toSet()));
    }

    private static DragonAttackDefinition definition(String id, Set<DragonFightPhase> allowedPhases) {
        return new DragonAttackDefinition(id, allowedPhases, 1, 1, 1, 0);
    }
}
