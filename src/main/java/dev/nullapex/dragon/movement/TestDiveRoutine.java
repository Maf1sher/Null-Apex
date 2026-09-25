package dev.nullapex.dragon.movement;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Temporary movement-only test routine. Delete this class and its command to remove the demo. */
public final class TestDiveRoutine implements FlightRoutine {
    private static final int MAX_CLIMB_TICKS = 600;
    private static final int MAX_DIVE_TICKS = 180;
    private static final int MAX_RECOVERY_TICKS = 140;
    private static final double CLIMB_HEIGHT = 60.0;
    private static final double DIVE_END_HEIGHT = 2.0;
    private static final double DIVE_END_HORIZONTAL_DISTANCE = 6.0;
    private static final double DIVE_PASSED_TARGET_HEIGHT = -2.0;

    private final Player target;
    private Stage stage = Stage.CLIMB;
    private int stageTicks;
    private Vec3 climbTarget;
    private Vec3 recoveryOffset;
    private boolean reachedDiveTargetHeight;

    public TestDiveRoutine(Player target) {
        this.target = target;
    }

    @Override
    public FlightCommand tick(EnderDragon dragon) {
        if (this.target.isRemoved() || !this.target.isAlive() || this.target.level() != dragon.level()) {
            return null;
        }

        this.stageTicks++;
        Vec3 targetPosition = this.target.position();
        return switch (this.stage) {
            case CLIMB -> this.tickClimb(dragon, targetPosition);
            case DIVE -> this.tickDive(dragon, targetPosition);
            case RECOVER -> this.tickRecover(dragon, targetPosition);
        };
    }

    private FlightCommand tickClimb(EnderDragon dragon, Vec3 targetPosition) {
        if (this.climbTarget == null) {
            this.climbTarget = new Vec3(dragon.getX(), climbTargetY(dragon.getY()), dragon.getZ());
        }

        if (dragon.getY() >= this.climbTarget.y - 1.0) {
            this.changeStage(Stage.DIVE);
            return this.tickDive(dragon, targetPosition);
        } else if (this.stageTicks > MAX_CLIMB_TICKS) {
            return null;
        }

        return new FlightCommand(this.climbTarget, 5.0F, 0.32F, 0.0);
    }

    private FlightCommand tickDive(EnderDragon dragon, Vec3 targetPosition) {
        double horizontalDistance = dragon.position().subtract(targetPosition).horizontalDistance();
        if (dragon.getY() >= targetPosition.y + DIVE_END_HEIGHT) {
            this.reachedDiveTargetHeight = true;
        }

        if (shouldRecoverFromDive(
            dragon.getY(), targetPosition.y, horizontalDistance, this.stageTicks, this.reachedDiveTargetHeight
        )) {
            this.beginRecovery(dragon);
            return this.tickRecover(dragon, targetPosition);
        }

        float approach = (float)FlightMath.clamp(1.0 - horizontalDistance / 40.0, 0.0, 1.0);
        float progress = FlightMath.smootherStep(approach);
        float verticalAcceleration = lerp(5.0F, 8.0F, progress);
        float turnResponsiveness = lerp(0.35F, 0.50F, progress);
        double horizontalSpeed = lerp(1.0, 0.2, progress);
        return new FlightCommand(targetPosition.add(0.0, 1.0, 0.0), verticalAcceleration, turnResponsiveness, horizontalSpeed);
    }

    private FlightCommand tickRecover(EnderDragon dragon, Vec3 targetPosition) {
        if (this.stageTicks > MAX_RECOVERY_TICKS
            || dragon.position().distanceToSqr(targetPosition.add(this.recoveryOffset)) <= 144.0) {
            return null;
        }

        float progress = FlightMath.smootherStep(this.stageTicks / 36.0F);
        float verticalAcceleration = lerp(8.0F, 1.5F, progress);
        double horizontalSpeed = lerp(1.0, 0.75, progress);
        Vec3 recoveryTarget = targetPosition.add(this.recoveryOffset);
        return new FlightCommand(recoveryTarget, verticalAcceleration, 0.35F, horizontalSpeed);
    }

    static double climbTargetY(double startY) {
        return startY + CLIMB_HEIGHT;
    }

    static boolean shouldRecoverFromDive(
        double dragonY,
        double playerY,
        double horizontalDistance,
        int stageTicks,
        boolean reachedDiveTargetHeight
    ) {
        return stageTicks > MAX_DIVE_TICKS
            || (dragonY <= playerY + DIVE_END_HEIGHT && horizontalDistance <= DIVE_END_HORIZONTAL_DISTANCE)
            || (reachedDiveTargetHeight && dragonY <= playerY + DIVE_PASSED_TARGET_HEIGHT);
    }

    private void beginRecovery(EnderDragon dragon) {
        Vec3 horizontalDirection = dragon.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        if (horizontalDirection.lengthSqr() < 1.0E-4) {
            float yaw = dragon.getYRot() * ((float)Math.PI / 180.0F);
            horizontalDirection = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
        } else {
            horizontalDirection = horizontalDirection.normalize();
        }

        this.recoveryOffset = horizontalDirection.scale(42.0).add(0.0, 34.0, 0.0);
        this.changeStage(Stage.RECOVER);
    }

    private void changeStage(Stage next) {
        this.stage = next;
        this.stageTicks = 0;
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static double lerp(double from, double to, float progress) {
        return from + (to - from) * progress;
    }

    private enum Stage {
        CLIMB,
        DIVE,
        RECOVER
    }
}
