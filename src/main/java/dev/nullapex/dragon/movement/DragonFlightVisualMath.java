package dev.nullapex.dragon.movement;

/** Math for visual dragon flight adjustments, kept independent of Minecraft classes. */
public final class DragonFlightVisualMath {
    private static final double MIN_UPWARD_SPEED = 0.2;
    private static final double MIN_VERTICAL_TO_HORIZONTAL_RATIO = 1.25;
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
}
