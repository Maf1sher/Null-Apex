package dev.nullapex.dragon.attack;

import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Objects;
import java.util.Set;

/** Timing and phase eligibility for one dragon attack. */
public record DragonAttackDefinition(
    String id,
    Set<DragonFightPhase> allowedFightPhases,
    int windupTicks,
    int activeTicks,
    int recoveryTicks,
    int cooldownTicks
) {
    public DragonAttackDefinition {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Attack id must not be blank");
        }
        allowedFightPhases = Set.copyOf(Objects.requireNonNull(allowedFightPhases, "allowedFightPhases"));
        if (allowedFightPhases.isEmpty()) {
            throw new IllegalArgumentException("An attack must be allowed in at least one fight phase");
        }
        if (windupTicks <= 0 || activeTicks <= 0 || recoveryTicks <= 0) {
            throw new IllegalArgumentException("Attack lifecycle stage durations must be positive");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("Attack cooldown must not be negative");
        }
    }
}
