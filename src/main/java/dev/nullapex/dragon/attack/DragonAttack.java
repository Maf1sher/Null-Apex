package dev.nullapex.dragon.attack;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** A registered attack definition that creates fresh per-execution behavior. */
public interface DragonAttack {
    DragonAttackDefinition definition();

    /** Returns {@code null} when this attack cannot select a valid target. */
    DragonAttackExecution createExecution(EnderDragon dragon);
}
