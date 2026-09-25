package dev.nullapex.dragon.movement;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Temporary movement-only test routine. Delete this class and its command to remove the demo. */
public final class TestDiveRoutine implements FlightRoutine {
    private static final double MAX_DIRECT_SPEED = 1.8;
    private static final int MAX_CLIMB_TICKS = 600;
    private static final int MAX_DIVE_TICKS = 180;
    private static final int MAX_RECOVERY_TICKS = 140;
    private static final double CLIMB_HEIGHT = 60.0;
    private static final double MAX_CLIMB_VERTICAL_SPEED = 0.6;
    private static final double CLIMB_TOP_TOLERANCE = 3.0;
    private static final double CLIMB_TOP_MAX_VERTICAL_SPEED = 0.18;
    private static final double DIVE_PASS_START_HEIGHT = 12.0;
    private static final double DIVE_PASS_MAX_HEIGHT = 8.0;
    private static final double DIVE_PASS_CLEARANCE = 8.0;
    private static final double PASS_TARGET_MIN_LEAD = 16.0;
    private static final double PASS_TARGET_REMAINING_LEAD = 12.0;
    private static final double DIVE_HORIZONTAL_SPEED = 1.0;
    private static final double RECOVERY_FORWARD_DISTANCE = 32.0;
    private static final double RECOVERY_RISE = 6.0;
    private static final double MAX_RECOVERY_VERTICAL_SPEED = 0.35;

    private final Player target;
    private Stage stage = Stage.CLIMB;
    private int stageTicks;
    private Vec3 climbTarget;
    private Vec3 passDirection;
    private Vec3 recoveryTarget;

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
            case RECOVER -> this.tickRecover(dragon);
        };
    }

    private FlightCommand tickClimb(EnderDragon dragon, Vec3 targetPosition) {
        if (this.climbTarget == null) {
            this.climbTarget = new Vec3(dragon.getX(), climbTargetY(dragon.getY()), dragon.getZ());
        }

        if (shouldStartDive(dragon.getY(), this.climbTarget.y, dragon.getDeltaMovement().y)) {
            this.changeStage(Stage.DIVE);
            return this.tickDive(dragon, targetPosition);
        } else if (this.stageTicks > MAX_CLIMB_TICKS) {
            return null;
        }

        return this.directCommand(dragon, this.climbTarget, 0.0, 0.05, 0.32F, MAX_CLIMB_VERTICAL_SPEED);
    }

    private FlightCommand tickDive(EnderDragon dragon, Vec3 targetPosition) {
        double horizontalDistance = dragon.position().subtract(targetPosition).horizontalDistance();
        double verticalOffset = dragon.getY() - targetPosition.y;

        if (this.passDirection == null && shouldBeginPass(verticalOffset)) {
            this.passDirection = horizontalDirectionToTargetOrFacing(dragon, targetPosition);
        }

        double alongTrackDistance = this.passDirection == null
            ? Double.NEGATIVE_INFINITY
            : alongTrackDistance(dragon.position(), targetPosition, this.passDirection);
        if (shouldBeginRecovery(alongTrackDistance, verticalOffset, this.stageTicks)) {
            this.beginRecovery(dragon);
            return this.tickRecover(dragon);
        }

        float approach = (float)FlightMath.clamp(1.0 - horizontalDistance / 40.0, 0.0, 1.0);
        float progress = FlightMath.smootherStep(approach);
        float verticalAcceleration = lerp(5.0F, 8.0F, progress);
        float turnResponsiveness = lerp(0.35F, 0.50F, progress);
        Vec3 headingTarget = targetPosition.add(0.0, 1.0, 0.0);
        if (this.passDirection != null) {
            double leadDistance = passTargetLeadDistance(alongTrackDistance);
            headingTarget = targetPosition.add(this.passDirection.scale(leadDistance)).add(0.0, 1.0, 0.0);
        }
        return this.directCommand(
            dragon,
            headingTarget,
            DIVE_HORIZONTAL_SPEED,
            verticalAcceleration * 0.01,
            turnResponsiveness,
            1.4
        );
    }

    private FlightCommand tickRecover(EnderDragon dragon) {
        if (this.stageTicks > MAX_RECOVERY_TICKS
            || (dragon.position().distanceToSqr(this.recoveryTarget) <= 144.0
                && Math.abs(dragon.getDeltaMovement().y) <= 0.25)) {
            return null;
        }

        float progress = FlightMath.smootherStep(this.stageTicks / 36.0F);
        float verticalAcceleration = lerp(8.0F, 1.5F, progress);
        double horizontalSpeed = lerp(1.0, 0.75, progress);
        return this.directCommand(
            dragon,
            this.recoveryTarget,
            horizontalSpeed,
            verticalAcceleration * 0.01,
            0.35F,
            MAX_RECOVERY_VERTICAL_SPEED
        );
    }

    private FlightCommand directCommand(
        EnderDragon dragon,
        Vec3 target,
        double horizontalSpeed,
        double maxAcceleration,
        float turnResponsiveness,
        double maxVerticalSpeed
    ) {
        Vec3 offset = target.subtract(dragon.position());
        Vec3 horizontalOffset = offset.multiply(1.0, 0.0, 1.0);
        Vec3 desiredHorizontalVelocity = horizontalOffset.lengthSqr() < 1.0E-9
            ? Vec3.ZERO
            : horizontalOffset.normalize().scale(horizontalSpeed);
        Vec3 desiredVelocity = new Vec3(
            desiredHorizontalVelocity.x,
            cappedVerticalSpeed(offset.y, maxVerticalSpeed),
            desiredHorizontalVelocity.z
        );
        return FlightCommand.directVelocity(
            target, desiredVelocity, MAX_DIRECT_SPEED, maxAcceleration, turnResponsiveness
        );
    }

    static double climbTargetY(double startY) {
        return startY + CLIMB_HEIGHT;
    }

    static double cappedVerticalSpeed(double verticalDistance, double maxVerticalSpeed) {
        return FlightMath.clamp(
            FlightMath.desiredVerticalSpeed(verticalDistance), -maxVerticalSpeed, maxVerticalSpeed
        );
    }

    static boolean shouldStartDive(double dragonY, double climbTargetY, double verticalVelocity) {
        return Math.abs(dragonY - climbTargetY) <= CLIMB_TOP_TOLERANCE
            && Math.abs(verticalVelocity) <= CLIMB_TOP_MAX_VERTICAL_SPEED;
    }

    static boolean shouldBeginPass(double verticalOffset) {
        return verticalOffset <= DIVE_PASS_START_HEIGHT;
    }

    static boolean shouldBeginRecovery(double alongTrackDistance, double verticalOffset, int stageTicks) {
        return stageTicks > MAX_DIVE_TICKS
            || (alongTrackDistance >= DIVE_PASS_CLEARANCE && verticalOffset <= DIVE_PASS_MAX_HEIGHT);
    }

    static double passTargetLeadDistance(double alongTrackDistance) {
        return Math.max(PASS_TARGET_MIN_LEAD, alongTrackDistance + PASS_TARGET_REMAINING_LEAD);
    }

    static double recoveryTargetY(double dragonY) {
        return dragonY + RECOVERY_RISE;
    }

    private static double alongTrackDistance(Vec3 dragonPosition, Vec3 targetPosition, Vec3 direction) {
        Vec3 relativePosition = dragonPosition.subtract(targetPosition);
        return relativePosition.x * direction.x + relativePosition.z * direction.z;
    }

    private static Vec3 horizontalDirectionToTargetOrFacing(EnderDragon dragon, Vec3 targetPosition) {
        Vec3 direction = targetPosition.subtract(dragon.position()).multiply(1.0, 0.0, 1.0);
        if (direction.lengthSqr() >= 1.0E-4) {
            return direction.normalize();
        }

        direction = dragon.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        if (direction.lengthSqr() >= 1.0E-4) {
            return direction.normalize();
        }

        float yaw = dragon.getYRot() * ((float)Math.PI / 180.0F);
        return new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
    }

    private void beginRecovery(EnderDragon dragon) {
        Vec3 horizontalDirection = this.passDirection == null
            ? horizontalDirectionToTargetOrFacing(dragon, dragon.position().add(dragon.getDeltaMovement()))
            : this.passDirection;
        this.recoveryTarget = new Vec3(
            dragon.getX() + horizontalDirection.x * RECOVERY_FORWARD_DISTANCE,
            recoveryTargetY(dragon.getY()),
            dragon.getZ() + horizontalDirection.z * RECOVERY_FORWARD_DISTANCE
        );
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
