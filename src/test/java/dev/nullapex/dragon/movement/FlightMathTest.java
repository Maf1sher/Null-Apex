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
    void vectorApproachLimitsTotalVelocityChange() {
        FlightVector approached = FlightMath.approachVector(FlightVector.ZERO, new FlightVector(3.0, 4.0, 0.0), 2.0);

        assertEquals(1.2, approached.x(), 1.0E-9);
        assertEquals(1.6, approached.y(), 1.0E-9);
        assertEquals(2.0, approached.length(), 1.0E-9);
    }

    @Test
    void vectorLengthClampPreservesDirectionAndCapsSpeed() {
        FlightVector clamped = FlightMath.clampLength(new FlightVector(3.0, 4.0, 0.0), 2.5);

        assertEquals(1.5, clamped.x(), 1.0E-9);
        assertEquals(2.0, clamped.y(), 1.0E-9);
        assertEquals(2.5, clamped.length(), 1.0E-9);
    }

    @Test
    void directVelocityCompensatesForDragonDrag() {
        FlightVector desiredVelocity = new FlightVector(0.45, 0.6, -0.8);
        FlightVector preDragVelocity = FlightMath.compensateForDragonDrag(desiredVelocity, 37.0F);
        FlightVector postDragVelocity = FlightMath.applyDragonDrag(preDragVelocity, 37.0F);

        assertEquals(desiredVelocity.x(), postDragVelocity.x(), 1.0E-8);
        assertEquals(desiredVelocity.y(), postDragVelocity.y(), 1.0E-8);
        assertEquals(desiredVelocity.z(), postDragVelocity.z(), 1.0E-8);

        FlightVector currentVelocity = new FlightVector(0.0, 0.0, 0.0);
        FlightVector limitedVelocity = FlightMath.approachVector(currentVelocity, desiredVelocity, 0.1);
        FlightVector appliedVelocity = FlightMath.applyDragonDrag(
            FlightMath.compensateForDragonDrag(limitedVelocity, 37.0F), 37.0F
        );
        assertEquals(0.1, appliedVelocity.length(), 1.0E-8);
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
    void climbCapsVerticalSpeedAndWaitsForTheDragonToLevelNearTheTarget() {
        assertEquals(0.6, TestDiveRoutine.cappedVerticalSpeed(100.0, 0.6), 1.0E-9);
        assertEquals(-0.6, TestDiveRoutine.cappedVerticalSpeed(-100.0, 0.6), 1.0E-9);
        assertFalse(TestDiveRoutine.shouldStartDive(119.0, 124.0, 0.0));
        assertFalse(TestDiveRoutine.shouldStartDive(123.0, 124.0, 0.3));
        assertTrue(TestDiveRoutine.shouldStartDive(123.0, 124.0, 0.1));
    }

    @Test
    void climbUsesDiagonalVelocityToReduceVanillaWingFlapRate() {
        FlightVector velocity = TestDiveRoutine.climbVelocity(3.0, 4.0, 100.0);

        assertEquals(0.24, velocity.x(), 1.0E-9);
        assertEquals(0.6, velocity.y(), 1.0E-9);
        assertEquals(0.32, velocity.z(), 1.0E-9);
        assertEquals(0.4, Math.hypot(velocity.x(), velocity.z()), 1.0E-9);
    }

    @Test
    void customAscentCapsFinalWingFlapRateWithoutChangingVanillaOrDiveRates() {
        double vanillaExponent = DragonFlightVisualMath.limitWingFlapExponent(1.8, 0.0, false);
        double verticalAscentExponent = DragonFlightVisualMath.limitWingFlapExponent(1.8, 0.0, true);
        double diagonalAscentExponent = DragonFlightVisualMath.limitWingFlapExponent(1.8, 0.4, true);
        double slowAscentExponent = DragonFlightVisualMath.limitWingFlapExponent(0.2, 0.4, true);
        double diveExponent = DragonFlightVisualMath.limitWingFlapExponent(-1.8, 0.0, true);

        assertEquals(1.8, vanillaExponent, 1.0E-9);
        assertEquals(0.06, 0.2 * Math.pow(2.0, verticalAscentExponent), 1.0E-9);
        assertEquals(0.06, 0.2 / 5.0 * Math.pow(2.0, diagonalAscentExponent), 1.0E-9);
        assertEquals(0.2, slowAscentExponent, 1.0E-9);
        assertEquals(-1.8, diveExponent, 1.0E-9);
    }

    @Test
    void verticalImpactRoutineBuildsSpeedAndBrakesBeforeItsApex() {
        assertEquals(1.8, VerticalImpactRoutine.climbSpeedForRemainingDistance(80.0), 1.0E-9);
        assertEquals(1.8, VerticalImpactRoutine.climbSpeedForRemainingDistance(24.25), 1.0E-9);
        assertEquals(0.8, VerticalImpactRoutine.climbSpeedForRemainingDistance(8.0), 1.0E-9);
        assertEquals(0.0, VerticalImpactRoutine.climbSpeedForRemainingDistance(4.0), 1.0E-9);

        assertFalse(VerticalImpactRoutine.shouldBeginTurn(91.0, 100.0, 0.1));
        assertFalse(VerticalImpactRoutine.shouldBeginTurn(93.0, 100.0, 0.2));
        assertTrue(VerticalImpactRoutine.shouldBeginTurn(93.0, 100.0, 0.1));
    }

    @Test
    void verticalImpactRoutineDetectsCrossingTheCapturedGroundSurface() {
        assertFalse(VerticalImpactRoutine.crossedImpactSurface(10.0, 9.6, 9.5));
        assertTrue(VerticalImpactRoutine.crossedImpactSurface(10.0, 9.0, 9.5));
        assertFalse(VerticalImpactRoutine.crossedImpactSurface(9.0, 8.0, 9.5));
    }

    @Test
    void divePassBeginsNearPlayerAndRecoveryWaitsForThePass() {
        assertFalse(TestDiveRoutine.shouldBeginPass(13.0));
        assertTrue(TestDiveRoutine.shouldBeginPass(12.0));
        assertFalse(TestDiveRoutine.shouldBeginRecovery(10.0, 9.0, 80));
        assertFalse(TestDiveRoutine.shouldBeginRecovery(7.0, 2.0, 80));
        assertTrue(TestDiveRoutine.shouldBeginRecovery(8.0, 8.0, 80));
        assertTrue(TestDiveRoutine.shouldBeginRecovery(0.0, 30.0, 181));
    }

    @Test
    void passTargetStaysAheadAndRecoveryOnlyRisesSlightlyFromThePassHeight() {
        assertEquals(16.0, TestDiveRoutine.passTargetLeadDistance(-50.0), 1.0E-9);
        assertEquals(22.0, TestDiveRoutine.passTargetLeadDistance(10.0), 1.0E-9);
        assertEquals(70.0, TestDiveRoutine.recoveryTargetY(64.0), 1.0E-9);
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
