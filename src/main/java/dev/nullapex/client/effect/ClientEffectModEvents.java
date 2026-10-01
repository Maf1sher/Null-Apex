package dev.nullapex.client.effect;

import dev.nullapex.NullApex;
import dev.nullapex.dragon.effect.entity.ModEffectEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = NullApex.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEffectModEvents {
    private ClientEffectModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEffectEntities.EFFECT_PROBE.get(), EffectProbeRenderer::new);
        VisualEffectRendererRegistry.registerBuiltIns();
    }
}
