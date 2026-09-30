package dev.nullapex.dragon.attack;

import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Objects;

/** Minecraft-independent state machine for one active dragon attack. */
public final class DragonAttackLifecycle {
    private DragonAttackStage stage = DragonAttackStage.IDLE;
    private DragonAttackDefinition definition;
    private int remainingStageTicks;

    public StartResult tryStart(DragonAttackDefinition definition, DragonFightPhase fightPhase) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(fightPhase, "fightPhase");

        if (this.stage != DragonAttackStage.IDLE) {
            return StartResult.ALREADY_ACTIVE;
        }
        if (!definition.allowedFightPhases().contains(fightPhase)) {
            return StartResult.PHASE_NOT_ALLOWED;
        }

        this.definition = definition;
        this.stage = DragonAttackStage.WINDUP;
        this.remainingStageTicks = definition.windupTicks();
        return StartResult.STARTED;
    }

    public void tick() {
        if (this.stage == DragonAttackStage.IDLE) {
            return;
        }

        this.remainingStageTicks--;
        if (this.remainingStageTicks > 0) {
            return;
        }

        switch (this.stage) {
            case WINDUP -> this.enterStage(DragonAttackStage.ACTIVE, this.definition.activeTicks());
            case ACTIVE -> this.enterStage(DragonAttackStage.RECOVERY, this.definition.recoveryTicks());
            case RECOVERY -> this.reset();
            case IDLE -> throw new IllegalStateException("An idle attack lifecycle cannot have a timer");
        }
    }

    public void cancel() {
        this.reset();
    }

    public DragonAttackStage stage() {
        return this.stage;
    }

    public String activeAttackId() {
        return this.definition == null ? null : this.definition.id();
    }

    public int remainingStageTicks() {
        return this.remainingStageTicks;
    }

    private void enterStage(DragonAttackStage stage, int durationTicks) {
        this.stage = stage;
        this.remainingStageTicks = durationTicks;
    }

    private void reset() {
        this.stage = DragonAttackStage.IDLE;
        this.definition = null;
        this.remainingStageTicks = 0;
    }

    public enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        PHASE_NOT_ALLOWED
    }
}
