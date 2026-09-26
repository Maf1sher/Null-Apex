package dev.nullapex.dragon.movement;

/** Why the controller stopped invoking a flight routine. */
public enum FlightRoutineEndReason {
    COMPLETED,
    CANCELLED,
    PHASE_TAKEOVER,
    FAILED
}
