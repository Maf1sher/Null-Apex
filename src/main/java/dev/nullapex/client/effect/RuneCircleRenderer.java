package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.effect.EffectTimeline;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Textured, layered sample renderer that also serves as a debug test for world-space VFX. */
final class RuneCircleRenderer implements VisualEffectRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        NullApex.MOD_ID, "textures/effect/rune_circle.png"
    );
    private static final RenderType RENDER_TYPE = EffectRenderTypes.visual(TEXTURE);
    private static final String OUTER_PASS = "outer";
    private static final String MIDDLE_PASS = "middle";
    private static final String INNER_PASS = "inner";
    private static final List<EffectRenderPass> RENDER_PASSES = List.of(
        new EffectRenderPass(OUTER_PASS, RENDER_TYPE),
        new EffectRenderPass(MIDDLE_PASS, RENDER_TYPE),
        new EffectRenderPass(INNER_PASS, RENDER_TYPE)
    );

    @Override
    public List<EffectRenderPass> renderPasses(EffectRenderContext context) {
        return RENDER_PASSES;
    }

    @Override
    public void render(
        EffectRenderContext context,
        EffectRenderPass pass,
        PoseStack poseStack,
        MultiBufferSource bufferSource
    ) {
        float entrance = EffectTimeline.easeOutCubic(Math.min(1.0F, context.progress() / 0.15F));
        float alpha = EffectTimeline.fadeEnvelope(context.progress(), 0.10F, 0.25F);
        float worldScale = context.scale() * entrance;
        VertexConsumer vertices = bufferSource.getBuffer(RENDER_TYPE);

        switch (pass.id()) {
            case OUTER_PASS -> renderLayer(poseStack, vertices, worldScale,
                context.ageTicks() * 0.035F, alpha, 0.30F);
            case MIDDLE_PASS -> renderLayer(poseStack, vertices, worldScale * 0.72F,
                -context.ageTicks() * 0.052F, alpha, 0.20F);
            case INNER_PASS -> renderLayer(poseStack, vertices, worldScale * 0.43F,
                context.ageTicks() * 0.075F, alpha, 0.10F);
            default -> throw new IllegalArgumentException("Unknown rune circle render pass: " + pass.id());
        }
    }

    @Override
    public AABB cullingBounds(EffectRenderContext context) {
        double maximumHeight = 0.30;
        double radius = Math.sqrt(2.0 * context.scale() * context.scale() + maximumHeight * maximumHeight);
        double x = context.transform().position().x();
        double y = context.transform().position().y();
        double z = context.transform().position().z();
        return new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
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
