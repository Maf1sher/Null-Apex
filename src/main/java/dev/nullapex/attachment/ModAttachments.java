package dev.nullapex.attachment;

import dev.nullapex.NullApex;
import dev.nullapex.dragon.fight.DragonFightPhase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final Codec<Map<String, Long>> DRAGON_ATTACK_COOLDOWNS_CODEC = Codec.list(
        RecordCodecBuilder.<AttackCooldown>create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(AttackCooldown::id),
            Codec.LONG.fieldOf("deadline").forGetter(AttackCooldown::deadline)
        ).apply(instance, AttackCooldown::new))
    ).xmap(
        entries -> entries.stream().collect(Collectors.toUnmodifiableMap(AttackCooldown::id, AttackCooldown::deadline)),
        cooldowns -> cooldowns.entrySet().stream()
            .map(entry -> new AttackCooldown(entry.getKey(), entry.getValue()))
            .toList()
    );

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

    public static final Supplier<AttachmentType<Map<String, Long>>> DRAGON_ATTACK_COOLDOWNS = ATTACHMENT_TYPES.register(
        "dragon_attack_cooldowns",
        () -> AttachmentType.builder(() -> Map.<String, Long>of())
            .serialize(DRAGON_ATTACK_COOLDOWNS_CODEC)
            .build()
    );

    private record AttackCooldown(String id, long deadline) {
    }

    private ModAttachments() {
    }
}
