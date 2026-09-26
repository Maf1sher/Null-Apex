package dev.nullapex.dragon.movement;

/** Minecraft-independent protected-phase handoff policy. */
final class FlightRoutinePhasePolicy {
    private FlightRoutinePhasePolicy() {
    }

    static boolean shouldYieldControl(DragonMovementPhase phase, boolean routineAllowsPhase) {
        return phase == DragonMovementPhase.DYING
            || (phase.isProtected() && !routineAllowsPhase);
    }
}
