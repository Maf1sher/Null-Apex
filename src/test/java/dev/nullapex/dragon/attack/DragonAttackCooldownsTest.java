package dev.nullapex.dragon.attack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DragonAttackCooldownsTest {
    @Test
    void cooldownRunsFromItsStartTickAndExpiresAtItsDeadline() {
        Map<String, Long> cooldowns = DragonAttackCooldowns.startCooldown(Map.of(), "swoop", 100L, 200);

        assertEquals(200L, DragonAttackCooldowns.remainingTicks(cooldowns, "swoop", 100L));
        assertEquals(1L, DragonAttackCooldowns.remainingTicks(cooldowns, "swoop", 299L));
        assertEquals(0L, DragonAttackCooldowns.remainingTicks(cooldowns, "swoop", 300L));
    }

    @Test
    void startingACooldownPrunesExpiredEntriesAndPreservesOtherActiveEntries() {
        Map<String, Long> existing = Map.of("expired", 90L, "active", 150L);

        Map<String, Long> updated = DragonAttackCooldowns.startCooldown(existing, "new", 100L, 20);

        assertEquals(Map.of("active", 150L, "new", 120L), updated);
    }
}
