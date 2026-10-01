package dev.nullapex.dragon.effect;

import java.util.UUID;

/** Server-side handle used to stop a transient client visual before its natural expiry. */
public record VisualEffectHandle(UUID instanceId) {
}
