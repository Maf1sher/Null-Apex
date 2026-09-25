package dev.nullapex.dragon.movement;

public final class FlightMath {
    private static final double MAX_VERTICAL_SPEED = 1.4;
    private static final double VERTICAL_SPEED_PER_BLOCK = 0.08;
    private static final double VERTICAL_DRAG = 0.91;

    private FlightMath() {
    }

    public static double approach(double current, double target, double maxChange) {
        if (maxChange < 0.0) {
            throw new IllegalArgumentException("maxChange must not be negative");
        }

        return current + clamp(target - current, -maxChange, maxChange);
    }

    /** Returns the pre-drag velocity needed to approach a target vertical speed safely. */
    public static double verticalVelocityBeforeDrag(double currentVelocity, double verticalDistance, double maxAcceleration) {
        double targetVelocity = clamp(verticalDistance * VERTICAL_SPEED_PER_BLOCK, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
        double velocityAfterAcceleration = approach(currentVelocity, targetVelocity, maxAcceleration);
        return velocityAfterAcceleration / VERTICAL_DRAG;
    }

    public static float approachAngle(float current, float target, float maxChange) {
        if (maxChange < 0.0F) {
            throw new IllegalArgumentException("maxChange must not be negative");
        }

        float difference = wrapDegrees(target - current);
        return wrapDegrees(current + (float)clamp(difference, -maxChange, maxChange));
    }

    public static double stepScale(double deltaX, double deltaY, double deltaZ, double maxDistance) {
        if (maxDistance < 0.0) {
            throw new IllegalArgumentException("maxDistance must not be negative");
        }

        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        return distance <= maxDistance || distance < 1.0E-9 ? 1.0 : maxDistance / distance;
    }

    public static double alignVerticalTargetY(double currentY, double targetY, double verticalVelocity, double recentYChange) {
        if (targetY < currentY && (verticalVelocity > 0.015 || recentYChange > 0.1)) {
            return currentY;
        }
        if (targetY > currentY && (verticalVelocity < -0.015 || recentYChange < -0.1)) {
            return currentY;
        }
        return targetY;
    }

    public static float smootherStep(float progress) {
        float value = (float)clamp(progress, 0.0, 1.0);
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
