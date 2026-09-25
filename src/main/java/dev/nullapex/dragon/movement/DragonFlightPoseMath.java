package dev.nullapex.dragon.movement;

/** Shared pitch and part-offset math for the rendered dragon pose and multipart hitboxes. */
public final class DragonFlightPoseMath {
    private static final double MIN_DIRECTION_LENGTH = 1.0E-4;

    private DragonFlightPoseMath() {
    }

    /** Returns the direction pitch in degrees, or {@code null} for a stationary vector. */
    public static Float pitchDegrees(FlightVector velocity) {
        double horizontalSpeed = Math.hypot(velocity.x(), velocity.z());
        double speed = Math.hypot(horizontalSpeed, velocity.y());
        if (speed < MIN_DIRECTION_LENGTH || !Double.isFinite(speed)) {
            return null;
        }

        return (float)Math.toDegrees(Math.atan2(velocity.y(), horizontalSpeed));
    }

    /** Returns a forward offset for the dragon's local yaw and pitch, in world coordinates. */
    public static FlightVector forwardOffset(float yawDegrees, float pitchDegrees, double distance) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double horizontalScale = Math.cos(pitch) * distance;
        return new FlightVector(
            Math.sin(yaw) * horizontalScale,
            Math.sin(pitch) * distance,
            -Math.cos(yaw) * horizontalScale
        );
    }
}
