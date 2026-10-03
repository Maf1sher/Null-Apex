package dev.nullapex.client.effect;

import java.util.Objects;

/** Immutable per-effect selection of the operation applied by the screen compositor. */
public record ScreenEffectSettings(
    ScreenEffectOperation operation,
    WaveDistortionSettings waveDistortionSettings
) {
    public static final ScreenEffectSettings DIAGNOSTIC_PREVIEW =
        new ScreenEffectSettings(ScreenEffectOperation.DIAGNOSTIC_MASK_PREVIEW);

    public ScreenEffectSettings(ScreenEffectOperation operation) {
        this(operation, null);
    }

    public ScreenEffectSettings {
        Objects.requireNonNull(operation, "operation");
        if (operation == ScreenEffectOperation.MASK_SCOPED_WAVE_DISTORTION) {
            Objects.requireNonNull(waveDistortionSettings, "waveDistortionSettings");
        } else if (waveDistortionSettings != null) {
            throw new IllegalArgumentException("Wave settings only apply to wave distortion");
        }
    }

    public static ScreenEffectSettings waveDistortion(WaveDistortionSettings settings) {
        return new ScreenEffectSettings(ScreenEffectOperation.MASK_SCOPED_WAVE_DISTORTION,
            Objects.requireNonNull(settings, "settings"));
    }
}
