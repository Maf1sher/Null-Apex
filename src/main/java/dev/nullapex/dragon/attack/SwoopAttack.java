package dev.nullapex.dragon.attack;

import dev.nullapex.dragon.fight.DragonFightPhase;
import dev.nullapex.dragon.movement.DragonMovementController;
import dev.nullapex.dragon.movement.FlightCommand;
import dev.nullapex.dragon.movement.FlightMath;
import dev.nullapex.dragon.movement.FlightRoutine;
import dev.nullapex.dragon.movement.FlightRoutineEndReason;
import dev.nullapex.dragon.movement.RoutineStartResult;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/** A short, target-focused flight attack with a damaging head-and-neck pass. */
public final class SwoopAttack implements DragonAttack {
    public static final SwoopAttack INSTANCE = new SwoopAttack();
    public static final DragonAttackDefinition DEFINITION = new DragonAttackDefinition(
        "swoop",
        Set.of(DragonFightPhase.ESCALATION, DragonFightPhase.FINAL),
        40,
        60,
        60,
        200
    );

    private static final double MAX_TARGET_DISTANCE_SQUARED = 96.0 * 96.0;

    private SwoopAttack() {
    }

    @Override
    public DragonAttackDefinition definition() {
        return DEFINITION;
    }

    @Override
    public DragonAttackExecution createExecution(EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Player target = serverLevel.players().stream()
            .filter(player -> player.isAlive() && !player.isSpectator() && !player.isCreative())
            .filter(player -> dragon.distanceToSqr(player) <= MAX_TARGET_DISTANCE_SQUARED)
            .min((first, second) -> Double.compare(dragon.distanceToSqr(first), dragon.distanceToSqr(second)))
            .orElse(null);
        return target == null ? null : new SwoopExecution(target);
    }

    private static final class SwoopExecution implements DragonAttackExecution {
        private final Player target;
        private SwoopRoutine routine;
        private DragonAttackStage stage = DragonAttackStage.WINDUP;

        private SwoopExecution(Player target) {
            this.target = target;
        }

        @Override
        public boolean tryStart(EnderDragon dragon, Consumer<DragonAttackEndReason> behaviorEnded) {
            if (!this.isValidTarget(dragon)) {
                return false;
            }

            this.routine = new SwoopRoutine(this.target, behaviorEnded);
            RoutineStartResult result = DragonMovementController.tryStartRoutine(dragon, this.routine);
            if (result != RoutineStartResult.STARTED) {
                this.routine = null;
                return false;
            }

            this.routine.accepted = true;
            this.playCue(dragon, 0.65F);
            return true;
        }

        @Override
        public void tick(EnderDragon dragon, DragonAttackStage stage) {
            if (this.stage == stage) {
                return;
            }
            this.stage = stage;
            if (this.routine != null) {
                this.routine.setStage(stage);
            }
            if (stage == DragonAttackStage.ACTIVE) {
                this.playCue(dragon, 1.0F);
            }
        }

        @Override
        public void stop(EnderDragon dragon, DragonAttackEndReason reason) {
            if (this.routine != null) {
                DragonMovementController.stopRoutine(dragon);
                this.routine = null;
            }
        }

        private boolean isValidTarget(EnderDragon dragon) {
            return !this.target.isRemoved()
                && this.target.isAlive()
                && !this.target.isSpectator()
                && !this.target.isCreative()
                && this.target.level() == dragon.level();
        }

        private void playCue(EnderDragon dragon, float pitch) {
            if (!(dragon.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            Vec3 position = this.target.position().add(0.0, 1.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, position.x, position.y, position.z,
                18, 0.35, 0.25, 0.35, 0.015);
            serverLevel.playSound(null, this.target.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 1.0F, pitch);
        }
    }

    private static final class SwoopRoutine implements FlightRoutine {
        private static final int HEAD_PART = 0;
        private static final int NECK_PART = 1;
        private static final int MAX_RECOVERY_TICKS = 60;
        private static final double MAX_SPEED = 1.8;

        private final Player target;
        private final Consumer<DragonAttackEndReason> behaviorEnded;
        private final AABB[] previousPartBoxes = new AABB[NECK_PART + 1];
        private DragonAttackStage stage = DragonAttackStage.WINDUP;
        private Vec3 recoveryTarget;
        private int stageTicks;
        private boolean hit;
        private boolean accepted;

        private SwoopRoutine(Player target, Consumer<DragonAttackEndReason> behaviorEnded) {
            this.target = target;
            this.behaviorEnded = behaviorEnded;
        }

        private void setStage(DragonAttackStage stage) {
            if (this.stage != stage) {
                this.stage = stage;
                this.stageTicks = 0;
                if (stage == DragonAttackStage.ACTIVE) {
                    this.hit = false;
                }
            }
        }

        @Override
        public FlightCommand tick(EnderDragon dragon) {
            if (!this.target.isAlive() || this.target.isRemoved() || this.target.level() != dragon.level()) {
                return null;
            }

            this.stageTicks++;
            if (this.stage == DragonAttackStage.ACTIVE && !this.hit && this.detectHit(dragon)) {
                this.applyHit(dragon);
            }

            return switch (this.stage) {
                case WINDUP -> this.windupCommand(dragon);
                case ACTIVE -> this.diveCommand(dragon);
                case RECOVERY -> this.recoveryCommand(dragon);
                case IDLE -> null;
            };
        }

        @Override
        public void onStop(EnderDragon dragon, FlightRoutineEndReason reason) {
            if (this.accepted) {
                this.behaviorEnded.accept(switch (reason) {
                    case COMPLETED -> DragonAttackEndReason.COMPLETED;
                    case CANCELLED, PHASE_TAKEOVER -> DragonAttackEndReason.CANCELLED;
                    case FAILED -> DragonAttackEndReason.FAILED;
                });
            }
        }

        private FlightCommand windupCommand(EnderDragon dragon) {
            Vec3 targetPosition = this.target.position().add(0.0, 10.0, 0.0);
            return this.directCommand(dragon, targetPosition, 0.75, 0.8, 0.05, 0.38F);
        }

        private FlightCommand diveCommand(EnderDragon dragon) {
            Vec3 targetPosition = this.target.position();
            Vec3 horizontalDirection = horizontalDirection(dragon.position(), targetPosition);
            Vec3 passPoint = targetPosition.add(horizontalDirection.scale(10.0)).add(0.0, 0.5, 0.0);
            return this.directCommand(dragon, passPoint, 1.35, 1.4, 0.08, 0.50F);
        }

        private FlightCommand recoveryCommand(EnderDragon dragon) {
            if (this.recoveryTarget == null) {
                Vec3 direction = horizontalDirection(Vec3.ZERO, dragon.getDeltaMovement());
                if (direction.lengthSqr() < 1.0E-6) {
                    direction = horizontalDirection(dragon.position(), this.target.position());
                }
                this.recoveryTarget = dragon.position().add(direction.scale(32.0)).add(0.0, 6.0, 0.0);
            }

            if (this.stageTicks > MAX_RECOVERY_TICKS
                || (dragon.position().distanceToSqr(this.recoveryTarget) <= 144.0
                    && Math.abs(dragon.getDeltaMovement().y) <= 0.25)) {
                return null;
            }
            return this.directCommand(dragon, this.recoveryTarget, 0.8, 0.35, 0.04, 0.35F);
        }

        private FlightCommand directCommand(
            EnderDragon dragon,
            Vec3 destination,
            double horizontalSpeed,
            double maxVerticalSpeed,
            double maxAcceleration,
            float turnResponsiveness
        ) {
            Vec3 offset = destination.subtract(dragon.position());
            Vec3 horizontalOffset = offset.multiply(1.0, 0.0, 1.0);
            Vec3 horizontalVelocity = horizontalOffset.lengthSqr() < 1.0E-9
                ? Vec3.ZERO
                : horizontalOffset.normalize().scale(horizontalSpeed);
            double verticalVelocity = FlightMath.clamp(
                FlightMath.desiredVerticalSpeed(offset.y), -maxVerticalSpeed, maxVerticalSpeed
            );
            Vec3 desiredVelocity = new Vec3(horizontalVelocity.x, verticalVelocity, horizontalVelocity.z);
            return FlightCommand.directVelocity(
                destination, desiredVelocity, MAX_SPEED, maxAcceleration, turnResponsiveness
            );
        }

        private boolean detectHit(EnderDragon dragon) {
            PartEntity<?>[] parts = dragon.getParts();
            if (parts.length <= NECK_PART) {
                return false;
            }

            SwoopHitDetection.Box targetBox = box(this.target.getBoundingBox());
            boolean intersects = false;
            for (int partIndex = HEAD_PART; partIndex <= NECK_PART; partIndex++) {
                AABB currentBox = parts[partIndex].getBoundingBox();
                intersects |= SwoopHitDetection.intersectsSweptPart(
                    this.previousPartBoxes[partIndex] == null ? null : box(this.previousPartBoxes[partIndex]),
                    box(currentBox),
                    targetBox
                );
                this.previousPartBoxes[partIndex] = currentBox;
            }
            return intersects;
        }

        private static SwoopHitDetection.Box box(AABB box) {
            return new SwoopHitDetection.Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }

        private void applyHit(EnderDragon dragon) {
            this.hit = true;
            if (!(dragon.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            boolean damaged = this.target.hurt(dragon.damageSources().mobAttack(dragon), 8.0F);
            if (!damaged) {
                return;
            }

            Vec3 knockbackDirection = this.target.position().subtract(dragon.position());
            this.target.knockback(1.1, -knockbackDirection.x, -knockbackDirection.z);
            Vec3 position = this.target.position().add(0.0, 0.7, 0.0);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, position.x, position.y, position.z,
                28, 0.45, 0.35, 0.45, 0.04);
            serverLevel.playSound(null, this.target.blockPosition(), SoundEvents.ENDER_DRAGON_HURT,
                SoundSource.HOSTILE, 1.0F, 1.0F);
        }

        private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
            Vec3 direction = to.subtract(from).multiply(1.0, 0.0, 1.0);
            if (direction.lengthSqr() < 1.0E-6) {
                return Vec3.ZERO;
            }
            return direction.normalize();
        }
    }
}
