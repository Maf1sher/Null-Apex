package dev.nullapex.mixin;

import dev.nullapex.dragon.movement.DragonFlightVisualMath;
import dev.nullapex.dragon.movement.DragonFlightVisualState;
import dev.nullapex.dragon.movement.DragonMovementController;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragon.class)
abstract class EnderDragonMixin {
    @ModifyArg(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;pow(DD)D"
        ),
        index = 1
    )
    private double nullApex$limitCustomAscentWingFlapRate(double verticalSpeed) {
        EnderDragon dragon = (EnderDragon)(Object)this;
        boolean customDirectFlight = DragonFlightVisualState.isCustomDirectFlight(dragon);
        return DragonFlightVisualMath.limitWingFlapExponent(
            verticalSpeed,
            dragon.getDeltaMovement().horizontalDistance(),
            customDirectFlight
        );
    }

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 nullApex$resolveFlightTarget(DragonPhaseInstance phase) {
        EnderDragon dragon = (EnderDragon)(Object)this;
        return DragonMovementController.mixinResolveTarget(dragon, phase, phase.getFlyTargetLocation());
    }

    @ModifyArg(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 0
        ),
        index = 0
    )
    private Vec3 nullApex$adjustVerticalMovement(Vec3 movement) {
        EnderDragon dragon = (EnderDragon)(Object)this;
        return DragonMovementController.mixinAdjustVerticalMovement(dragon, movement);
    }

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getTurnSpeed()F"
        )
    )
    private float nullApex$resolveTurnResponsiveness(DragonPhaseInstance phase) {
        float vanillaValue = phase.getTurnSpeed();
        return DragonMovementController.mixinResolveTurnResponsiveness((EnderDragon)(Object)this, vanillaValue);
    }

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void nullApex$adjustHorizontalAcceleration(EnderDragon dragon, float acceleration, Vec3 direction) {
        if (DragonMovementController.mixinApplyDirectVelocity(dragon)) {
            return;
        }

        float controlledAcceleration = DragonMovementController.mixinAdjustHorizontalAcceleration(dragon, acceleration);
        dragon.moveRelative(controlledAcceleration, direction);
    }
}
