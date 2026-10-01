package dev.nullapex.sound;

import dev.nullapex.NullApex;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
        BuiltInRegistries.SOUND_EVENT, NullApex.MOD_ID
    );

    public static final Supplier<SoundEvent> EFFECT_TEST = SOUNDS.register(
        "effect_test",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NullApex.MOD_ID, "effect_test"))
    );

    private ModSounds() {
    }
}
