package dev.nullapex.client.effect;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

/** Optional scene-color capture and passthrough composition for client screen effects. */
final class ClientScreenCompositor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int READ_FRAMEBUFFER = 0x8CA8;
    private static final int DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int COLOR_BUFFER_BIT = 0x00004000;
    private static final int NEAREST_FILTER = 0x2600;

    private TextureTarget sceneColorTarget;
    private volatile boolean enabled;
    private int unavailableWidth = -1;
    private int unavailableHeight = -1;

    void setEnabled(boolean enabled) {
        if (enabled && !this.enabled) {
            this.unavailableWidth = -1;
            this.unavailableHeight = -1;
        }
        this.enabled = enabled;
    }

    void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        if (!this.enabled) {
            this.releaseSceneColorTarget();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.releaseSceneColorTarget();
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;
        if (width <= 0 || height <= 0) {
            this.releaseSceneColorTarget();
            return;
        }
        if (width != this.unavailableWidth || height != this.unavailableHeight) {
            this.unavailableWidth = -1;
            this.unavailableHeight = -1;
        }
        if (width == this.unavailableWidth && height == this.unavailableHeight) {
            return;
        }

        boolean sceneCaptured = false;
        try {
            this.ensureTarget(width, height);
            this.captureSceneColor(mainTarget, this.sceneColorTarget);
            sceneCaptured = true;

            mainTarget.bindWrite(true);
            this.sceneColorTarget.blitToScreen(mainTarget.viewWidth, mainTarget.viewHeight);
            this.restoreMainTargetState(mainTarget);
        } catch (RuntimeException exception) {
            if (sceneCaptured) {
                this.restoreCapturedScene(mainTarget);
            } else {
                this.restoreMainTargetState(mainTarget);
            }
            this.unavailableWidth = width;
            this.unavailableHeight = height;
            this.releaseSceneColorTarget();
            LOGGER.warn("Screen-effect compositing is unavailable at {}x{}; continuing without it.",
                width, height, exception);
        }
    }

    void onLevelUnload() {
        this.enabled = false;
        this.unavailableWidth = -1;
        this.unavailableHeight = -1;
        this.releaseSceneColorTarget();
    }

    void onResourceReload() {
        this.unavailableWidth = -1;
        this.unavailableHeight = -1;
        this.releaseSceneColorTarget();
    }

    private void ensureTarget(int width, int height) {
        if (this.sceneColorTarget == null) {
            this.sceneColorTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            this.sceneColorTarget.setFilterMode(NEAREST_FILTER);
        } else if (this.sceneColorTarget.width != width || this.sceneColorTarget.height != height) {
            this.sceneColorTarget.resize(width, height, Minecraft.ON_OSX);
            this.sceneColorTarget.setFilterMode(NEAREST_FILTER);
        }
    }

    private void captureSceneColor(RenderTarget mainTarget, RenderTarget captureTarget) {
        GlStateManager._glBindFramebuffer(READ_FRAMEBUFFER, mainTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(DRAW_FRAMEBUFFER, captureTarget.frameBufferId);
        GlStateManager._glBlitFrameBuffer(
            0, 0, mainTarget.width, mainTarget.height,
            0, 0, captureTarget.width, captureTarget.height,
            COLOR_BUFFER_BIT, NEAREST_FILTER
        );
        mainTarget.bindWrite(false);
    }

    private void restoreCapturedScene(RenderTarget mainTarget) {
        try {
            mainTarget.bindWrite(true);
            if (this.sceneColorTarget != null) {
                this.sceneColorTarget.blitToScreen(mainTarget.viewWidth, mainTarget.viewHeight);
            }
        } catch (RuntimeException recoveryException) {
            LOGGER.warn("Could not restore the captured scene after a compositor failure.", recoveryException);
        } finally {
            this.restoreMainTargetState(mainTarget);
        }
    }

    private void restoreMainTargetState(RenderTarget mainTarget) {
        mainTarget.bindWrite(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void releaseSceneColorTarget() {
        TextureTarget target = this.sceneColorTarget;
        this.sceneColorTarget = null;
        if (target == null) {
            return;
        }

        if (RenderSystem.isOnRenderThreadOrInit()) {
            target.destroyBuffers();
        } else {
            RenderSystem.recordRenderCall(target::destroyBuffers);
        }
    }
}
