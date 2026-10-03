package dev.nullapex.client.effect;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

/** Optional depth-aware per-effect mask composition for client screen effects. */
final class ClientScreenCompositor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int READ_FRAMEBUFFER = 0x8CA8;
    private static final int DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int COLOR_BUFFER_BIT = 0x00004000;
    private static final int NEAREST_FILTER = 0x2600;

    private TextureTarget sceneColorTarget;
    private TextureTarget compositionTarget;
    private TextureTarget screenMaskTarget;
    private boolean hadScreenEffects;
    private boolean failAfterCompositionOnce;
    private int unavailableWidth = -1;
    private int unavailableHeight = -1;

    void render(RenderLevelStageEvent event, ClientVisualEffectManager manager, List<ScreenEffectFrame> screenEffects) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        if (screenEffects.isEmpty()) {
            this.hadScreenEffects = false;
            this.unavailableWidth = -1;
            this.unavailableHeight = -1;
            this.releaseTargets();
            return;
        }
        if (!this.hadScreenEffects) {
            this.unavailableWidth = -1;
            this.unavailableHeight = -1;
        }
        this.hadScreenEffects = true;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || manager == null) {
            this.releaseTargets();
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        int width = mainTarget.width;
        int height = mainTarget.height;
        if (width <= 0 || height <= 0) {
            this.releaseTargets();
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
            this.ensureTargets(width, height);
            if (mainTarget.getDepthTextureId() <= 0 || this.screenMaskTarget.getDepthTextureId() <= 0) {
                throw new IllegalStateException("The main render target has no sampleable depth attachment");
            }

            this.captureSceneColor(mainTarget, this.sceneColorTarget);
            sceneCaptured = true;

            RenderTarget currentColor = mainTarget;
            for (ScreenEffectFrame screenEffect : screenEffects) {
                this.renderMask(event, manager, mainTarget, screenEffect);
                RenderTarget destination = currentColor == mainTarget ? this.compositionTarget : mainTarget;
                this.compositeEffect(currentColor, destination, screenEffect);
                currentColor = destination;
            }

            if (currentColor != mainTarget) {
                mainTarget.bindWrite(true);
                currentColor.blitToScreen(mainTarget.viewWidth, mainTarget.viewHeight);
            }
            if (this.failAfterCompositionOnce) {
                this.failAfterCompositionOnce = false;
                throw new IllegalStateException("Screen compositor failure injection");
            }
            this.restoreMainTargetState(mainTarget);
        } catch (RuntimeException exception) {
            this.failAfterCompositionOnce = false;
            if (sceneCaptured) {
                this.restoreCapturedScene(mainTarget);
            } else {
                this.restoreMainTargetState(mainTarget);
            }
            this.unavailableWidth = width;
            this.unavailableHeight = height;
            this.releaseTargets();
            LOGGER.warn("Depth-aware screen effects are unavailable at {}x{}; continuing without them.",
                width, height, exception);
        }
    }

    void onLevelUnload() {
        this.hadScreenEffects = false;
        this.failAfterCompositionOnce = false;
        this.unavailableWidth = -1;
        this.unavailableHeight = -1;
        this.releaseTargets();
    }

    void onResourceReload() {
        this.failAfterCompositionOnce = false;
        this.unavailableWidth = -1;
        this.unavailableHeight = -1;
        this.releaseTargets();
    }

    boolean armFailureAfterCompositionOnce() {
        this.failAfterCompositionOnce = true;
        return true;
    }

    private void renderMask(RenderLevelStageEvent event, ClientVisualEffectManager manager, RenderTarget mainTarget,
        ScreenEffectFrame screenEffect) {
        this.screenMaskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        this.screenMaskTarget.clear(false);
        this.screenMaskTarget.copyDepthFrom(mainTarget);
        this.screenMaskTarget.bindWrite(true);
        try {
            manager.renderScreenMask(event, screenEffect);
        } finally {
            this.screenMaskTarget.unbindWrite();
        }
    }

    private void compositeMask(RenderTarget sourceColor, RenderTarget destination, ScreenEffectMask mask) {
        ShaderInstance shader = Objects.requireNonNull(EffectRenderTypes.screenCompositeShader(),
            "screen composite shader is not registered");

        this.bindCompositeInputs(shader, sourceColor, mask);
        this.setUniform(shader, "Operation", 0.0F);
        this.drawComposite(destination, shader);
    }

    private void compositeWaveDistortion(RenderTarget sourceColor, RenderTarget destination,
        ScreenEffectFrame screenEffect) {
        ShaderInstance shader = Objects.requireNonNull(EffectRenderTypes.screenCompositeShader(),
            "screen composite shader is not registered");
        WaveDistortionSettings settings = Objects.requireNonNull(
            screenEffect.settings().waveDistortionSettings(), "wave distortion settings");

        this.bindCompositeInputs(shader, sourceColor, screenEffect.mask());
        this.setUniform(shader, "Operation", 1.0F);
        this.setUniform(shader, "WaveAmplitudePixels", settings.amplitudePixels());
        this.setUniform(shader, "WaveFrequency", settings.frequencyCycles());
        this.setUniform(shader, "WaveSpeed", settings.speedCyclesPerTick());
        this.setUniform(shader, "WaveTime", screenEffect.context().ageTicks());
        this.setUniform(shader, "ViewportWidth", destination.width);
        this.drawComposite(destination, shader);
    }

    private void bindCompositeInputs(ShaderInstance shader, RenderTarget sourceColor, ScreenEffectMask mask) {
        shader.setSampler("SceneSampler", sourceColor.getColorTextureId());
        shader.setSampler("MaskSampler", this.screenMaskTarget.getColorTextureId());
        shader.setSampler("DepthSampler", this.screenMaskTarget.getDepthTextureId());
        AbstractUniform maskColor = Objects.requireNonNull(shader.getUniform("MaskColor"), "MaskColor uniform");
        maskColor.set(mask.red(), mask.green(), mask.blue());
        this.setUniform(shader, "MaskStrength", mask.strength());
    }

    private void drawComposite(RenderTarget destination, ShaderInstance shader) {
        destination.bindWrite(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        boolean shaderApplied = false;
        try {
            shader.apply();
            shaderApplied = true;
            BufferBuilder builder = RenderSystem.renderThreadTesselator()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.addVertex(0.0F, 0.0F, 0.0F);
            builder.addVertex(1.0F, 0.0F, 0.0F);
            builder.addVertex(1.0F, 1.0F, 0.0F);
            builder.addVertex(0.0F, 1.0F, 0.0F);
            BufferUploader.draw(builder.buildOrThrow());
        } finally {
            if (shaderApplied) {
                shader.clear();
            }
        }
    }

    private void compositeEffect(RenderTarget sourceColor, RenderTarget destination, ScreenEffectFrame screenEffect) {
        switch (screenEffect.settings().operation()) {
            case DIAGNOSTIC_MASK_PREVIEW -> this.compositeMask(sourceColor, destination, screenEffect.mask());
            case MASK_SCOPED_WAVE_DISTORTION -> this.compositeWaveDistortion(sourceColor, destination, screenEffect);
        }
    }

    private void setUniform(ShaderInstance shader, String name, float value) {
        AbstractUniform uniform = Objects.requireNonNull(shader.getUniform(name), name + " uniform");
        uniform.set(value);
    }

    private void ensureTargets(int width, int height) {
        this.sceneColorTarget = this.ensureTarget(this.sceneColorTarget, width, height, false);
        this.compositionTarget = this.ensureTarget(this.compositionTarget, width, height, false);
        this.screenMaskTarget = this.ensureTarget(this.screenMaskTarget, width, height, true);
    }

    private TextureTarget ensureTarget(TextureTarget target, int width, int height, boolean useDepth) {
        if (target == null) {
            target = new TextureTarget(width, height, useDepth, Minecraft.ON_OSX);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
        target.setFilterMode(NEAREST_FILTER);
        return target;
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

    private void releaseTargets() {
        TextureTarget sceneColor = this.sceneColorTarget;
        TextureTarget composition = this.compositionTarget;
        TextureTarget mask = this.screenMaskTarget;
        this.sceneColorTarget = null;
        this.compositionTarget = null;
        this.screenMaskTarget = null;
        this.releaseTarget(sceneColor);
        this.releaseTarget(composition);
        this.releaseTarget(mask);
    }

    private void releaseTarget(TextureTarget target) {
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
