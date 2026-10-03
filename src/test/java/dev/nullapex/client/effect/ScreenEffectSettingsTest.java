package dev.nullapex.client.effect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScreenEffectSettingsTest {
    @Test
    void diagnosticPreviewSelectsTheExistingMaskOperation() {
        assertEquals(ScreenEffectOperation.DIAGNOSTIC_MASK_PREVIEW,
            ScreenEffectSettings.DIAGNOSTIC_PREVIEW.operation());
    }

    @Test
    void rejectsMissingOperation() {
        assertThrows(NullPointerException.class, () -> new ScreenEffectSettings(null));
    }

    @Test
    void waveDistortionRequiresItsSettings() {
        assertThrows(NullPointerException.class,
            () -> new ScreenEffectSettings(ScreenEffectOperation.MASK_SCOPED_WAVE_DISTORTION));
        assertEquals(ScreenEffectOperation.MASK_SCOPED_WAVE_DISTORTION,
            ScreenEffectSettings.waveDistortion(WaveDistortionSettings.DEBUG_DEFAULT).operation());
    }

    @Test
    void rejectsWaveSettingsForTheDiagnosticOperation() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenEffectSettings(
            ScreenEffectOperation.DIAGNOSTIC_MASK_PREVIEW, WaveDistortionSettings.DEBUG_DEFAULT));
    }

    @Test
    void sceneBlurRequiresItsSettings() {
        assertThrows(NullPointerException.class,
            () -> new ScreenEffectSettings(ScreenEffectOperation.MASK_SCOPED_SCENE_BLUR));
        assertDoesNotThrow(() -> ScreenEffectSettings.sceneBlur(SceneBlurSettings.DEBUG_DEFAULT));
    }

    @Test
    void rejectsSettingsBelongingToAnotherOperation() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenEffectSettings(
            ScreenEffectOperation.MASK_SCOPED_SCENE_BLUR,
            WaveDistortionSettings.DEBUG_DEFAULT,
            SceneBlurSettings.DEBUG_DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> new ScreenEffectSettings(
            ScreenEffectOperation.MASK_SCOPED_WAVE_DISTORTION,
            WaveDistortionSettings.DEBUG_DEFAULT,
            SceneBlurSettings.DEBUG_DEFAULT));
    }
}
