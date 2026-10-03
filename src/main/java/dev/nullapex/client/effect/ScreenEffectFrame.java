package dev.nullapex.client.effect;

import java.util.Objects;

/** A visible, active screen-effect contribution prepared for the current render frame. */
record ScreenEffectFrame(EffectRenderContext context, ScreenEffectRenderer renderer, ScreenEffectMask mask) {
    ScreenEffectFrame {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(mask, "mask");
    }
}
