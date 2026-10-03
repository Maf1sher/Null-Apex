package dev.nullapex.client.effect;

import java.util.Objects;

/** Immutable per-effect selection of the operation applied by the screen compositor. */
public record ScreenEffectSettings(
    ScreenEffectOperation operation,
    WaveDistortionSettings waveDistortionSettings,
    SceneBlurSettings sceneBlurSettings,
    BloomSettings bloomSettings
) {
    public static final ScreenEffectSettings DIAGNOSTIC_PREVIEW =
        new ScreenEffectSettings(ScreenEffectOperation.DIAGNOSTIC_MASK_PREVIEW);

    public ScreenEffectSettings(ScreenEffectOperation operation) {
        this(operation, null, null, null);
    }

    public ScreenEffectSettings(ScreenEffectOperation operation, WaveDistortionSettings waveDistortionSettings) {
        this(operation, waveDistortionSettings, null, null);
    }

    public ScreenEffectSettings(ScreenEffectOperation operation, WaveDistortionSettings waveDistortionSettings,
        SceneBlurSettings sceneBlurSettings) {
        this(operation, waveDistortionSettings, sceneBlurSettings, null);
    }

    public ScreenEffectSettings {
        Objects.requireNonNull(operation, "operation");
        switch (operation) {
            case DIAGNOSTIC_MASK_PREVIEW -> {
                if (waveDistortionSettings != null || sceneBlurSettings != null || bloomSettings != null) {
                    throw new IllegalArgumentException("Diagnostic preview does not accept operation settings");
                }
            }
            case MASK_SCOPED_WAVE_DISTORTION -> {
                Objects.requireNonNull(waveDistortionSettings, "waveDistortionSettings");
                if (sceneBlurSettings != null || bloomSettings != null) {
                    throw new IllegalArgumentException("Settings only apply to their matching screen operation");
                }
            }
            case MASK_SCOPED_SCENE_BLUR -> {
                Objects.requireNonNull(sceneBlurSettings, "sceneBlurSettings");
                if (waveDistortionSettings != null || bloomSettings != null) {
                    throw new IllegalArgumentException("Settings only apply to their matching screen operation");
                }
            }
            case MASK_SCOPED_BLOOM -> {
                Objects.requireNonNull(bloomSettings, "bloomSettings");
                if (waveDistortionSettings != null || sceneBlurSettings != null) {
                    throw new IllegalArgumentException("Settings only apply to their matching screen operation");
                }
            }
        }
    }

    public static ScreenEffectSettings waveDistortion(WaveDistortionSettings settings) {
        return new ScreenEffectSettings(ScreenEffectOperation.MASK_SCOPED_WAVE_DISTORTION,
            Objects.requireNonNull(settings, "settings"), null);
    }

    public static ScreenEffectSettings sceneBlur(SceneBlurSettings settings) {
        return new ScreenEffectSettings(ScreenEffectOperation.MASK_SCOPED_SCENE_BLUR, null,
            Objects.requireNonNull(settings, "settings"));
    }

    public static ScreenEffectSettings bloom(BloomSettings settings) {
        return new ScreenEffectSettings(ScreenEffectOperation.MASK_SCOPED_BLOOM, null, null,
            Objects.requireNonNull(settings, "settings"));
    }
}
