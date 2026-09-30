package dev.nullapex.dragon.fight;

import dev.nullapex.attachment.ModAttachments;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.neoforged.neoforge.common.NeoForge;

/** Server-authoritative progression for the logical phases of an Ender Dragon fight. */
public final class DragonFightDirector {
    private static final DragonFightProgressionMetricsSource METRICS_SOURCE =
        new HealthOnlyDragonFightProgressionMetricsSource();
    private static final DragonFightPhasePolicy PHASE_POLICY = new HealthThresholdPhasePolicy();

    private DragonFightDirector() {
    }

    public static DragonFightPhase getCurrentPhase(EnderDragon dragon) {
        Objects.requireNonNull(dragon, "dragon");
        if (dragon.level().isClientSide) {
            throw new IllegalStateException("Dragon fight phases are server-authoritative");
        }
        return dragon.getData(ModAttachments.DRAGON_FIGHT_PHASE);
    }

    public static EnderDragon getActiveDragon(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        EndDragonFight fight = level.getDragonFight();
        UUID activeDragonId = fight == null ? null : fight.getDragonUUID();
        if (activeDragonId == null) {
            return null;
        }

        Entity entity = level.getEntity(activeDragonId);
        return entity instanceof EnderDragon dragon ? dragon : null;
    }

    public static boolean isActiveDragon(EnderDragon dragon) {
        Objects.requireNonNull(dragon, "dragon");
        return dragon.level() instanceof ServerLevel serverLevel
            && !dragon.isDeadOrDying()
            && getActiveDragon(serverLevel) == dragon;
    }

    static void tick(EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel serverLevel) || dragon.isDeadOrDying()) {
            return;
        }

        if (getActiveDragon(serverLevel) != dragon) {
            return;
        }

        DragonFightPhase previousPhase = getCurrentPhase(dragon);
        DragonFightProgressionMetrics metrics = METRICS_SOURCE.collect(dragon);
        DragonFightPhase currentPhase = PHASE_POLICY.nextPhase(previousPhase, metrics);
        if (currentPhase == previousPhase) {
            return;
        }

        dragon.setData(ModAttachments.DRAGON_FIGHT_PHASE, currentPhase);
        NeoForge.EVENT_BUS.post(new DragonFightPhaseChangedEvent(dragon, previousPhase, currentPhase));
    }
}
