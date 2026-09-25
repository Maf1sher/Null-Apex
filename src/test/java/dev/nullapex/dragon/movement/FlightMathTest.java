package dev.nullapex.dragon.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlightMathTest {
    @Test
    void scalarApproachNeverExceedsConfiguredAcceleration() {
        assertEquals(0.25, FlightMath.approach(0.1, 1.0, 0.15), 1.0E-9);
        assertEquals(0.0, FlightMath.approach(0.1, 0.0, 0.15), 1.0E-9);
    }

    @Test
    void verticalVelocityControlAppliesAccelerationEvenForDistantTargets() {
        double velocityBeforeDrag = FlightMath.verticalVelocityBeforeDrag(0.0, 100.0, 0.018);

        assertEquals(0.018, velocityBeforeDrag * 0.91, 1.0E-9);
    }

    @Test
    void verticalVelocityControlLimitsReversalAndCompensatesForDrag() {
        double velocityBeforeDrag = FlightMath.verticalVelocityBeforeDrag(0.5, -100.0, 0.08);
        double velocityAfterDrag = velocityBeforeDrag * 0.91;

        assertEquals(0.42, velocityAfterDrag, 1.0E-9);
        assertEquals(0.08, Math.abs(0.5 - velocityAfterDrag), 1.0E-9);
    }

    @Test
    void verticalVelocityControlSlowsNearTheTargetAndCapsFarTargetSpeed() {
        double nearTargetVelocity = FlightMath.verticalVelocityBeforeDrag(0.0, 1.0, 0.08) * 0.91;
        double farTargetVelocity = FlightMath.verticalVelocityBeforeDrag(0.0, 100.0, 2.0) * 0.91;

        assertEquals(0.08, nearTargetVelocity, 1.0E-9);
        assertEquals(1.4, farTargetVelocity, 1.0E-9);
    }

    @Test
    void angleApproachUsesShortestPathAcrossWrapBoundary() {
        assertEquals(-179.0F, FlightMath.approachAngle(179.0F, -170.0F, 2.0F), 1.0E-5F);
    }

    @Test
    void reversalIsRateLimitedInsteadOfInstantaneous() {
        float turned = FlightMath.approachAngle(0.0F, 180.0F, 12.0F);

        assertEquals(12.0F, Math.abs(turned), 1.0E-5F);
        assertTrue(Math.abs(turned) < 180.0F);
    }

    @Test
    void targetStepScaleLimitsLongChanges() {
        assertEquals(0.25, FlightMath.stepScale(0.0, 10.0, 0.0, 2.5), 1.0E-9);
        assertEquals(1.0, FlightMath.stepScale(1.0, 0.0, 0.0, 2.5), 1.0E-9);
    }

    @Test
    void climbTargetIsRelativeToTheDragonStartPositionNotThePlayer() {
        assertEquals(124.0, TestDiveRoutine.climbTargetY(64.0), 1.0E-9);
    }

    @Test
    void diveDoesNotRecoverSevenBlocksAboveOrFarFromThePlayer() {
        assertFalse(TestDiveRoutine.shouldRecoverFromDive(71.0, 64.0, 20.0, 80, true));
        assertFalse(TestDiveRoutine.shouldRecoverFromDive(66.0, 64.0, 8.0, 80, true));
        assertTrue(TestDiveRoutine.shouldRecoverFromDive(66.0, 64.0, 5.0, 80, true));
        assertTrue(TestDiveRoutine.shouldRecoverFromDive(61.0, 64.0, 20.0, 80, true));
        assertFalse(TestDiveRoutine.shouldRecoverFromDive(61.0, 100.0, 20.0, 80, false));
        assertTrue(TestDiveRoutine.shouldRecoverFromDive(80.0, 64.0, 20.0, 181, false));
    }

    @Test
    void verticalTargetWaitsUntilTheBodyLevelsBeforeReversingPitch() {
        assertEquals(50.0, FlightMath.alignVerticalTargetY(50.0, 20.0, 0.2, 1.0), 1.0E-9);
        assertEquals(20.0, FlightMath.alignVerticalTargetY(50.0, 20.0, 0.0, 0.0), 1.0E-9);
    }

    @Test
    void upwardTargetWaitsForDescendingMotionToLevel() {
        assertEquals(30.0, FlightMath.alignVerticalTargetY(30.0, 60.0, -0.2, -1.0), 1.0E-9);
        assertEquals(60.0, FlightMath.alignVerticalTargetY(30.0, 60.0, 0.0, 0.0), 1.0E-9);
    }

    @Test
    void smootherStepHasFlatEndpointsAndClampsProgress() {
        assertEquals(0.0F, FlightMath.smootherStep(-1.0F), 1.0E-6F);
        assertEquals(0.0F, FlightMath.smootherStep(0.0F), 1.0E-6F);
        assertEquals(0.5F, FlightMath.smootherStep(0.5F), 1.0E-6F);
        assertEquals(1.0F, FlightMath.smootherStep(1.0F), 1.0E-6F);
        assertEquals(1.0F, FlightMath.smootherStep(2.0F), 1.0E-6F);
    }
}
