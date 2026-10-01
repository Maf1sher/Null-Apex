package dev.nullapex.dragon.attack;

import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Objects;

/** Phase-specific ranges for automatic attack start intervals and retries. */
final class DragonAttackSelectionTiming {
    private static final TickRange OPENING_INTERVAL = new TickRange(360, 480);
    private static final TickRange ESCALATION_INTERVAL = new TickRange(300, 360);
    private static final TickRange FINAL_INTERVAL = new TickRange(240, 300);
    private static final TickRange RETRY_INTERVAL = new TickRange(20, 40);

    private DragonAttackSelectionTiming() {
    }

    static TickRange attackInterval(DragonFightPhase phase) {
        Objects.requireNonNull(phase, "phase");
        return switch (phase) {
            case OPENING -> OPENING_INTERVAL;
            case ESCALATION -> ESCALATION_INTERVAL;
            case FINAL -> FINAL_INTERVAL;
        };
    }

    static TickRange retryInterval() {
        return RETRY_INTERVAL;
    }

    record TickRange(int minimumTicks, int maximumTicks) {
        TickRange {
            if (minimumTicks <= 0 || maximumTicks < minimumTicks) {
                throw new IllegalArgumentException("Tick range must be positive and ordered");
            }
        }

        int size() {
            return this.maximumTicks - this.minimumTicks + 1;
        }

        int ticksForOffset(int offset) {
            if (offset < 0 || offset >= this.size()) {
                throw new IllegalArgumentException("Random offset is outside the tick range");
            }
            return this.minimumTicks + offset;
        }
    }
}
