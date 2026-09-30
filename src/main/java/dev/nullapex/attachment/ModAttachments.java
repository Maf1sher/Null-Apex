package dev.nullapex.attachment;

import dev.nullapex.NullApex;
import dev.nullapex.dragon.fight.DragonFightPhase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final Codec<DragonFightPhase> DRAGON_FIGHT_PHASE_CODEC = Codec.STRING.comapFlatMap(
        serializedName -> {
            try {
                return DataResult.success(DragonFightPhase.fromSerializedName(serializedName));
            } catch (IllegalArgumentException exception) {
                return DataResult.error(exception::getMessage);
            }
        },
        DragonFightPhase::serializedName
    );

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES, NullApex.MOD_ID
    );

    public static final Supplier<AttachmentType<Boolean>> DRAGON_DIRECT_FLIGHT = ATTACHMENT_TYPES.register(
        "dragon_direct_flight",
        () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build()
    );

    public static final Supplier<AttachmentType<DragonFightPhase>> DRAGON_FIGHT_PHASE = ATTACHMENT_TYPES.register(
        "dragon_fight_phase",
        () -> AttachmentType.builder(() -> DragonFightPhase.OPENING)
            .serialize(DRAGON_FIGHT_PHASE_CODEC)
            .build()
    );

    private ModAttachments() {
    }
}
