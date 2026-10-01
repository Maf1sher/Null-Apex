package dev.nullapex.dragon.effect.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Harmless registered entity used to validate effect-entity tracking and client rendering. */
public final class EffectProbeEntity extends TimedEffectEntity {
    public EffectProbeEntity(EntityType<? extends EffectProbeEntity> entityType, Level level) {
        super(entityType, level);
    }
}
