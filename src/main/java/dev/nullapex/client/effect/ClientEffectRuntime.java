package dev.nullapex.client.effect;

import dev.nullapex.dragon.effect.network.StartVisualEffectPayload;
import dev.nullapex.dragon.effect.network.StopVisualEffectPayload;
import dev.nullapex.dragon.effect.network.UpdateVisualEffectPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Client-lifetime owner and narrow entry point for effect event delegates. */
public final class ClientEffectRuntime {
    private static ClientVisualEffectManager manager;

    private ClientEffectRuntime() {
    }

    static void initialize() {
        if (manager != null) {
            return;
        }
        VisualEffectRendererRegistry rendererRegistry = new VisualEffectRendererRegistry();
        rendererRegistry.registerBuiltIns();
        manager = new ClientVisualEffectManager(rendererRegistry);
    }

    public static void start(StartVisualEffectPayload payload) {
        ClientVisualEffectManager current = manager;
        if (current != null) {
            current.start(payload);
        }
    }

    public static void stop(StopVisualEffectPayload payload) {
        ClientVisualEffectManager current = manager;
        if (current != null) {
            current.stop(payload);
        }
    }

    public static void update(UpdateVisualEffectPayload payload) {
        ClientVisualEffectManager current = manager;
        if (current != null) {
            current.update(payload);
        }
    }

    static void render(RenderLevelStageEvent event) {
        ClientVisualEffectManager current = manager;
        if (current != null) {
            current.render(event);
        }
    }

    static void onLevelUnload(ClientLevel level) {
        ClientVisualEffectManager current = manager;
        if (current != null) {
            current.onLevelUnload(level);
        }
    }

    static void onResourceReload(ResourceManager resourceManager) {
        ClientVisualEffectManager current = manager;
        if (current != null) {
            current.onResourceReload(resourceManager);
        }
    }
}
