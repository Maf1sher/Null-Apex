package dev.nullapex.dragon.movement;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-dragon steering state for custom flight routines. Vanilla flight remains untouched whenever
 * no routine is active.
 */
public final class DragonMovementController {
    private static final Logger LOGGER = LoggerFactory.getLogger("null_apex");
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

    static synchronized DragonMovementController forDragon(EnderDragon dragon) {
        Objects.requireNonNull(dragon, "dragon");
        return CONTROLLERS.computeIfAbsent(dragon, ignored -> new DragonMovementController());
    }

    private static synchronized DragonMovementController existingForDragon(EnderDragon dragon) {
        return CONTROLLERS.get(dragon);
    }

    /** Starts a routine without replacing an existing routine. */
    public static boolean startRoutine(EnderDragon dragon, FlightRoutine routine) {
        return tryStartRoutine(dragon, routine) == RoutineStartResult.STARTED;
    }

    /** Starts a routine and reports why it was rejected, if it could not be started. */
    public static RoutineStartResult tryStartRoutine(EnderDragon dragon, FlightRoutine routine) {
        Objects.requireNonNull(dragon, "dragon");
        Objects.requireNonNull(routine, "routine");

        if (dragon.level().isClientSide) {
            return RoutineStartResult.CLIENT_SIDE;
        }
        if (dragon.isDeadOrDying()) {
            return RoutineStartResult.DRAGON_DEAD;
        }

        DragonPhaseInstance phase = dragon.getPhaseManager().getCurrentPhase();
        if (requiresVanillaControl(phase)) {
            return RoutineStartResult.PROTECTED_PHASE;
        }

        DragonMovementController controller = forDragon(dragon);
        if (controller.routine != null) {
            return RoutineStartResult.ALREADY_ACTIVE;
        }

        controller.clearMovementState(dragon);
        controller.routine = routine;
        try {
            routine.onStart(dragon);
        } catch (RuntimeException exception) {
            controller.finishRoutine(dragon, FlightRoutineEndReason.FAILED);
            LOGGER.error("Dragon flight routine failed during initialization", exception);
            return RoutineStartResult.INITIALIZATION_FAILED;
        }
        return RoutineStartResult.STARTED;
    }

    public static boolean stopRoutine(EnderDragon dragon) {
        Objects.requireNonNull(dragon, "dragon");
        if (dragon.level().isClientSide) {
            return false;
        }

        DragonMovementController controller = existingForDragon(dragon);
        if (controller == null || controller.routine == null) {
            return false;
        }

        controller.finishRoutine(dragon, FlightRoutineEndReason.CANCELLED);
        return true;
    }

    /** Internal bridge for the EnderDragon mixin; integrations should use the routine lifecycle API. */
    public static Vec3 mixinResolveTarget(EnderDragon dragon, DragonPhaseInstance phase, Vec3 vanillaTarget) {
        return forDragon(dragon).resolveTarget(dragon, phase, vanillaTarget);
    }

    /** Internal bridge for the EnderDragon mixin; integrations should use the routine lifecycle API. */
    public static Vec3 mixinAdjustVerticalMovement(EnderDragon dragon, Vec3 vanillaMovement) {
        return forDragon(dragon).adjustVerticalMovement(dragon, vanillaMovement);
    }

    /** Internal bridge for the EnderDragon mixin; integrations should use the routine lifecycle API. */
    public static float mixinResolveTurnResponsiveness(EnderDragon dragon, float vanillaValue) {
        return forDragon(dragon).resolveTurnResponsiveness(vanillaValue);
    }

    /** Internal bridge for the EnderDragon mixin; integrations should use the routine lifecycle API. */
    public static boolean mixinApplyDirectVelocity(EnderDragon dragon) {
        return forDragon(dragon).applyDirectVelocity(dragon);
    }

    /** Internal bridge for the EnderDragon mixin; integrations should use the routine lifecycle API. */
    public static float mixinAdjustHorizontalAcceleration(EnderDragon dragon, float vanillaAcceleration) {
        return forDragon(dragon).adjustHorizontalAcceleration(dragon, vanillaAcceleration);
    }

    Vec3 resolveTarget(EnderDragon dragon, DragonPhaseInstance phase, Vec3 vanillaTarget) {
        FlightRoutine activeRoutine = this.routine;
        if (activeRoutine == null) {
            this.clearMovementState(dragon);
            return vanillaTarget;
        }

        FlightRoutineResult result;
        try {
            DragonMovementPhase movementPhase = movementPhase(phase);
            if (movementPhase == DragonMovementPhase.DYING
                || (movementPhase.isProtected() && FlightRoutinePhasePolicy.shouldYieldControl(
                    movementPhase, activeRoutine.canContinueDuring(movementPhase)
                ))) {
                this.finishRoutine(dragon, FlightRoutineEndReason.PHASE_TAKEOVER);
                return this.vanillaTargetForCurrentPhase(dragon, phase, vanillaTarget);
            }

            if (activeRoutine instanceof ManagedFlightRoutine managedRoutine) {
                result = Objects.requireNonNull(managedRoutine.tickResult(dragon), "routine result");
            } else {
                FlightCommand legacyCommand = activeRoutine.tick(dragon);
                result = legacyCommand == null
                    ? FlightRoutineResult.complete()
                    : FlightRoutineResult.command(legacyCommand);
            }
        } catch (RuntimeException exception) {
            this.finishRoutine(dragon, FlightRoutineEndReason.FAILED);
            LOGGER.error("Dragon flight routine failed while ticking", exception);
            return this.vanillaTargetForCurrentPhase(dragon, phase, vanillaTarget);
        }

        if (result instanceof FlightRoutineResult.Complete) {
            this.finishRoutine(dragon, FlightRoutineEndReason.COMPLETED);
            return this.vanillaTargetForCurrentPhase(dragon, phase, vanillaTarget);
        }
        if (result instanceof FlightRoutineResult.Pause) {
            this.clearMovementState(dragon);
            return this.vanillaTargetForCurrentPhase(dragon, phase, vanillaTarget);
        }

        FlightCommand command = ((FlightRoutineResult.Command)result).command();

        this.activeCommand = command;
        DragonFlightVisualState.setCustomDirectFlight(dragon, command.usesDirectVelocity());
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

    Vec3 adjustVerticalMovement(EnderDragon dragon, Vec3 vanillaMovement) {
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

    float resolveTurnResponsiveness(float vanillaValue) {
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
    boolean applyDirectVelocity(EnderDragon dragon) {
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

    float adjustHorizontalAcceleration(EnderDragon dragon, float vanillaAcceleration) {
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

    private void clearMovementState(EnderDragon dragon) {
        this.activeCommand = null;
        this.smoothedTarget = null;
        this.smoothedThrottle = 1.0;
        this.throttleInitialized = false;
        this.turnResponsivenessInitialized = false;
        DragonFlightVisualState.setCustomDirectFlight(dragon, false);
    }

    private void finishRoutine(EnderDragon dragon, FlightRoutineEndReason reason) {
        FlightRoutine finishedRoutine = this.routine;
        this.routine = null;
        this.clearMovementState(dragon);
        if (finishedRoutine == null) {
            return;
        }

        try {
            finishedRoutine.onStop(dragon, reason);
        } catch (RuntimeException exception) {
            LOGGER.error("Dragon flight routine failed during shutdown ({})", reason, exception);
        }
    }

    private static boolean requiresVanillaControl(DragonPhaseInstance phase) {
        return movementPhase(phase).isProtected();
    }

    private static DragonMovementPhase movementPhase(DragonPhaseInstance phase) {
        EnderDragonPhase<?> currentPhase = phase.getPhase();
        if (currentPhase == EnderDragonPhase.DYING) {
            return DragonMovementPhase.DYING;
        }
        if (currentPhase == EnderDragonPhase.LANDING_APPROACH) {
            return DragonMovementPhase.LANDING_APPROACH;
        }
        if (currentPhase == EnderDragonPhase.LANDING) {
            return DragonMovementPhase.LANDING;
        }
        return phase.isSitting() ? DragonMovementPhase.SITTING : DragonMovementPhase.NORMAL;
    }

    private Vec3 vanillaTargetForCurrentPhase(EnderDragon dragon, DragonPhaseInstance originalPhase, Vec3 vanillaTarget) {
        DragonPhaseInstance currentPhase = dragon.getPhaseManager().getCurrentPhase();
        return currentPhase == originalPhase ? vanillaTarget : currentPhase.getFlyTargetLocation();
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
