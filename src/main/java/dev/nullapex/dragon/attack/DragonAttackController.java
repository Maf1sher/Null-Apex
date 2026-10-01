package dev.nullapex.dragon.attack;

import dev.nullapex.attachment.ModAttachments;
import dev.nullapex.dragon.effect.DragonEffectScope;
import dev.nullapex.dragon.fight.DragonFightDirector;
import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-side attack lifecycle state for one dragon. */
public final class DragonAttackController {
    private static final Logger LOGGER = LoggerFactory.getLogger("null_apex");

    private final DragonAttackLifecycle lifecycle = new DragonAttackLifecycle();
    private final DragonAttackSelectionSchedule selectionSchedule = new DragonAttackSelectionSchedule();
    private DragonAttackExecution activeExecution;
    private DragonEffectScope activeEffectScope;

    private DragonAttackController() {
    }

    public static StartResult tryStart(EnderDragon dragon, String attackId) {
        Objects.requireNonNull(dragon, "dragon");
        Objects.requireNonNull(attackId, "attackId");

        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return new StartResult(StartStatus.CLIENT_SIDE, 0L, null);
        }
        if (dragon.isDeadOrDying()) {
            return new StartResult(StartStatus.DRAGON_DEAD, 0L, null);
        }
        if (!DragonFightDirector.isActiveDragon(dragon)) {
            return new StartResult(StartStatus.NOT_ACTIVE_DRAGON, 0L, null);
        }

        DragonAttack attack = DragonAttackRegistry.find(attackId).orElse(null);
        if (attack == null) {
            return new StartResult(StartStatus.UNKNOWN_ATTACK, 0L, DragonFightDirector.getCurrentPhase(dragon));
        }

        DragonFightPhase fightPhase = DragonFightDirector.getCurrentPhase(dragon);
        DragonAttackController controller = existingForDragon(dragon);
        if (controller != null && controller.lifecycle.stage() != DragonAttackStage.IDLE) {
            return new StartResult(StartStatus.ALREADY_ACTIVE, 0L, fightPhase);
        }
        DragonAttackDefinition definition = attack.definition();
        if (!definition.allowedFightPhases().contains(fightPhase)) {
            return new StartResult(StartStatus.PHASE_NOT_ALLOWED, 0L, fightPhase);
        }

        long gameTime = serverLevel.getGameTime();
        Map<String, Long> cooldowns = dragon.getData(ModAttachments.DRAGON_ATTACK_COOLDOWNS);
        long cooldownRemaining = DragonAttackCooldowns.remainingTicks(cooldowns, definition.id(), gameTime);
        if (cooldownRemaining > 0L) {
            return new StartResult(StartStatus.COOLDOWN_ACTIVE, cooldownRemaining, fightPhase);
        }

        DragonAttackExecution execution = attack.createExecution(dragon);
        if (execution == null) {
            return new StartResult(StartStatus.NO_TARGET, 0L, fightPhase);
        }

        if (controller == null) {
            controller = forDragon(dragon);
        }
        DragonAttackLifecycle.StartResult lifecycleResult = controller.lifecycle.tryStart(definition, fightPhase);
        if (lifecycleResult != DragonAttackLifecycle.StartResult.STARTED) {
            StartStatus status = lifecycleResult == DragonAttackLifecycle.StartResult.ALREADY_ACTIVE
                ? StartStatus.ALREADY_ACTIVE
                : StartStatus.PHASE_NOT_ALLOWED;
            return new StartResult(status, 0L, fightPhase);
        }

        controller.activeExecution = execution;
        DragonEffectScope effectScope = new DragonEffectScope(serverLevel);
        controller.activeEffectScope = effectScope;
        DragonAttackController owner = controller;
        boolean behaviorStarted;
        try {
            behaviorStarted = execution.tryStart(
                dragon,
                effectScope,
                reason -> owner.onBehaviorEnded(dragon, execution, reason)
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Dragon attack '{}' failed during startup", attackId, exception);
            behaviorStarted = false;
        }

        if (!behaviorStarted || controller.activeExecution != execution) {
            controller.finishActive(dragon, DragonAttackEndReason.FAILED);
            return new StartResult(StartStatus.BEHAVIOR_UNAVAILABLE, 0L, fightPhase);
        }

        if (definition.cooldownTicks() > 0) {
            dragon.setData(
                ModAttachments.DRAGON_ATTACK_COOLDOWNS,
                DragonAttackCooldowns.startCooldown(cooldowns, definition.id(), gameTime, definition.cooldownTicks())
            );
        }
        controller.selectionSchedule.onAttackStarted(
            fightPhase, gameTime, randomIntervalTicks(dragon, DragonAttackSelectionTiming.attackInterval(fightPhase))
        );
        return new StartResult(StartStatus.STARTED, 0L, fightPhase);
    }

    public static Status getStatus(EnderDragon dragon, String inspectedAttackId) {
        Objects.requireNonNull(dragon, "dragon");
        Objects.requireNonNull(inspectedAttackId, "inspectedAttackId");
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Dragon attack state is server-authoritative");
        }

        DragonAttackController controller = existingForDragon(dragon);
        DragonAttackLifecycle lifecycle = controller == null ? null : controller.lifecycle;
        long cooldownRemaining = DragonAttackCooldowns.remainingTicks(
            dragon.getData(ModAttachments.DRAGON_ATTACK_COOLDOWNS),
            inspectedAttackId,
            serverLevel.getGameTime()
        );
        return new Status(
            lifecycle == null ? DragonAttackStage.IDLE : lifecycle.stage(),
            lifecycle == null ? null : lifecycle.activeAttackId(),
            lifecycle == null ? 0 : lifecycle.remainingStageTicks(),
            cooldownRemaining
        );
    }

    public static boolean isIdle(EnderDragon dragon) {
        Objects.requireNonNull(dragon, "dragon");
        if (!(dragon.level() instanceof ServerLevel)) {
            return false;
        }

        DragonAttackController controller = existingForDragon(dragon);
        return controller == null || controller.lifecycle.stage() == DragonAttackStage.IDLE;
    }

    static boolean isAutomaticSelectionDue(EnderDragon dragon, DragonFightPhase phase, long gameTime) {
        DragonAttackController controller = forDragon(dragon);
        return controller.selectionSchedule.isDue(
            phase,
            gameTime,
            () -> randomIntervalTicks(dragon, DragonAttackSelectionTiming.attackInterval(phase))
        );
    }

    static void deferAutomaticSelectionUntil(EnderDragon dragon, long gameTime) {
        forDragon(dragon).selectionSchedule.deferUntil(gameTime);
    }

    static void deferAutomaticSelectionUntilPhaseChange(EnderDragon dragon) {
        forDragon(dragon).selectionSchedule.deferForPhaseChange();
    }

    static void retryAutomaticSelection(EnderDragon dragon, long gameTime) {
        DragonAttackSelectionTiming.TickRange retryRange = DragonAttackSelectionTiming.retryInterval();
        int retryTicks = randomIntervalTicks(dragon, retryRange);
        forDragon(dragon).selectionSchedule.retryAt(gameTime, retryTicks);
    }

    private static int randomIntervalTicks(EnderDragon dragon, DragonAttackSelectionTiming.TickRange range) {
        return range.ticksForOffset(dragon.getRandom().nextInt(range.size()));
    }

    public static void tick(EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel)) {
            return;
        }

        DragonAttackController controller = existingForDragon(dragon);
        if (controller == null) {
            return;
        }
        if (dragon.isDeadOrDying() || !DragonFightDirector.isActiveDragon(dragon)) {
            controller.finishActive(dragon, DragonAttackEndReason.CANCELLED);
            return;
        }

        DragonAttackExecution execution = controller.activeExecution;
        if (execution == null) {
            controller.finishActive(dragon, DragonAttackEndReason.CANCELLED);
            return;
        }

        controller.lifecycle.tick();
        DragonAttackStage stage = controller.lifecycle.stage();
        if (stage == DragonAttackStage.IDLE) {
            controller.finishActive(dragon, DragonAttackEndReason.COMPLETED);
            return;
        }

        try {
            execution.tick(dragon, stage);
        } catch (RuntimeException exception) {
            LOGGER.error("Dragon attack '{}' failed while ticking", controller.lifecycle.activeAttackId(), exception);
            controller.finishActive(dragon, DragonAttackEndReason.FAILED);
        }
    }

    private static DragonAttackController forDragon(EnderDragon dragon) {
        DragonAttackControllerAccess access = controllerAccess(dragon);
        DragonAttackController controller = access.nullApex$getAttackController();
        if (controller == null) {
            controller = new DragonAttackController();
            access.nullApex$setAttackController(controller);
        }
        return controller;
    }

    private static DragonAttackController existingForDragon(EnderDragon dragon) {
        return controllerAccess(dragon).nullApex$getAttackController();
    }

    private static DragonAttackControllerAccess controllerAccess(EnderDragon dragon) {
        return (DragonAttackControllerAccess)(Object)dragon;
    }

    private void onBehaviorEnded(
        EnderDragon dragon,
        DragonAttackExecution execution,
        DragonAttackEndReason reason
    ) {
        if (this.activeExecution == execution) {
            this.finishActive(dragon, reason);
        }
    }

    private void finishActive(EnderDragon dragon, DragonAttackEndReason reason) {
        DragonAttackExecution execution = this.activeExecution;
        DragonEffectScope effectScope = this.activeEffectScope;
        this.activeExecution = null;
        this.activeEffectScope = null;
        this.lifecycle.cancel();
        try {
            if (execution != null) {
                execution.stop(dragon, reason);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Dragon attack cleanup failed ({})", reason, exception);
        } finally {
            if (effectScope != null) {
                effectScope.close();
            }
        }
    }

    public enum StartStatus {
        STARTED,
        CLIENT_SIDE,
        DRAGON_DEAD,
        NOT_ACTIVE_DRAGON,
        ALREADY_ACTIVE,
        COOLDOWN_ACTIVE,
        PHASE_NOT_ALLOWED,
        UNKNOWN_ATTACK,
        NO_TARGET,
        BEHAVIOR_UNAVAILABLE
    }

    public record StartResult(StartStatus status, long cooldownTicksRemaining, DragonFightPhase fightPhase) {
    }

    public record Status(
        DragonAttackStage stage,
        String activeAttackId,
        int remainingStageTicks,
        long cooldownTicksRemaining
    ) {
    }
}
