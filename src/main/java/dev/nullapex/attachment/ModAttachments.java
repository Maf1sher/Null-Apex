package dev.nullapex.attachment;

import dev.nullapex.NullApex;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES, NullApex.MOD_ID
    );

    public static final Supplier<AttachmentType<Boolean>> DRAGON_DIRECT_FLIGHT = ATTACHMENT_TYPES.register(
        "dragon_direct_flight",
        () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build()
    );

    private ModAttachments() {
    }
}
