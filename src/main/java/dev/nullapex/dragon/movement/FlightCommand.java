package dev.nullapex.dragon.movement;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/** A single server-side flight request consumed by the dragon movement controller. */
public record FlightCommand(
    Vec3 target,
    float verticalAcceleration,
    float turnResponsiveness,
    Double horizontalCruiseSpeed,
    Vec3 desiredVelocity,
    double maxSpeed,
    double maxAcceleration
) {
    public FlightCommand(Vec3 target, float verticalAcceleration, float turnResponsiveness, Double horizontalCruiseSpeed) {
        this(target, verticalAcceleration, turnResponsiveness, horizontalCruiseSpeed, null, 0.0, 0.0);
    }

    public FlightCommand {
        Objects.requireNonNull(target, "target");
        if (!Float.isFinite(verticalAcceleration) || verticalAcceleration < 0.0F) {
            throw new IllegalArgumentException("verticalAcceleration must be finite and non-negative");
        }
        if (!Float.isFinite(turnResponsiveness) || turnResponsiveness < 0.0F) {
            throw new IllegalArgumentException("turnResponsiveness must be finite and non-negative");
        }
        if (horizontalCruiseSpeed != null && (!Double.isFinite(horizontalCruiseSpeed) || horizontalCruiseSpeed < 0.0)) {
            throw new IllegalArgumentException("horizontalCruiseSpeed must be finite and non-negative");
        }

        if (desiredVelocity == null) {
            if (maxSpeed != 0.0 || maxAcceleration != 0.0) {
                throw new IllegalArgumentException("direct velocity limits require a desired velocity");
            }
        } else {
            if (!Double.isFinite(desiredVelocity.x) || !Double.isFinite(desiredVelocity.y) || !Double.isFinite(desiredVelocity.z)) {
                throw new IllegalArgumentException("desiredVelocity components must be finite");
            }
            if (verticalAcceleration != 0.0F || horizontalCruiseSpeed != null) {
                throw new IllegalArgumentException("direct velocity commands cannot also request target-mode acceleration");
            }
            if (!Double.isFinite(maxSpeed) || maxSpeed < 0.0) {
                throw new IllegalArgumentException("maxSpeed must be finite and non-negative");
            }
            if (!Double.isFinite(maxAcceleration) || maxAcceleration < 0.0) {
                throw new IllegalArgumentException("maxAcceleration must be finite and non-negative");
            }
        }
    }

    public static FlightCommand vanilla(Vec3 target, float verticalAcceleration, float turnResponsiveness) {
        return new FlightCommand(target, verticalAcceleration, turnResponsiveness, null);
    }

    /** Requests a world-space velocity while retaining a separate target for dragon heading. */
    public static FlightCommand directVelocity(
        Vec3 headingTarget,
        Vec3 desiredVelocity,
        double maxSpeed,
        double maxAcceleration,
        float turnResponsiveness
    ) {
        return new FlightCommand(headingTarget, 0.0F, turnResponsiveness, null, desiredVelocity, maxSpeed, maxAcceleration);
    }

    public boolean usesDirectVelocity() {
        return this.desiredVelocity != null;
    }
}
