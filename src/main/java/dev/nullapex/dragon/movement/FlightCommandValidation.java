package dev.nullapex.dragon.movement;

/** Minecraft-independent validation shared by flight-command construction and unit tests. */
final class FlightCommandValidation {
    private FlightCommandValidation() {
    }

    static void requireFiniteTarget(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("target coordinates must be finite");
        }
    }

    static void requireFiniteVelocity(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("desiredVelocity components must be finite");
        }
    }
}
