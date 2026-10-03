package dev.nullapex.client.effect;

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
}
