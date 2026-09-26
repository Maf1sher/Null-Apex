package dev.nullapex.dragon.movement;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Produces one custom flight command per server tick. Returning {@code null} ends a legacy routine
 * and hands control back to vanilla. Implementations must keep callbacks short and server-thread safe.
 */
@FunctionalInterface
public interface FlightRoutine {
    FlightCommand tick(EnderDragon dragon);

    /** Called once after the controller accepts this routine. */
    default void onStart(EnderDragon dragon) {
    }

    /** Called once when an active routine completes, is cancelled, fails, or yields to a phase. */
    default void onStop(EnderDragon dragon, FlightRoutineEndReason reason) {
    }

    /** Whether this routine may continue during the supplied protected vanilla phase. */
    default boolean canContinueDuring(DragonMovementPhase phase) {
        return false;
    }
}
