package dev.nullapex;

import dev.nullapex.attachment.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NullApex.MOD_ID)
public final class NullApex {
    public static final String MOD_ID = "null_apex";

    public NullApex(IEventBus modEventBus) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
    }
}
