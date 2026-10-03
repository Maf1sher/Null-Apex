package dev.nullapex.client.effect;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nullapex.NullApex;
import java.io.IOException;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Render types for translucent effect geometry that must not write to the depth buffer. */
final class EffectRenderTypes {
    private static ShaderInstance visualEffectShader;
    private static ShaderInstance screenMaskShader;
    private static ShaderInstance screenCompositeShader;
    private static ShaderInstance screenBloomShader;
    private static final RenderStateShard.ShaderStateShard VISUAL_EFFECT_SHADER =
        new RenderStateShard.ShaderStateShard(() -> visualEffectShader);
    private static final RenderStateShard.ShaderStateShard SCREEN_MASK_SHADER =
        new RenderStateShard.ShaderStateShard(() -> screenMaskShader);
    private static final RenderStateShard.OutputStateShard SCREEN_MASK_TARGET =
        new RenderStateShard.OutputStateShard("null_apex_screen_mask_target", () -> { }, () -> { });
    private static final Function<ResourceLocation, RenderType> VISUAL = Util.memoize(
        texture -> createVisual(texture, RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER,
            "null_apex_translucent_visual_effect")
    );
    private static final Function<ResourceLocation, RenderType> DEBUG_VISUAL = Util.memoize(
        texture -> createVisual(texture, VISUAL_EFFECT_SHADER, "null_apex_shader_debug_visual_effect")
    );
    private static final Function<ResourceLocation, RenderType> SCREEN_MASK = Util.memoize(
        EffectRenderTypes::createScreenMask
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_UNSORTED = Util.memoize(
        texture -> createEntity(texture)
    );

    private EffectRenderTypes() {
    }

    static RenderType visual(ResourceLocation texture) {
        return VISUAL.apply(texture);
    }

    static RenderType debugVisual(ResourceLocation texture) {
        return DEBUG_VISUAL.apply(texture);
    }

    static RenderType screenMask(ResourceLocation texture) {
        return SCREEN_MASK.apply(texture);
    }

    static ShaderInstance screenCompositeShader() {
        return screenCompositeShader;
    }

    static ShaderInstance screenBloomShader() {
        return screenBloomShader;
    }

    static RenderType entityUnsorted(ResourceLocation texture) {
        return ENTITY_UNSORTED.apply(texture);
    }

    static void registerShaders(RegisterShadersEvent event) throws IOException {
        ResourceLocation shaderId = ResourceLocation.fromNamespaceAndPath(NullApex.MOD_ID, "effect_visual");
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), shaderId, DefaultVertexFormat.BLOCK),
            shader -> visualEffectShader = shader
        );

        ResourceLocation maskShaderId = ResourceLocation.fromNamespaceAndPath(NullApex.MOD_ID, "screen_mask");
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), maskShaderId, DefaultVertexFormat.BLOCK),
            shader -> screenMaskShader = shader
        );

        ResourceLocation compositeShaderId = ResourceLocation.fromNamespaceAndPath(NullApex.MOD_ID,
            "screen_composite");
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), compositeShaderId, DefaultVertexFormat.POSITION),
            shader -> screenCompositeShader = shader
        );

        ResourceLocation bloomShaderId = ResourceLocation.fromNamespaceAndPath(NullApex.MOD_ID, "screen_bloom");
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), bloomShaderId, DefaultVertexFormat.POSITION),
            shader -> screenBloomShader = shader
        );
    }

    private static RenderType createVisual(
        ResourceLocation texture,
        RenderStateShard.ShaderStateShard shaderState,
        String renderTypeName
    ) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(shaderState)
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setOutputState(RenderStateShard.PARTICLES_TARGET)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(true);
        return RenderType.create(
            renderTypeName,
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            2_097_152,
            true,
            true,
            state
        );
    }

    private static RenderType createEntity(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setOverlayState(RenderStateShard.OVERLAY)
            .setOutputState(RenderStateShard.MAIN_TARGET)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(true);
        return RenderType.create(
            "null_apex_translucent_entity_effect",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            true,
            false,
            state
        );
    }

    private static RenderType createScreenMask(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(SCREEN_MASK_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setOutputState(SCREEN_MASK_TARGET)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(true);
        return RenderType.create(
            "null_apex_screen_effect_mask",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            2_097_152,
            true,
            true,
            state
        );
    }
}
