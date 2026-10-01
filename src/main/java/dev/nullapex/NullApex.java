package dev.nullapex;

import dev.nullapex.attachment.ModAttachments;
import dev.nullapex.dragon.effect.entity.ModEffectEntities;
import dev.nullapex.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NullApex.MOD_ID)
public final class NullApex {
    public static final String MOD_ID = "null_apex";

    public NullApex(IEventBus modEventBus) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModEffectEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
    }
}
