package dev.nullapex.dragon.movement;

/** Math for visual dragon flight adjustments, kept independent of Minecraft classes. */
public final class DragonFlightVisualMath {
    private static final double VANILLA_BASE_WING_FLAP_RATE = 0.2;
    private static final double MAX_CUSTOM_ASCENT_WING_FLAP_RATE = 0.06;

    private DragonFlightVisualMath() {
    }

    /** Limits vanilla's final wing-flap phase advance during custom upward direct flight. */
    public static double limitWingFlapExponent(
        double verticalSpeed,
        double horizontalSpeed,
        boolean customDirectFlight
    ) {
        if (!customDirectFlight || verticalSpeed <= 0.0) {
            return verticalSpeed;
        }

        double horizontalFactor = Math.max(horizontalSpeed, 0.0) * 10.0 + 1.0;
        double maxVerticalMultiplier = MAX_CUSTOM_ASCENT_WING_FLAP_RATE * horizontalFactor
            / VANILLA_BASE_WING_FLAP_RATE;
        double maxExponent = Math.log(maxVerticalMultiplier) / Math.log(2.0);
        return Math.min(verticalSpeed, maxExponent);
    }
}
