package dev.nullapex.client.effect;

import java.util.Objects;

/** Immutable per-effect selection of the operation applied by the screen compositor. */
public record ScreenEffectSettings(ScreenEffectOperation operation) {
    public static final ScreenEffectSettings DIAGNOSTIC_PREVIEW =
        new ScreenEffectSettings(ScreenEffectOperation.DIAGNOSTIC_MASK_PREVIEW);

    public ScreenEffectSettings {
        Objects.requireNonNull(operation, "operation");
    }
}
