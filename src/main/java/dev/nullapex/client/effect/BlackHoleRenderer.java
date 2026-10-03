package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.effect.EffectTimeline;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

/** Debug renderer for a cinematic, three-dimensional black hole and its plasma flows. */
final class BlackHoleRenderer implements ScreenEffectRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        NullApex.MOD_ID, "textures/effect/rune_circle.png"
    );
    private static final RenderType CORE_RENDER_TYPE = EffectRenderTypes.blackHoleCore(TEXTURE);
    private static final RenderType DISK_RENDER_TYPE = EffectRenderTypes.blackHoleDisk(TEXTURE);
    private static final RenderType SCREEN_MASK_RENDER_TYPE = EffectRenderTypes.blackHoleScreenMask(TEXTURE);
    private static final String CORE_PASS = "core";
    private static final String DISK_PASS = "disk";
    private static final int SPHERE_LATITUDE_SEGMENTS = 64;
    private static final int SPHERE_LONGITUDE_SEGMENTS = 128;
    private static final int DISK_ANGULAR_SEGMENTS = 128;
    private static final int DISK_RADIAL_SEGMENTS = 12;
    private static final float CORE_RADIUS_FACTOR = 0.38F;
    private static final float DISK_INNER_RADIUS_FACTOR = 0.42F;
    private static final float DISK_RADIUS_FACTOR = 1.25F;
    private static final BlackHoleScreenSettings SCREEN_SETTINGS = BlackHoleScreenSettings.CINEMATIC_DEFAULT;
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
                scale, EffectTimeline.fadeEnvelope(context.progress(), 0.08F, 0.12F),
                BlackHoleLighting.emissionScale(daylightFactor(context)), shaderData);
            default -> throw new IllegalArgumentException("Unknown black-hole render pass: " + pass.id());
        }
    }

    @Override
    public AABB cullingBounds(EffectRenderContext context) {
        double radius = context.scale() * 1.55;
        double x = context.transform().position().x();
        double y = context.transform().position().y();
        double z = context.transform().position().z();
        return new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }

    @Override
    public ScreenEffectSettings screenEffectSettings(EffectRenderContext context) {
        BlackHoleScreenSettings adaptedSettings = BlackHoleLighting.screenSettings(
            SCREEN_SETTINGS, daylightFactor(context));
        return ScreenEffectSettings.blackHole(adaptedSettings);
    }

    @Override
    public ScreenEffectMask screenEffectMask(EffectRenderContext context) {
        float strength = 0.96F * EffectTimeline.fadeEnvelope(context.progress(), 0.08F, 0.12F);
        return new ScreenEffectMask(1.0F, 1.0F, 1.0F, strength);
    }

    @Override
    public RenderType screenMaskRenderType(EffectRenderContext context) {
        return SCREEN_MASK_RENDER_TYPE;
    }

    @Override
    public void renderScreenMask(EffectRenderContext context, RenderType maskRenderType, PoseStack poseStack,
        MultiBufferSource bufferSource) {
        float alpha = EffectTimeline.fadeEnvelope(context.progress(), 0.08F, 0.12F);
        renderInfluenceMask(poseStack, bufferSource.getBuffer(maskRenderType),
            context.scale() * SCREEN_SETTINGS.lensRadiusScale(), alpha);
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

    private static void renderInfluenceMask(PoseStack poseStack, VertexConsumer vertices, float radius, float alpha) {
        for (int latitude = 0; latitude < SPHERE_LATITUDE_SEGMENTS; latitude++) {
            float top = (float)latitude / SPHERE_LATITUDE_SEGMENTS;
            float bottom = (float)(latitude + 1) / SPHERE_LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < SPHERE_LONGITUDE_SEGMENTS; longitude++) {
                float left = (float)longitude / SPHERE_LONGITUDE_SEGMENTS;
                float right = (float)(longitude + 1) / SPHERE_LONGITUDE_SEGMENTS;
                addMaskSphereVertex(vertices, poseStack.last(), radius, left, top, alpha);
                addMaskSphereVertex(vertices, poseStack.last(), radius, left, bottom, alpha);
                addMaskSphereVertex(vertices, poseStack.last(), radius, right, bottom, alpha);
                addMaskSphereVertex(vertices, poseStack.last(), radius, right, top, alpha);
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
            .setColor(0.0F, 0.0F, 0.0F, 1.0F)
            .setUv(longitude, latitude)
            .setOverlay(shaderData)
            .setLight(0x00F000F0)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    private static void addMaskSphereVertex(VertexConsumer vertices, PoseStack.Pose pose, float radius,
        float longitude, float latitude, float alpha) {
        double theta = Math.PI * latitude;
        double phi = Math.PI * 2.0 * longitude;
        float sinTheta = (float)Math.sin(theta);
        float normalX = sinTheta * (float)Math.cos(phi);
        float normalY = (float)Math.cos(theta);
        float normalZ = sinTheta * (float)Math.sin(phi);
        vertices.addVertex(pose, normalX * radius, normalY * radius, normalZ * radius)
            .setColor(1.0F, 1.0F, 1.0F, alpha)
            .setUv(longitude, latitude)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    private static void renderDisk(PoseStack poseStack, VertexConsumer vertices, EffectRenderContext context,
        float scale, float alpha, float emissionScale, int shaderData) {
        float rotation = context.ageTicks() * 0.018F;
        float diskScale = scale * DISK_RADIUS_FACTOR;
        renderAccretionLayer(poseStack, vertices, diskScale, scale * DISK_INNER_RADIUS_FACTOR, diskScale,
            rotation, diskScale * 0.035F, diskScale * 0.035F, alpha, emissionScale, shaderData);
        renderAccretionLayer(poseStack, vertices, diskScale, diskScale * 0.54F, diskScale * 0.94F,
            -context.ageTicks() * 0.027F, diskScale * 0.06F, diskScale * 0.055F,
            alpha * 0.44F, emissionScale, shaderData);
    }

    private static void renderAccretionLayer(PoseStack poseStack, VertexConsumer vertices, float scale,
        float innerRadius, float outerRadius, float rotation, float halfThickness, float warpAmplitude,
        float alpha, float emissionScale, int shaderData) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(rotation));
        PoseStack.Pose pose = poseStack.last();
        for (int radial = 0; radial < DISK_RADIAL_SEGMENTS; radial++) {
            float radialStart = (float)radial / DISK_RADIAL_SEGMENTS;
            float radialEnd = (float)(radial + 1) / DISK_RADIAL_SEGMENTS;
            float radius0 = innerRadius + (outerRadius - innerRadius) * radialStart;
            float radius1 = innerRadius + (outerRadius - innerRadius) * radialEnd;
            for (int segment = 0; segment < DISK_ANGULAR_SEGMENTS; segment++) {
                double angle0 = Math.PI * 2.0 * segment / DISK_ANGULAR_SEGMENTS;
                double angle1 = Math.PI * 2.0 * (segment + 1) / DISK_ANGULAR_SEGMENTS;
                float warp00 = diskWarp(radius0, angle0, scale, warpAmplitude);
                float warp10 = diskWarp(radius1, angle0, scale, warpAmplitude);
                float warp11 = diskWarp(radius1, angle1, scale, warpAmplitude);
                float warp01 = diskWarp(radius0, angle1, scale, warpAmplitude);
                float thickness0 = halfThickness * (0.35F + 0.65F * radius0 / scale);
                float thickness1 = halfThickness * (0.35F + 0.65F * radius1 / scale);

                addDiskQuad(vertices, pose, scale,
                    radius0, angle0, warp00 + thickness0,
                    radius1, angle0, warp10 + thickness1,
                    radius1, angle1, warp11 + thickness1,
                    radius0, angle1, warp01 + thickness0,
                    alpha, emissionScale, shaderData);
                addDiskQuad(vertices, pose, scale,
                    radius0, angle1, warp01 - thickness0,
                    radius1, angle1, warp11 - thickness1,
                    radius1, angle0, warp10 - thickness1,
                    radius0, angle0, warp00 - thickness0,
                    alpha * 0.72F, emissionScale, shaderData);
                if (radial == DISK_RADIAL_SEGMENTS - 1) {
                    addDiskQuad(vertices, pose, scale,
                        radius1, angle0, warp10 + thickness1,
                        radius1, angle0, warp10 - thickness1,
                        radius1, angle1, warp11 - thickness1,
                        radius1, angle1, warp11 + thickness1,
                        alpha * 0.85F, emissionScale, shaderData);
                }
                if (radial == 0) {
                    addDiskQuad(vertices, pose, scale,
                        radius0, angle1, warp01 + thickness0,
                        radius0, angle1, warp01 - thickness0,
                        radius0, angle0, warp00 - thickness0,
                        radius0, angle0, warp00 + thickness0,
                        alpha * 0.72F, emissionScale, shaderData);
                }
            }
        }
        poseStack.popPose();
    }

    private static float diskWarp(float radius, double angle, float scale, float amplitude) {
        float normalizedRadius = radius / scale;
        float angularWave = (float)Math.sin(angle * 2.0 + normalizedRadius * 3.5);
        float corrugation = (float)Math.sin(angle * 6.0 - normalizedRadius * 18.0);
        return amplitude * (angularWave * (0.3F + normalizedRadius * 0.7F) + corrugation * 0.22F);
    }

    private static void addDiskQuad(VertexConsumer vertices, PoseStack.Pose pose, float uvRadius,
        float radius0, double angle0, float z0, float radius1, double angle1, float z1,
        float radius2, double angle2, float z2, float radius3, double angle3, float z3,
        float alpha, float emissionScale, int shaderData) {
        addDiskVertex(vertices, pose, radius0, angle0, z0, uvRadius, alpha, emissionScale, shaderData);
        addDiskVertex(vertices, pose, radius1, angle1, z1, uvRadius, alpha, emissionScale, shaderData);
        addDiskVertex(vertices, pose, radius2, angle2, z2, uvRadius, alpha, emissionScale, shaderData);
        addDiskVertex(vertices, pose, radius3, angle3, z3, uvRadius, alpha, emissionScale, shaderData);
    }

    private static void addDiskVertex(VertexConsumer vertices, PoseStack.Pose pose, float radius,
        double angle, float z, float uvRadius, float alpha, float emissionScale, int shaderData) {
        float x = radius * (float)Math.cos(angle);
        float y = radius * (float)Math.sin(angle);
        vertices.addVertex(pose, x, y, z)
            .setColor(emissionScale, emissionScale, emissionScale, alpha)
            .setUv(0.5F + x / (2.0F * uvRadius), 0.5F + y / (2.0F * uvRadius))
            .setOverlay(shaderData)
            .setLight(0x00F000F0)
            .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static float daylightFactor(EffectRenderContext context) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0.0F;
        }

        var position = context.transform().position();
        BlockPos blockPosition = BlockPos.containing(position.x(), position.y(), position.z());
        float skyBrightness = level.getBrightness(LightLayer.SKY, blockPosition) / 15.0F;
        float skyDarkening = Math.min(1.0F, level.getSkyDarken() / 15.0F);
        float blockBrightness = level.getBrightness(LightLayer.BLOCK, blockPosition) / 15.0F;
        float ambientBrightness = Math.max(skyBrightness * (1.0F - skyDarkening), blockBrightness * 0.35F);
        float normalized = Math.max(0.0F, Math.min(1.0F, (ambientBrightness - 0.12F) / 0.78F));
        return normalized * normalized * (3.0F - 2.0F * normalized);
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
