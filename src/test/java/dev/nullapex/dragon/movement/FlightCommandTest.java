package dev.nullapex.dragon.movement;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FlightCommandTest {
    @Test
    void rejectsNonFiniteTargetCoordinates() {
        assertThrows(
            IllegalArgumentException.class,
            () -> FlightCommandValidation.requireFiniteTarget(Double.NaN, 10.0, 20.0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> FlightCommandValidation.requireFiniteTarget(10.0, Double.POSITIVE_INFINITY, 20.0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> FlightCommandValidation.requireFiniteTarget(10.0, 20.0, Double.NEGATIVE_INFINITY)
        );
    }

    @Test
    void rejectsNonFiniteDesiredVelocityComponents() {
        assertThrows(
            IllegalArgumentException.class,
            () -> FlightCommandValidation.requireFiniteVelocity(0.0, Double.NaN, 1.0)
        );
    }
}
