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
        DragonMovementController controller = DragonMovementController.existingForDragon(dragon);
        return controller == null ? vanillaTarget : controller.resolveTarget(dragon, phase, vanillaTarget);
    }

    public static Vec3 adjustVerticalMovement(EnderDragon dragon, Vec3 vanillaMovement) {
        DragonMovementController controller = DragonMovementController.existingForDragon(dragon);
        return controller == null ? vanillaMovement : controller.adjustVerticalMovement(dragon, vanillaMovement);
    }

    public static float resolveTurnResponsiveness(EnderDragon dragon, float vanillaValue) {
        DragonMovementController controller = DragonMovementController.existingForDragon(dragon);
        return controller == null ? vanillaValue : controller.resolveTurnResponsiveness(vanillaValue);
    }

    public static boolean applyDirectVelocity(EnderDragon dragon) {
        DragonMovementController controller = DragonMovementController.existingForDragon(dragon);
        return controller != null && controller.applyDirectVelocity(dragon);
    }

    public static float adjustHorizontalAcceleration(EnderDragon dragon, float vanillaAcceleration) {
        DragonMovementController controller = DragonMovementController.existingForDragon(dragon);
        return controller == null
            ? vanillaAcceleration
            : controller.adjustHorizontalAcceleration(dragon, vanillaAcceleration);
    }
}
