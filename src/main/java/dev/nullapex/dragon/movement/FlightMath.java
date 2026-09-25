package dev.nullapex.dragon.movement;

public final class FlightMath {
    private static final double MAX_VERTICAL_SPEED = 1.4;
    private static final double VERTICAL_SPEED_PER_BLOCK = 0.08;
    private static final double VERTICAL_DRAG = 0.91;
    private static final double HORIZONTAL_DRAG_BASE = 0.8;
    private static final double HORIZONTAL_DRAG_RANGE = 0.15;
    private static final int DRAG_COMPENSATION_ITERATIONS = 8;

    private FlightMath() {
    }

    public static double approach(double current, double target, double maxChange) {
        if (maxChange < 0.0) {
            throw new IllegalArgumentException("maxChange must not be negative");
        }

        return current + clamp(target - current, -maxChange, maxChange);
    }

    public static FlightVector approachVector(FlightVector current, FlightVector target, double maxChange) {
        if (!Double.isFinite(maxChange) || maxChange < 0.0) {
            throw new IllegalArgumentException("maxChange must be finite and non-negative");
        }

        FlightVector difference = target.subtract(current);
        double distance = difference.length();
        if (distance <= maxChange || distance < 1.0E-9) {
            return target;
        }

        return current.add(difference.scale(maxChange / distance));
    }

    public static FlightVector clampLength(FlightVector vector, double maxLength) {
        if (!Double.isFinite(maxLength) || maxLength < 0.0) {
            throw new IllegalArgumentException("maxLength must be finite and non-negative");
        }
        if (maxLength == 0.0) {
            return FlightVector.ZERO;
        }

        double largestComponent = Math.max(Math.abs(vector.x()), Math.max(Math.abs(vector.y()), Math.abs(vector.z())));
        if (largestComponent == 0.0) {
            return vector;
        }

        FlightVector scaled = vector.scale(1.0 / largestComponent);
        double scaledLength = scaled.length();
        if (largestComponent <= maxLength / scaledLength) {
            return vector;
        }

        return scaled.scale(maxLength / scaledLength);
    }

    /** Compensates for the Ender Dragon's per-tick drag to request a post-drag velocity. */
    public static FlightVector compensateForDragonDrag(FlightVector desiredVelocity, float yawDegrees) {
        FlightVector preDrag = new FlightVector(
            desiredVelocity.x() / 0.875,
            desiredVelocity.y() / VERTICAL_DRAG,
            desiredVelocity.z() / 0.875
        );
        for (int iteration = 0; iteration < DRAG_COMPENSATION_ITERATIONS; iteration++) {
            double horizontalDrag = horizontalDragFactor(preDrag, yawDegrees);
            preDrag = new FlightVector(
                desiredVelocity.x() / horizontalDrag,
                desiredVelocity.y() / VERTICAL_DRAG,
                desiredVelocity.z() / horizontalDrag
            );
        }
        return preDrag;
    }

    /** Mirrors the velocity drag applied at the end of EnderDragon.aiStep. */
    public static FlightVector applyDragonDrag(FlightVector velocity, float yawDegrees) {
        double horizontalDrag = horizontalDragFactor(velocity, yawDegrees);
        return new FlightVector(
            velocity.x() * horizontalDrag,
            velocity.y() * VERTICAL_DRAG,
            velocity.z() * horizontalDrag
        );
    }

    /** Returns the pre-drag velocity needed to approach a target vertical speed safely. */
    public static double verticalVelocityBeforeDrag(double currentVelocity, double verticalDistance, double maxAcceleration) {
        double targetVelocity = desiredVerticalSpeed(verticalDistance);
        double velocityAfterAcceleration = approach(currentVelocity, targetVelocity, maxAcceleration);
        return velocityAfterAcceleration / VERTICAL_DRAG;
    }

    public static double desiredVerticalSpeed(double verticalDistance) {
        return clamp(verticalDistance * VERTICAL_SPEED_PER_BLOCK, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
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

    private static double horizontalDragFactor(FlightVector velocity, float yawDegrees) {
        float yawRadians = yawDegrees * ((float)Math.PI / 180.0F);
        FlightVector velocityDirection = normalizeOrZero(velocity);
        FlightVector facingDirection = normalizeOrZero(new FlightVector(
            Math.sin(yawRadians), velocity.y(), -Math.cos(yawRadians)
        ));
        double dot = velocityDirection.x() * facingDirection.x()
            + velocityDirection.y() * facingDirection.y()
            + velocityDirection.z() * facingDirection.z();
        return HORIZONTAL_DRAG_BASE + HORIZONTAL_DRAG_RANGE * (dot + 1.0) / 2.0;
    }

    private static FlightVector normalizeOrZero(FlightVector vector) {
        double length = vector.length();
        return length < 1.0E-4 || !Double.isFinite(length)
            ? FlightVector.ZERO
            : vector.scale(1.0 / length);
    }
}
