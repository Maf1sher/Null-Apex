package dev.nullapex.dragon.fight;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

@FunctionalInterface
public interface DragonFightProgressionMetricsSource {
    DragonFightProgressionMetrics collect(EnderDragon dragon);
}
