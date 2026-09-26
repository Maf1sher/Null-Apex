package dev.nullapex.dragon.movement;

/** Movement-relevant vanilla phase categories exposed to flight routines. */
public enum DragonMovementPhase {
    NORMAL,
    LANDING_APPROACH,
    LANDING,
    SITTING,
    DYING;

    boolean isProtected() {
        return this != NORMAL;
    }
}
