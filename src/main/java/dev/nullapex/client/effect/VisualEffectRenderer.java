package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Client-only renderer for one registered transient visual effect type. The pose stack is positioned and oriented
 * at the effect's interpolated world transform before this callback.
 */
public interface VisualEffectRenderer {
    void render(VisualEffectInstance effect, float ageTicks, float progress, PoseStack poseStack,
        MultiBufferSource bufferSource);

    RenderType renderType();

    /** Refreshes any renderer-owned resources after Minecraft applies a client resource reload. */
    default void onResourceReload(ResourceManager resourceManager) {
    }
}
