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
    private static final RenderStateShard.ShaderStateShard VISUAL_EFFECT_SHADER =
        new RenderStateShard.ShaderStateShard(() -> visualEffectShader);
    private static final Function<ResourceLocation, RenderType> VISUAL = Util.memoize(
        texture -> createVisual(texture, RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER,
            "null_apex_translucent_visual_effect")
    );
    private static final Function<ResourceLocation, RenderType> DEBUG_VISUAL = Util.memoize(
        texture -> createVisual(texture, VISUAL_EFFECT_SHADER, "null_apex_shader_debug_visual_effect")
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

    static RenderType entityUnsorted(ResourceLocation texture) {
        return ENTITY_UNSORTED.apply(texture);
    }

    static void registerVisualShader(RegisterShadersEvent event) throws IOException {
        ResourceLocation shaderId = ResourceLocation.fromNamespaceAndPath(NullApex.MOD_ID, "effect_visual");
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), shaderId, DefaultVertexFormat.BLOCK),
            shader -> visualEffectShader = shader
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
}
