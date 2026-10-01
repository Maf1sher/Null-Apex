package dev.nullapex.dragon.attack;

import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Runtime-only per-dragon schedule for the next automatic attack attempt. */
final class DragonAttackSelectionSchedule {
    private static final long UNSCHEDULED = Long.MIN_VALUE;

    private DragonFightPhase scheduledPhase;
    private long lastAttackStartTime = UNSCHEDULED;
    private long nextSelectionTime = UNSCHEDULED;

    boolean isDue(DragonFightPhase phase, long gameTime, IntSupplier intervalTicks) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(intervalTicks, "intervalTicks");

        if (this.scheduledPhase == null) {
            this.scheduledPhase = phase;
            this.nextSelectionTime = Math.addExact(gameTime, intervalTicks.getAsInt());
        } else if (this.scheduledPhase != phase) {
            this.scheduledPhase = phase;
            long anchorTime = this.lastAttackStartTime == UNSCHEDULED ? gameTime : this.lastAttackStartTime;
            this.nextSelectionTime = Math.addExact(anchorTime, intervalTicks.getAsInt());
        }

        return gameTime >= this.nextSelectionTime;
    }

    void onAttackStarted(DragonFightPhase phase, long gameTime, int intervalTicks) {
        Objects.requireNonNull(phase, "phase");
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException("Attack interval must be positive");
        }

        this.scheduledPhase = phase;
        this.lastAttackStartTime = gameTime;
        this.nextSelectionTime = Math.addExact(gameTime, intervalTicks);
    }

    void deferUntil(long gameTime) {
        this.nextSelectionTime = Math.max(this.nextSelectionTime, gameTime);
    }

    void deferForPhaseChange() {
        this.nextSelectionTime = Long.MAX_VALUE;
    }

    void retryAt(long gameTime, int retryTicks) {
        if (retryTicks <= 0) {
            throw new IllegalArgumentException("Retry interval must be positive");
        }
        this.nextSelectionTime = Math.addExact(gameTime, retryTicks);
    }

    long nextSelectionTime() {
        return this.nextSelectionTime;
    }
}
