package dev.nullapex.dragon.attack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Utilities for cooldown deadlines stored against the server world's game time. */
public final class DragonAttackCooldowns {
    private DragonAttackCooldowns() {
    }

    public static long remainingTicks(Map<String, Long> deadlines, String attackId, long gameTime) {
        Objects.requireNonNull(deadlines, "deadlines");
        Objects.requireNonNull(attackId, "attackId");
        long deadline = deadlines.getOrDefault(attackId, Long.MIN_VALUE);
        return deadline <= gameTime ? 0L : deadline - gameTime;
    }

    public static Map<String, Long> startCooldown(
        Map<String, Long> deadlines,
        String attackId,
        long gameTime,
        int cooldownTicks
    ) {
        Objects.requireNonNull(deadlines, "deadlines");
        Objects.requireNonNull(attackId, "attackId");
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("Cooldown must not be negative");
        }

        Map<String, Long> updated = new HashMap<>();
        deadlines.forEach((id, deadline) -> {
            if (deadline > gameTime && !id.equals(attackId)) {
                updated.put(id, deadline);
            }
        });
        if (cooldownTicks > 0) {
            updated.put(attackId, Math.addExact(gameTime, cooldownTicks));
        }
        return Map.copyOf(updated);
    }
}
