package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Optional screen-space mask contribution for a visual-effect renderer. */
public interface ScreenEffectRenderer extends VisualEffectRenderer {
    /** Returns the per-instance compositor inputs for this effect. */
    ScreenEffectMask screenEffectMask(EffectRenderContext context);

    /** Returns a render type that writes coverage to the currently bound mask target. */
    RenderType screenMaskRenderType(EffectRenderContext context);

    /** Renders this effect's mask using the supplied mask-target render type. */
    void renderScreenMask(EffectRenderContext context, RenderType maskRenderType, PoseStack poseStack,
        MultiBufferSource bufferSource);
}
