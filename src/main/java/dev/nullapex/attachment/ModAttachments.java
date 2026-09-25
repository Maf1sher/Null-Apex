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

    public static final Supplier<AttachmentType<Float>> DRAGON_ASCENT_PITCH = ATTACHMENT_TYPES.register(
        "dragon_ascent_pitch",
        () -> AttachmentType.builder(() -> 0.0F).sync(ByteBufCodecs.FLOAT).build()
    );

    public static final Supplier<AttachmentType<Float>> DRAGON_FLIGHT_PITCH = ATTACHMENT_TYPES.register(
        "dragon_flight_pitch",
        () -> AttachmentType.builder(() -> 0.0F).sync(ByteBufCodecs.FLOAT).build()
    );

    private ModAttachments() {
    }
}
