package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.effect.EffectTimeline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Textured, layered sample renderer that also serves as a debug test for world-space VFX. */
final class RuneCircleRenderer implements VisualEffectRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        NullApex.MOD_ID, "textures/effect/rune_circle.png"
    );
    private static final RenderType RENDER_TYPE = EffectRenderTypes.visual(TEXTURE);

    @Override
    public void render(
        VisualEffectInstance effect,
        float ageTicks,
        float progress,
        PoseStack poseStack,
        MultiBufferSource bufferSource
    ) {
        float entrance = EffectTimeline.easeOutCubic(Math.min(1.0F, progress / 0.15F));
        float alpha = EffectTimeline.fadeEnvelope(progress, 0.10F, 0.25F);
        float worldScale = effect.payload().scale() * entrance;
        VertexConsumer vertices = bufferSource.getBuffer(RENDER_TYPE);

        renderLayer(poseStack, vertices, worldScale, ageTicks * 0.035F, alpha, 0.30F);
        renderLayer(poseStack, vertices, worldScale * 0.72F, -ageTicks * 0.052F, alpha, 0.20F);
        renderLayer(poseStack, vertices, worldScale * 0.43F, ageTicks * 0.075F, alpha, 0.10F);
    }

    @Override
    public RenderType renderType() {
        return RENDER_TYPE;
    }

    private static void renderLayer(
        PoseStack poseStack,
        VertexConsumer vertices,
        float scale,
        float rotation,
        float alpha,
        float height
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0, height, 0.0);
        poseStack.mulPose(Axis.YP.rotation(rotation));
        poseStack.scale(scale, 1.0F, scale);
        PoseStack.Pose pose = poseStack.last();
        vertices.addVertex(pose, -1.0F, 0.0F, -1.0F).setColor(0.45F, 0.92F, 1.0F, alpha)
            .setUv(0.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vertices.addVertex(pose, -1.0F, 0.0F, 1.0F).setColor(0.45F, 0.92F, 1.0F, alpha)
            .setUv(0.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vertices.addVertex(pose, 1.0F, 0.0F, 1.0F).setColor(0.45F, 0.92F, 1.0F, alpha)
            .setUv(1.0F, 1.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0).setNormal(pose, 0.0F, 1.0F, 0.0F);
        vertices.addVertex(pose, 1.0F, 0.0F, -1.0F).setColor(0.45F, 0.92F, 1.0F, alpha)
            .setUv(1.0F, 0.0F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0).setNormal(pose, 0.0F, 1.0F, 0.0F);
        poseStack.popPose();
    }
}
