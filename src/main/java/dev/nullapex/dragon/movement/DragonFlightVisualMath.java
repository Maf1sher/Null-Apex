package dev.nullapex.dragon.movement;

/** Math for visual dragon flight adjustments, kept independent of Minecraft classes. */
public final class DragonFlightVisualMath {
    private static final double MIN_UPWARD_SPEED = 0.2;
    private static final double MIN_VERTICAL_TO_HORIZONTAL_RATIO = 1.25;
    private static final double VANILLA_BASE_WING_FLAP_RATE = 0.2;
    private static final double MAX_CUSTOM_ASCENT_WING_FLAP_RATE = 0.06;
    private static final float MAX_ASCENT_PITCH = (float)Math.toRadians(35.0);

    private DragonFlightVisualMath() {
    }

    /** Returns a negative model-space pitch for steep custom ascents, or zero otherwise. */
    public static float ascentPitch(FlightVector velocity) {
        double horizontalSpeed = Math.hypot(velocity.x(), velocity.z());
        if (velocity.y() < MIN_UPWARD_SPEED
            || velocity.y() < horizontalSpeed * MIN_VERTICAL_TO_HORIZONTAL_RATIO) {
            return 0.0F;
        }

        double pitch = Math.atan2(velocity.y(), horizontalSpeed);
        return (float)-Math.min(pitch, MAX_ASCENT_PITCH);
    }

    public static float clampAscentPitch(float pitch) {
        return (float)FlightMath.clamp(pitch, -MAX_ASCENT_PITCH, 0.0F);
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
