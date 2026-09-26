package dev.nullapex.dragon.movement;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Extended routine contract for implementations that need an explicit pause result. Existing
 * {@link FlightRoutine} lambdas remain supported and treat a {@code null} command as completion.
 */
public interface ManagedFlightRoutine extends FlightRoutine {
    FlightRoutineResult tickResult(EnderDragon dragon);

    @Override
    default FlightCommand tick(EnderDragon dragon) {
        throw new UnsupportedOperationException("Managed routines are ticked through tickResult");
    }
}
