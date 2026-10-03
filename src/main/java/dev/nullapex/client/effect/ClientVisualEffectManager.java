package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nullapex.dragon.effect.EffectTimeline;
import dev.nullapex.dragon.effect.EffectTransform;
import dev.nullapex.dragon.effect.network.StartVisualEffectPayload;
import dev.nullapex.dragon.effect.network.StopVisualEffectPayload;
import dev.nullapex.dragon.effect.network.UpdateVisualEffectPayload;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Instance-owned state and rendering for transient client visual effects. */
public final class ClientVisualEffectManager {
    private static final int MAX_ACTIVE_EFFECTS = 128;
    private final Map<UUID, VisualEffectInstance> activeEffects = new LinkedHashMap<>();
    private final VisualEffectRendererRegistry rendererRegistry;
    private ClientLevel currentLevel;

    ClientVisualEffectManager(VisualEffectRendererRegistry rendererRegistry) {
        this.rendererRegistry = rendererRegistry;
    }

    void start(StartVisualEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        this.synchronizeLevel(level);
        if (level == null || !level.dimension().location().equals(payload.dimensionId())) {
            return;
        }
        if (this.rendererRegistry.find(payload.effectId()).isEmpty()) {
            return;
        }
        this.activeEffects.put(payload.instanceId(), new VisualEffectInstance(payload, level.getGameTime()));
        while (this.activeEffects.size() > MAX_ACTIVE_EFFECTS) {
            UUID oldest = this.activeEffects.keySet().iterator().next();
            this.activeEffects.remove(oldest);
        }
    }

    void stop(StopVisualEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        this.synchronizeLevel(level);
        if (level == null || !level.dimension().location().equals(payload.dimensionId())) {
            return;
        }
        this.activeEffects.remove(payload.instanceId());
    }

    void update(UpdateVisualEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        this.synchronizeLevel(level);
        if (level == null || !level.dimension().location().equals(payload.dimensionId())) {
            return;
        }
        VisualEffectInstance effect = this.activeEffects.get(payload.instanceId());
        if (effect != null) {
            this.activeEffects.put(payload.instanceId(),
                effect.withTransform(payload.transform(), payload.updateGameTime()));
        }
    }

    void render(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        this.synchronizeLevel(level);
        if (level == null) {
            return;
        }
        ResourceLocation currentDimension = level.dimension().location();
        long gameTime = level.getGameTime();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        this.activeEffects.entrySet().removeIf(entry -> {
            StartVisualEffectPayload payload = entry.getValue().payload();
            return !payload.dimensionId().equals(currentDimension)
                || EffectTimeline.isFinished(gameTime, payload.startGameTime(), payload.durationTicks());
        });

        if (this.activeEffects.isEmpty()) {
            return;
        }

        Vec3 cameraPosition = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Set<RenderType> usedRenderTypes = new HashSet<>();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        try {
            for (VisualEffectInstance effect : this.activeEffects.values()) {
                this.rendererRegistry.find(effect.effectId()).ifPresent(renderer -> {
                    StartVisualEffectPayload payload = effect.payload();
                    EffectTransform transform = effect.interpolatedTransform(gameTime, partialTick);
                    double cullingRadius = payload.scale();
                    AABB renderBounds = new AABB(
                        transform.position().x() - cullingRadius,
                        transform.position().y() - cullingRadius,
                        transform.position().z() - cullingRadius,
                        transform.position().x() + cullingRadius,
                        transform.position().y() + cullingRadius,
                        transform.position().z() + cullingRadius
                    );
                    if (!event.getFrustum().isVisible(renderBounds)) {
                        return;
                    }
                    double age = EffectTimeline.ageTicks(gameTime, partialTick, payload.startGameTime());
                    float progress = EffectTimeline.progress(
                        gameTime, partialTick, payload.startGameTime(), payload.durationTicks()
                    );
                    poseStack.pushPose();
                    try {
                        poseStack.translate(transform.position().x(), transform.position().y(),
                            transform.position().z());
                        EffectRenderTransform.applyRotation(poseStack, transform.yaw(), transform.pitch(),
                            transform.roll());
                        usedRenderTypes.add(renderer.renderType());
                        renderer.render(effect, (float)age, progress, poseStack, buffers);
                    } finally {
                        poseStack.popPose();
                    }
                });
            }
        } finally {
            poseStack.popPose();
            for (RenderType renderType : usedRenderTypes) {
                buffers.endBatch(renderType);
            }
        }
    }

    void onLevelUnload(ClientLevel level) {
        if (this.currentLevel == level) {
            this.activeEffects.clear();
            this.currentLevel = null;
        }
    }

    void onResourceReload(ResourceManager resourceManager) {
        this.rendererRegistry.onResourceReload(resourceManager);
    }

    private void synchronizeLevel(ClientLevel level) {
        if (this.currentLevel != level) {
            this.activeEffects.clear();
            this.currentLevel = level;
        }
    }
}
