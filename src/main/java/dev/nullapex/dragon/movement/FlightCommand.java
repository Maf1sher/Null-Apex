package dev.nullapex.dragon.movement;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/** A single server-side flight request consumed by the dragon movement controller. */
public record FlightCommand(
    Vec3 target,
    float verticalAcceleration,
    float turnResponsiveness,
    Double horizontalCruiseSpeed
) {
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
    }

    public static FlightCommand vanilla(Vec3 target, float verticalAcceleration, float turnResponsiveness) {
        return new FlightCommand(target, verticalAcceleration, turnResponsiveness, null);
    }
}
