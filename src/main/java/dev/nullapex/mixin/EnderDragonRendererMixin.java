package dev.nullapex.mixin;

import dev.nullapex.dragon.movement.DragonFlightVisualState;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.renderer.entity.EnderDragonRenderer")
abstract class EnderDragonRendererMixin {
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;",
            ordinal = 1
        ),
        index = 0
    )
    private float nullApex$useCustomFlightPitch(float vanillaPitch, EnderDragon dragon) {
        Float customPitch = DragonFlightVisualState.flightPitchDegrees(dragon);
        return customPitch == null ? vanillaPitch : customPitch;
    }
}
