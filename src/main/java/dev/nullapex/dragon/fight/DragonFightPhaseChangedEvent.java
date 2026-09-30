package dev.nullapex.dragon.fight;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.bus.api.Event;

public final class DragonFightPhaseChangedEvent extends Event {
    private final EnderDragon dragon;
    private final DragonFightPhase previousPhase;
    private final DragonFightPhase currentPhase;

    public DragonFightPhaseChangedEvent(
        EnderDragon dragon,
        DragonFightPhase previousPhase,
        DragonFightPhase currentPhase
    ) {
        this.dragon = dragon;
        this.previousPhase = previousPhase;
        this.currentPhase = currentPhase;
    }

    public EnderDragon dragon() {
        return this.dragon;
    }

    public DragonFightPhase previousPhase() {
        return this.previousPhase;
    }

    public DragonFightPhase currentPhase() {
        return this.currentPhase;
    }
}
