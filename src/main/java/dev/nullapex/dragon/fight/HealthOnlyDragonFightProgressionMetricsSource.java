package dev.nullapex.dragon.fight;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public final class HealthOnlyDragonFightProgressionMetricsSource implements DragonFightProgressionMetricsSource {
    @Override
    public DragonFightProgressionMetrics collect(EnderDragon dragon) {
        double maxHealth = dragon.getMaxHealth();
        double healthRatio = maxHealth > 0.0 ? dragon.getHealth() / maxHealth : 0.0;
        return new DragonFightProgressionMetrics(healthRatio);
    }
}
