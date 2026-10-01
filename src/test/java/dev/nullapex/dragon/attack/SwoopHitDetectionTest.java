package dev.nullapex.dragon.attack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SwoopHitDetectionTest {
    @Test
    void detectsAHitWhenTheDragonPartSweepsAcrossTheTargetBetweenTicks() {
        SwoopHitDetection.Box previous = new SwoopHitDetection.Box(-3.0, 0.0, -0.5, -2.0, 1.0, 0.5);
        SwoopHitDetection.Box current = new SwoopHitDetection.Box(2.0, 0.0, -0.5, 3.0, 1.0, 0.5);
        SwoopHitDetection.Box target = new SwoopHitDetection.Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);

        assertTrue(SwoopHitDetection.intersectsSweptPart(previous, current, target));
    }

    @Test
    void doesNotHitWhenTheDragonPartRemainsAwayFromTheTarget() {
        SwoopHitDetection.Box previous = new SwoopHitDetection.Box(-3.0, 5.0, -0.5, -2.0, 6.0, 0.5);
        SwoopHitDetection.Box current = new SwoopHitDetection.Box(2.0, 5.0, -0.5, 3.0, 6.0, 0.5);
        SwoopHitDetection.Box target = new SwoopHitDetection.Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);

        assertFalse(SwoopHitDetection.intersectsSweptPart(previous, current, target));
    }

    @Test
    void checksTheCurrentPartBoxOnTheFirstMovementTick() {
        SwoopHitDetection.Box current = new SwoopHitDetection.Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        SwoopHitDetection.Box target = new SwoopHitDetection.Box(0.9, 0.0, 0.0, 1.5, 1.5, 1.5);

        assertTrue(SwoopHitDetection.intersectsSweptPart(null, current, target));
    }
}
