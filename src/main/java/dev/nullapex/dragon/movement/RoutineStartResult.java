package dev.nullapex.dragon.movement;

/** Outcome of attempting to start a flight routine. */
public enum RoutineStartResult {
    STARTED,
    CLIENT_SIDE,
    DRAGON_DEAD,
    PROTECTED_PHASE,
    ALREADY_ACTIVE,
    INITIALIZATION_FAILED
}
