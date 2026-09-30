package dev.nullapex.mixin;

import dev.nullapex.dragon.attack.DragonAttackController;
import dev.nullapex.dragon.attack.DragonAttackControllerAccess;
import dev.nullapex.dragon.movement.DragonMovementController;
import dev.nullapex.dragon.movement.DragonMovementControllerAccess;
import dev.nullapex.dragon.movement.DragonMovementMixinBridge;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragon.class)
abstract class EnderDragonMixin implements DragonMovementControllerAccess, DragonAttackControllerAccess {
    @Unique
    private DragonMovementController nullApex$movementController;

    @Unique
    private DragonAttackController nullApex$attackController;

    @Override
    @Unique
    public DragonAttackController nullApex$getAttackController() {
        return this.nullApex$attackController;
    }

    @Override
    @Unique
    public void nullApex$setAttackController(DragonAttackController controller) {
        this.nullApex$attackController = controller;
    }

    @Override
    @Unique
    public DragonMovementController nullApex$getMovementController() {
        return this.nullApex$movementController;
    }

    @Override
    @Unique
    public void nullApex$setMovementController(DragonMovementController controller) {
        this.nullApex$movementController = controller;
    }

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
        boolean customDirectFlight = DragonMovementMixinBridge.isCustomDirectFlight(dragon);
        return DragonMovementMixinBridge.limitWingFlapExponent(
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
        return DragonMovementMixinBridge.resolveTarget(dragon, phase, phase.getFlyTargetLocation());
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
        return DragonMovementMixinBridge.adjustVerticalMovement(dragon, movement);
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
        return DragonMovementMixinBridge.resolveTurnResponsiveness((EnderDragon)(Object)this, vanillaValue);
    }

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void nullApex$adjustHorizontalAcceleration(EnderDragon dragon, float acceleration, Vec3 direction) {
        if (DragonMovementMixinBridge.applyDirectVelocity(dragon)) {
            return;
        }

        float controlledAcceleration = DragonMovementMixinBridge.adjustHorizontalAcceleration(dragon, acceleration);
        dragon.moveRelative(controlledAcceleration, direction);
    }
}
