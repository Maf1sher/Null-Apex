package dev.nullapex.dragon.movement;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

/**
 * Per-dragon steering state for custom flight routines. Vanilla flight remains untouched whenever
 * no routine is active.
 */
public final class DragonMovementController {
    private static final Map<EnderDragon, DragonMovementController> CONTROLLERS = new WeakHashMap<>();
    private static final double CUSTOM_TARGET_STEP = 4.0;
    private static final float MAX_TURN_RESPONSIVENESS = 0.50F;
    private static final float MAX_DIRECT_TURN_RESPONSIVENESS = 1.0F;
    private static final double VERTICAL_ACCELERATION_SCALE = 0.01;
    private static final float TURN_CHANGE_PER_TICK = 0.04F;
    private static final double THROTTLE_CHANGE_PER_TICK = 0.12;
    private static final double THROTTLE_BRAKING_PER_TICK = 0.24;

    private Vec3 smoothedTarget;
    private FlightCommand activeCommand;
    private FlightRoutine routine;
    private float smoothedTurnResponsiveness;
    private double smoothedThrottle;
    private boolean turnResponsivenessInitialized;
    private boolean throttleInitialized;

    private DragonMovementController() {
    }

    public static synchronized DragonMovementController forDragon(EnderDragon dragon) {
        return CONTROLLERS.computeIfAbsent(dragon, ignored -> new DragonMovementController());
    }

    public static synchronized boolean startRoutine(EnderDragon dragon, FlightRoutine routine) {
        Objects.requireNonNull(routine, "routine");
        DragonPhaseInstance phase = dragon.getPhaseManager().getCurrentPhase();
        if (dragon.level().isClientSide || dragon.isDeadOrDying() || requiresVanillaControl(phase)) {
            return false;
        }

        DragonMovementController controller = forDragon(dragon);
        if (controller.routine != null) {
            return false;
        }

        controller.clearRoutineState(dragon);
        controller.routine = routine;
        return true;
    }

    public static synchronized boolean stopRoutine(EnderDragon dragon) {
        Objects.requireNonNull(dragon, "dragon");
        if (dragon.level().isClientSide) {
            return false;
        }

        DragonMovementController controller = CONTROLLERS.get(dragon);
        if (controller == null || controller.routine == null) {
            return false;
        }

        controller.clearRoutineState(dragon);
        return true;
    }

    public Vec3 resolveTarget(EnderDragon dragon, DragonPhaseInstance phase, Vec3 vanillaTarget) {
        if (this.requiresVanillaControl(phase)) {
            this.clearRoutineState(dragon);
            return vanillaTarget;
        }

        if (this.routine == null) {
            this.clearRoutineState(dragon);
            return vanillaTarget;
        }

        FlightCommand command = this.routine.tick(dragon);
        if (command == null) {
            this.clearRoutineState(dragon);
            return vanillaTarget;
        }

        this.activeCommand = command;
        float ascentPitch = command.usesDirectVelocity()
            ? DragonFlightVisualMath.ascentPitch(toFlightVector(command.desiredVelocity()))
            : 0.0F;
        DragonFlightVisualState.setAscentPitch(dragon, ascentPitch);
        Vec3 alignedTarget = this.alignTargetToFlightTrend(dragon, command.target());
        if (command.usesDirectVelocity()) {
            this.smoothedTarget = alignedTarget;
        } else if (this.smoothedTarget == null) {
            this.smoothedTarget = alignedTarget;
        } else {
            Vec3 difference = alignedTarget.subtract(this.smoothedTarget);
            double scale = FlightMath.stepScale(difference.x, difference.y, difference.z, CUSTOM_TARGET_STEP);
            this.smoothedTarget = this.smoothedTarget.add(difference.scale(scale));
        }

        this.smoothedTarget = this.alignTargetToFlightTrend(dragon, this.smoothedTarget);
        return this.smoothedTarget;
    }

    public Vec3 adjustVerticalMovement(EnderDragon dragon, Vec3 vanillaMovement) {
        if (this.routine == null || this.activeCommand == null || this.smoothedTarget == null) {
            return vanillaMovement;
        }
        if (this.activeCommand.usesDirectVelocity()) {
            return dragon.getDeltaMovement();
        }

        double maxAcceleration = FlightMath.clamp(this.activeCommand.verticalAcceleration(), 0.0, 8.0)
            * VERTICAL_ACCELERATION_SCALE;
        double controlledVerticalVelocity = FlightMath.verticalVelocityBeforeDrag(
            dragon.getDeltaMovement().y,
            this.smoothedTarget.y - dragon.getY(),
            maxAcceleration
        );
        return new Vec3(vanillaMovement.x, controlledVerticalVelocity, vanillaMovement.z);
    }

    public float resolveTurnResponsiveness(float vanillaValue) {
        if (this.routine == null || this.activeCommand == null) {
            this.turnResponsivenessInitialized = false;
            return vanillaValue;
        }

        float maxTurnResponsiveness = this.activeCommand.usesDirectVelocity()
            ? MAX_DIRECT_TURN_RESPONSIVENESS
            : MAX_TURN_RESPONSIVENESS;
        float target = (float)FlightMath.clamp(
            this.activeCommand.turnResponsiveness(), 0.0, maxTurnResponsiveness
        );
        if (!this.turnResponsivenessInitialized) {
            this.smoothedTurnResponsiveness = target;
            this.turnResponsivenessInitialized = true;
        } else {
            this.smoothedTurnResponsiveness = (float)FlightMath.approach(
                this.smoothedTurnResponsiveness, target, TURN_CHANGE_PER_TICK
            );
        }
        return this.smoothedTurnResponsiveness;
    }

    /** Applies a direct velocity request before vanilla collision-resolved movement and drag. */
    public boolean applyDirectVelocity(EnderDragon dragon) {
        if (this.routine == null || this.activeCommand == null || !this.activeCommand.usesDirectVelocity()) {
            return false;
        }

        FlightVector desiredVelocity = FlightMath.clampLength(
            toFlightVector(this.activeCommand.desiredVelocity()), this.activeCommand.maxSpeed()
        );
        FlightVector limitedPostDragVelocity = FlightMath.approachVector(
            toFlightVector(dragon.getDeltaMovement()), desiredVelocity, this.activeCommand.maxAcceleration()
        );
        FlightVector preDragVelocity = FlightMath.compensateForDragonDrag(limitedPostDragVelocity, dragon.getYRot());
        dragon.setDeltaMovement(toVec3(preDragVelocity));
        return true;
    }

    public float adjustHorizontalAcceleration(EnderDragon dragon, float vanillaAcceleration) {
        if (this.routine == null) {
            this.throttleInitialized = false;
            this.smoothedThrottle = 1.0;
            return vanillaAcceleration;
        }

        if (this.activeCommand == null || this.activeCommand.horizontalCruiseSpeed() == null) {
            if (!this.throttleInitialized) {
                return vanillaAcceleration;
            }

            this.smoothedThrottle = FlightMath.approach(this.smoothedThrottle, 1.0, THROTTLE_CHANGE_PER_TICK);
            if (Math.abs(1.0 - this.smoothedThrottle) < THROTTLE_CHANGE_PER_TICK) {
                this.throttleInitialized = false;
                return vanillaAcceleration;
            }
            return vanillaAcceleration * (float)this.smoothedThrottle;
        }

        double desiredSpeed = this.activeCommand.horizontalCruiseSpeed();
        double currentSpeed = dragon.getDeltaMovement().horizontalDistance();
        double targetThrottle = FlightMath.clamp((desiredSpeed - currentSpeed) * 5.0, 0.0, 1.6);
        if (!this.throttleInitialized) {
            this.smoothedThrottle = 0.0;
            this.throttleInitialized = true;
        }
        double throttleChange = targetThrottle < this.smoothedThrottle
            ? THROTTLE_BRAKING_PER_TICK
            : THROTTLE_CHANGE_PER_TICK;
        this.smoothedThrottle = FlightMath.approach(this.smoothedThrottle, targetThrottle, throttleChange);
        return vanillaAcceleration * (float)this.smoothedThrottle;
    }

    private void clearRoutineState(EnderDragon dragon) {
        this.routine = null;
        this.activeCommand = null;
        this.smoothedTarget = null;
        this.smoothedThrottle = 1.0;
        this.throttleInitialized = false;
        this.turnResponsivenessInitialized = false;
        DragonFlightVisualState.setAscentPitch(dragon, 0.0F);
    }

    private static boolean requiresVanillaControl(DragonPhaseInstance phase) {
        EnderDragonPhase<?> currentPhase = phase.getPhase();
        return phase.isSitting()
            || currentPhase == EnderDragonPhase.DYING
            || currentPhase == EnderDragonPhase.LANDING_APPROACH
            || currentPhase == EnderDragonPhase.LANDING;
    }

    private Vec3 alignTargetToFlightTrend(EnderDragon dragon, Vec3 target) {
        double recentYChange = dragon.getLatencyPos(5, 1.0F)[1] - dragon.getLatencyPos(10, 1.0F)[1];
        double alignedY = FlightMath.alignVerticalTargetY(
            dragon.getY(), target.y, dragon.getDeltaMovement().y, recentYChange
        );
        return new Vec3(target.x, alignedY, target.z);
    }

    private static FlightVector toFlightVector(Vec3 vector) {
        return new FlightVector(vector.x, vector.y, vector.z);
    }

    private static Vec3 toVec3(FlightVector vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
