package dev.nullapex.dragon.effect.network;

import dev.nullapex.NullApex;
import dev.nullapex.client.effect.ClientVisualEffectManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = NullApex.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class EffectNetwork {
    private EffectNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(StartVisualEffectPayload.TYPE, StartVisualEffectPayload.STREAM_CODEC,
            (payload, context) -> {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    context.enqueueWork(() -> ClientVisualEffectManager.start(payload));
                }
            });
        registrar.playToClient(UpdateVisualEffectPayload.TYPE, UpdateVisualEffectPayload.STREAM_CODEC,
            (payload, context) -> {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    context.enqueueWork(() -> ClientVisualEffectManager.update(payload));
                }
            });
        registrar.playToClient(StopVisualEffectPayload.TYPE, StopVisualEffectPayload.STREAM_CODEC,
            (payload, context) -> {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    context.enqueueWork(() -> ClientVisualEffectManager.stop(payload));
                }
            });
    }
}
