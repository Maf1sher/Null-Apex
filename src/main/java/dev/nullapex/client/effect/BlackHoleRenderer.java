package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.effect.EffectTimeline;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Debug renderer for a spherical black-hole core and its animated accretion disk. */
final class BlackHoleRenderer implements ScreenEffectRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        NullApex.MOD_ID, "textures/effect/rune_circle.png"
    );
    private static final RenderType CORE_RENDER_TYPE = EffectRenderTypes.blackHoleCore(TEXTURE);
    private static final RenderType DISK_RENDER_TYPE = EffectRenderTypes.blackHoleDisk(TEXTURE);
    private static final RenderType SCREEN_MASK_RENDER_TYPE = EffectRenderTypes.screenMask(TEXTURE);
    private static final String CORE_PASS = "core";
    private static final String DISK_PASS = "disk";
    private static final int SPHERE_LATITUDE_SEGMENTS = 16;
    private static final int SPHERE_LONGITUDE_SEGMENTS = 32;
    private static final int DISK_SEGMENTS = 64;
    private static final float CORE_RADIUS_FACTOR = 0.38F;
    private static final float DISK_INNER_RADIUS_FACTOR = 0.40F;
    private static final List<EffectRenderPass> RENDER_PASSES = List.of(
        new EffectRenderPass(CORE_PASS, CORE_RENDER_TYPE),
        new EffectRenderPass(DISK_PASS, DISK_RENDER_TYPE)
    );

    @Override
    public List<EffectRenderPass> renderPasses(EffectRenderContext context) {
        return RENDER_PASSES;
    }

    @Override
    public void render(EffectRenderContext context, EffectRenderPass pass, PoseStack poseStack,
        MultiBufferSource bufferSource) {
        float scale = animatedScale(context);
        if (scale <= 0.0F) {
            return;
        }

        int shaderData = shaderData(context);
        switch (pass.id()) {
            case CORE_PASS -> renderCore(poseStack, bufferSource.getBuffer(CORE_RENDER_TYPE),
                scale * CORE_RADIUS_FACTOR, shaderData);
            case DISK_PASS -> renderDisk(poseStack, bufferSource.getBuffer(DISK_RENDER_TYPE), context,
                scale, EffectTimeline.fadeEnvelope(context.progress(), 0.08F, 0.12F), shaderData);
            default -> throw new IllegalArgumentException("Unknown black-hole render pass: " + pass.id());
        }
    }

    @Override
    public AABB cullingBounds(EffectRenderContext context) {
        double radius = context.scale() * 1.05;
        double x = context.transform().position().x();
        double y = context.transform().position().y();
        double z = context.transform().position().z();
        return new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }

    @Override
    public ScreenEffectSettings screenEffectSettings(EffectRenderContext context) {
        return ScreenEffectSettings.bloom(new BloomSettings(0.45F, 1.25F, 10.0F));
    }

    @Override
    public ScreenEffectMask screenEffectMask(EffectRenderContext context) {
        float strength = 0.82F * EffectTimeline.fadeEnvelope(context.progress(), 0.08F, 0.12F);
        return new ScreenEffectMask(1.0F, 1.0F, 1.0F, strength);
    }

    @Override
    public RenderType screenMaskRenderType(EffectRenderContext context) {
        return SCREEN_MASK_RENDER_TYPE;
    }

    @Override
    public void renderScreenMask(EffectRenderContext context, RenderType maskRenderType, PoseStack poseStack,
        MultiBufferSource bufferSource) {
        float scale = animatedScale(context);
        if (scale <= 0.0F) {
            return;
        }

        float alpha = EffectTimeline.fadeEnvelope(context.progress(), 0.08F, 0.12F);
        VertexConsumer vertices = bufferSource.getBuffer(maskRenderType);
        renderAnnulus(poseStack, vertices, scale * DISK_INNER_RADIUS_FACTOR, scale * 0.60F, scale * 0.54F,
            context.ageTicks() * 0.018F, alpha, 0);
    }

    private static void renderCore(PoseStack poseStack, VertexConsumer vertices, float radius, int shaderData) {
        for (int latitude = 0; latitude < SPHERE_LATITUDE_SEGMENTS; latitude++) {
            float top = (float)latitude / SPHERE_LATITUDE_SEGMENTS;
            float bottom = (float)(latitude + 1) / SPHERE_LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < SPHERE_LONGITUDE_SEGMENTS; longitude++) {
                float left = (float)longitude / SPHERE_LONGITUDE_SEGMENTS;
                float right = (float)(longitude + 1) / SPHERE_LONGITUDE_SEGMENTS;
                addSphereVertex(vertices, poseStack.last(), radius, left, top, shaderData);
                addSphereVertex(vertices, poseStack.last(), radius, left, bottom, shaderData);
                addSphereVertex(vertices, poseStack.last(), radius, right, bottom, shaderData);
                addSphereVertex(vertices, poseStack.last(), radius, right, top, shaderData);
            }
        }
    }

    private static void addSphereVertex(VertexConsumer vertices, PoseStack.Pose pose, float radius,
        float longitude, float latitude, int shaderData) {
        double theta = Math.PI * latitude;
        double phi = Math.PI * 2.0 * longitude;
        float sinTheta = (float)Math.sin(theta);
        float normalX = sinTheta * (float)Math.cos(phi);
        float normalY = (float)Math.cos(theta);
        float normalZ = sinTheta * (float)Math.sin(phi);
        vertices.addVertex(pose, normalX * radius, normalY * radius, normalZ * radius)
            .setColor(0.004F, 0.001F, 0.012F, 1.0F)
            .setUv(longitude, latitude)
            .setOverlay(shaderData)
            .setLight(0x00F000F0)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    private static void renderDisk(PoseStack poseStack, VertexConsumer vertices, EffectRenderContext context,
        float scale, float alpha, int shaderData) {
        float rotation = context.ageTicks() * 0.018F;
        renderAnnulus(poseStack, vertices, scale * DISK_INNER_RADIUS_FACTOR, scale, scale,
            rotation, alpha, shaderData);
        renderAnnulus(poseStack, vertices, scale * 0.54F, scale * 0.78F, scale,
            -context.ageTicks() * 0.027F, alpha * 0.42F, shaderData);
    }

    private static void renderAnnulus(PoseStack poseStack, VertexConsumer vertices, float innerRadius,
        float outerRadius, float uvRadius, float rotation, float alpha, int shaderData) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(rotation));
        PoseStack.Pose pose = poseStack.last();
        for (int segment = 0; segment < DISK_SEGMENTS; segment++) {
            double startAngle = Math.PI * 2.0 * segment / DISK_SEGMENTS;
            double endAngle = Math.PI * 2.0 * (segment + 1) / DISK_SEGMENTS;
            addDiskVertex(vertices, pose, innerRadius, uvRadius, startAngle, alpha, shaderData);
            addDiskVertex(vertices, pose, outerRadius, uvRadius, startAngle, alpha, shaderData);
            addDiskVertex(vertices, pose, outerRadius, uvRadius, endAngle, alpha, shaderData);
            addDiskVertex(vertices, pose, innerRadius, uvRadius, endAngle, alpha, shaderData);
        }
        poseStack.popPose();
    }

    private static void addDiskVertex(VertexConsumer vertices, PoseStack.Pose pose, float radius,
        float uvRadius, double angle, float alpha, int shaderData) {
        float x = radius * (float)Math.cos(angle);
        float y = radius * (float)Math.sin(angle);
        float u = 0.5F + x / (2.0F * uvRadius);
        float v = 0.5F + y / (2.0F * uvRadius);
        vertices.addVertex(pose, x, y, 0.0F)
            .setColor(1.0F, 1.0F, 1.0F, alpha)
            .setUv(u, v)
            .setOverlay(shaderData)
            .setLight(0x00F000F0)
            .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static float animatedScale(EffectRenderContext context) {
        float progress = context.progress();
        float entrance = EffectTimeline.easeOutCubic(Math.min(1.0F, progress / 0.12F));
        float exitProgress = Math.max(0.0F, (progress - 0.88F) / 0.12F);
        float exit = 1.0F - EffectTimeline.easeOutCubic(exitProgress);
        return context.scale() * entrance * exit;
    }

    private static int shaderData(EffectRenderContext context) {
        int progress = Math.round(context.progress() * 32767.0F);
        int seed = Math.floorMod(Long.hashCode(context.seed()), 32768);
        return OverlayTexture.pack(progress, seed);
    }
}
