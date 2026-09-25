package dev.nullapex.dragon.movement;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** Produces one custom flight command per server tick; returning null hands control back to vanilla. */
@FunctionalInterface
public interface FlightRoutine {
    FlightCommand tick(EnderDragon dragon);
}
