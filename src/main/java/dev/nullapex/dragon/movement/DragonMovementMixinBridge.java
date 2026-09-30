package dev.nullapex.dragon.movement;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.phys.Vec3;

/** Internal entry points for {@code EnderDragonMixin}; integrations should use the routine lifecycle API. */
public final class DragonMovementMixinBridge {
    private DragonMovementMixinBridge() {
    }

    public static boolean isCustomDirectFlight(EnderDragon dragon) {
        return DragonFlightVisualState.isCustomDirectFlight(dragon);
    }

    public static double limitWingFlapExponent(
        double verticalSpeed,
        double horizontalSpeed,
        boolean customDirectFlight
    ) {
        return DragonFlightVisualMath.limitWingFlapExponent(verticalSpeed, horizontalSpeed, customDirectFlight);
    }

    public static Vec3 resolveTarget(EnderDragon dragon, DragonPhaseInstance phase, Vec3 vanillaTarget) {
        return DragonMovementController.forDragon(dragon).resolveTarget(dragon, phase, vanillaTarget);
    }

    public static Vec3 adjustVerticalMovement(EnderDragon dragon, Vec3 vanillaMovement) {
        return DragonMovementController.forDragon(dragon).adjustVerticalMovement(dragon, vanillaMovement);
    }

    public static float resolveTurnResponsiveness(EnderDragon dragon, float vanillaValue) {
        return DragonMovementController.forDragon(dragon).resolveTurnResponsiveness(vanillaValue);
    }

    public static boolean applyDirectVelocity(EnderDragon dragon) {
        return DragonMovementController.forDragon(dragon).applyDirectVelocity(dragon);
    }

    public static float adjustHorizontalAcceleration(EnderDragon dragon, float vanillaAcceleration) {
        return DragonMovementController.forDragon(dragon).adjustHorizontalAcceleration(dragon, vanillaAcceleration);
    }
}
