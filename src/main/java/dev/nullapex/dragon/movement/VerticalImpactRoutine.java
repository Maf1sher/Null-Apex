package dev.nullapex.dragon.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/** Operator test flight that climbs vertically and dives until its head reaches the ground. */
public final class VerticalImpactRoutine implements ManagedFlightRoutine {
    private static final double CLIMB_HEIGHT = 80.0;
    private static final double MIN_CLIMB_HEIGHT = 24.0;
    private static final double WORLD_CEILING_MARGIN = 16.0;
    private static final double MAX_DIRECT_SPEED = 1.8;
    private static final double MAX_ACCELERATION = 0.08;
    private static final double BRAKING_MARGIN = 4.0;
    private static final double STOP_SPEED = 0.08;
    private static final int MAX_ALIGNMENT_TICKS = 120;
    private static final int MAX_CLIMB_TICKS = 1200;
    private static final int MIN_TURN_TICKS = 34;
    private static final int MAX_TURN_TICKS = 80;
    private static final int MAX_DIVE_TICKS = 1200;
    private static final int HEAD_PART = 0;
    private static final int NECK_PART = 1;

    private Stage stage = Stage.ALIGN;
    private int stageTicks;
    private double centerX;
    private double centerZ;
    private double climbTargetY;
    private double groundSurfaceY;
    private int groundBlockX;
    private int groundBlockZ;
    private double previousHeadBottom = Double.NaN;

    public static boolean canStart(EnderDragon dragon) {
        double upwardVelocity = Math.max(dragon.getDeltaMovement().y, 0.0);
        double upwardStoppingDistance = upwardVelocity * upwardVelocity / (2.0 * MAX_ACCELERATION);
        double downwardVelocity = Math.max(-dragon.getDeltaMovement().y, 0.0);
        double downwardStoppingDistance = downwardVelocity * downwardVelocity / (2.0 * MAX_ACCELERATION);
        BlockPos currentColumn = BlockPos.containing(dragon.getX(), dragon.getY(), dragon.getZ());
        double groundSurfaceY = dragon.level().getHeight(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, currentColumn.getX(), currentColumn.getZ()
        );
        boolean enoughHeadroom = dragon.level().getMaxBuildHeight() - dragon.getY()
            >= MIN_CLIMB_HEIGHT + WORLD_CEILING_MARGIN + upwardStoppingDistance;
        boolean enoughGroundClearance = dragon.getY() - downwardStoppingDistance > groundSurfaceY + 3.0;
        return enoughHeadroom && enoughGroundClearance;
    }

    @Override
    public FlightRoutineResult tickResult(EnderDragon dragon) {
        this.stageTicks++;
        FlightCommand command = switch (this.stage) {
            case ALIGN -> this.tickAlign(dragon);
            case CLIMB -> this.tickClimb(dragon);
            case TURN -> this.tickTurn(dragon);
            case DIVE -> this.tickDive(dragon);
        };
        return command == null ? FlightRoutineResult.complete() : FlightRoutineResult.command(command);
    }

    @Override
    public boolean canContinueDuring(DragonMovementPhase phase) {
        return phase == DragonMovementPhase.LANDING_APPROACH
            || phase == DragonMovementPhase.LANDING
            || phase == DragonMovementPhase.SITTING;
    }

    private FlightCommand tickAlign(EnderDragon dragon) {
        if (dragon.getDeltaMovement().length() <= STOP_SPEED) {
            this.centerX = dragon.getX();
            this.centerZ = dragon.getZ();
            double currentY = dragon.getY();
            this.climbTargetY = Math.min(
                currentY + CLIMB_HEIGHT,
                dragon.level().getMaxBuildHeight() - WORLD_CEILING_MARGIN
            );
            if (this.climbTargetY - currentY < MIN_CLIMB_HEIGHT) {
                return null;
            }

            BlockPos column = BlockPos.containing(this.centerX, currentY, this.centerZ);
            this.groundBlockX = column.getX();
            this.groundBlockZ = column.getZ();
            this.groundSurfaceY = dragon.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.groundBlockX, this.groundBlockZ
            );
            this.changeStage(Stage.CLIMB);
            return this.tickClimb(dragon);
        }

        if (this.stageTicks > MAX_ALIGNMENT_TICKS) {
            return null;
        }

        return this.directCommand(
            dragon,
            dragon.getY() + 1.0,
            new Vec3(0.0, 1.0, 0.0),
            0.0
        );
    }

    private FlightCommand tickClimb(EnderDragon dragon) {
        if (shouldBeginTurn(dragon.getY(), this.climbTargetY, dragon.getDeltaMovement().y)) {
            this.changeStage(Stage.TURN);
            return this.tickTurn(dragon);
        }
        if (this.stageTicks > MAX_CLIMB_TICKS) {
            return null;
        }

        double desiredVerticalSpeed = climbSpeedForRemainingDistance(this.climbTargetY - dragon.getY());
        return this.directCommand(
            dragon,
            this.climbTargetY,
            new Vec3(0.0, desiredVerticalSpeed, 0.0),
            MAX_DIRECT_SPEED
        );
    }

    private FlightCommand tickTurn(EnderDragon dragon) {
        if (this.stageTicks >= MIN_TURN_TICKS && Math.abs(dragon.getDeltaMovement().y) <= STOP_SPEED) {
            this.changeStage(Stage.DIVE);
            this.previousHeadBottom = this.lowestMainPartBottom(dragon);
            return this.tickDive(dragon);
        }
        if (this.stageTicks > MAX_TURN_TICKS) {
            return null;
        }

        return this.directCommand(
            dragon,
            this.climbTargetY,
            new Vec3(0.0, -1.0, 0.0),
            0.0
        );
    }

    private FlightCommand tickDive(EnderDragon dragon) {
        if (this.hasHitGround(dragon)) {
            dragon.setDeltaMovement(Vec3.ZERO);
            EnderDragonPhase<?> currentPhase = dragon.getPhaseManager().getCurrentPhase().getPhase();
            if (currentPhase == EnderDragonPhase.LANDING_APPROACH || currentPhase == EnderDragonPhase.LANDING
                || currentPhase == EnderDragonPhase.SITTING_FLAMING
                || currentPhase == EnderDragonPhase.SITTING_SCANNING
                || currentPhase == EnderDragonPhase.SITTING_ATTACKING) {
                dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            }
            return null;
        }
        if (this.stageTicks > MAX_DIVE_TICKS) {
            return null;
        }

        return this.directCommand(
            dragon,
            this.groundSurfaceY,
            new Vec3(0.0, -MAX_DIRECT_SPEED, 0.0),
            MAX_DIRECT_SPEED
        );
    }

    private FlightCommand directCommand(EnderDragon dragon, double targetY, Vec3 desiredVelocity, double maxSpeed) {
        double targetX = this.stage == Stage.ALIGN ? dragon.getX() : this.centerX;
        double targetZ = this.stage == Stage.ALIGN ? dragon.getZ() : this.centerZ;
        return FlightCommand.directVelocity(
            new Vec3(targetX, targetY, targetZ),
            desiredVelocity,
            maxSpeed,
            MAX_ACCELERATION,
            0.32F
        );
    }

    private boolean hasHitGround(EnderDragon dragon) {
        PartEntity<?>[] parts = dragon.getParts();
        if (parts.length <= NECK_PART) {
            return false;
        }

        double currentBottom = Double.POSITIVE_INFINITY;
        boolean overlapsSurface = false;
        for (int partIndex = HEAD_PART; partIndex <= NECK_PART; partIndex++) {
            AABB box = parts[partIndex].getBoundingBox();
            if (this.overlapsGroundColumn(box)) {
                currentBottom = Math.min(currentBottom, box.minY);
                overlapsSurface |= box.minY <= this.groundSurfaceY && box.maxY >= this.groundSurfaceY - 1.0;
            }
        }
        if (!Double.isFinite(currentBottom)) {
            return false;
        }

        boolean crossedSurface = crossedImpactSurface(this.previousHeadBottom, currentBottom, this.groundSurfaceY);
        this.previousHeadBottom = currentBottom;
        return crossedSurface || overlapsSurface;
    }

    private double lowestMainPartBottom(EnderDragon dragon) {
        PartEntity<?>[] parts = dragon.getParts();
        if (parts.length <= NECK_PART) {
            return Double.NaN;
        }

        double lowestBottom = Double.POSITIVE_INFINITY;
        for (int partIndex = HEAD_PART; partIndex <= NECK_PART; partIndex++) {
            AABB box = parts[partIndex].getBoundingBox();
            if (this.overlapsGroundColumn(box)) {
                lowestBottom = Math.min(lowestBottom, box.minY);
            }
        }
        return Double.isFinite(lowestBottom) ? lowestBottom : Double.NaN;
    }

    private boolean overlapsGroundColumn(AABB box) {
        return box.maxX >= this.groundBlockX && box.minX <= this.groundBlockX + 1.0
            && box.maxZ >= this.groundBlockZ && box.minZ <= this.groundBlockZ + 1.0;
    }

    static double climbSpeedForRemainingDistance(double remainingDistance) {
        double distanceAvailableForBraking = Math.max(remainingDistance - BRAKING_MARGIN, 0.0);
        return Math.min(MAX_DIRECT_SPEED, Math.sqrt(2.0 * MAX_ACCELERATION * distanceAvailableForBraking));
    }

    static boolean shouldBeginTurn(double dragonY, double targetY, double verticalVelocity) {
        return dragonY >= targetY - BRAKING_MARGIN - 4.0 && Math.abs(verticalVelocity) <= 0.18;
    }

    static boolean crossedImpactSurface(double previousBottom, double currentBottom, double surfaceY) {
        return previousBottom > surfaceY && currentBottom <= surfaceY;
    }

    private void changeStage(Stage next) {
        this.stage = next;
        this.stageTicks = 0;
    }

    private enum Stage {
        ALIGN,
        CLIMB,
        TURN,
        DIVE
    }
}
