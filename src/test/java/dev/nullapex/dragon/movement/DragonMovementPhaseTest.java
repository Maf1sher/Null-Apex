package dev.nullapex.dragon.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DragonMovementPhaseTest {
    @Test
    void ordinaryRoutinesYieldToProtectedPhases() {
        assertFalse(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.NORMAL, false));
        assertTrue(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.LANDING_APPROACH, false));
        assertTrue(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.LANDING, false));
        assertTrue(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.SITTING, false));
        assertTrue(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.DYING, false));
    }

    @Test
    void optedInRoutinesMayContinueExceptDuringDying() {
        assertFalse(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.LANDING_APPROACH, true));
        assertFalse(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.LANDING, true));
        assertFalse(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.SITTING, true));
        assertTrue(FlightRoutinePhasePolicy.shouldYieldControl(DragonMovementPhase.DYING, true));
    }
}
