package dev.nullapex.client.effect;

import dev.nullapex.NullApex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = NullApex.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientEffectRenderEvents {
    private ClientEffectRenderEvents() {
    }

    @SubscribeEvent
    public static void renderEffects(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ClientEffectRuntime.render(event);
        }
    }
}
