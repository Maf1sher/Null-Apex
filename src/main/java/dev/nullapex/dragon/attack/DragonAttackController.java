package dev.nullapex.dragon.attack;

import dev.nullapex.attachment.ModAttachments;
import dev.nullapex.dragon.fight.DragonFightDirector;
import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** Server-side attack lifecycle state for one dragon. */
public final class DragonAttackController {
    private final DragonAttackLifecycle lifecycle = new DragonAttackLifecycle();

    private DragonAttackController() {
    }

    public static StartResult tryStart(EnderDragon dragon, DragonAttackDefinition definition) {
        Objects.requireNonNull(dragon, "dragon");
        Objects.requireNonNull(definition, "definition");

        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return new StartResult(StartStatus.CLIENT_SIDE, 0L, null);
        }
        if (dragon.isDeadOrDying()) {
            return new StartResult(StartStatus.DRAGON_DEAD, 0L, null);
        }
        if (!DragonFightDirector.isActiveDragon(dragon)) {
            return new StartResult(StartStatus.NOT_ACTIVE_DRAGON, 0L, null);
        }

        DragonFightPhase fightPhase = DragonFightDirector.getCurrentPhase(dragon);
        DragonAttackController controller = existingForDragon(dragon);
        if (controller != null && controller.lifecycle.stage() != DragonAttackStage.IDLE) {
            return new StartResult(StartStatus.ALREADY_ACTIVE, 0L, fightPhase);
        }
        if (!definition.allowedFightPhases().contains(fightPhase)) {
            return new StartResult(StartStatus.PHASE_NOT_ALLOWED, 0L, fightPhase);
        }

        long gameTime = serverLevel.getGameTime();
        Map<String, Long> cooldowns = dragon.getData(ModAttachments.DRAGON_ATTACK_COOLDOWNS);
        long cooldownRemaining = DragonAttackCooldowns.remainingTicks(cooldowns, definition.id(), gameTime);
        if (cooldownRemaining > 0L) {
            return new StartResult(StartStatus.COOLDOWN_ACTIVE, cooldownRemaining, fightPhase);
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

        if (definition.cooldownTicks() > 0) {
            dragon.setData(
                ModAttachments.DRAGON_ATTACK_COOLDOWNS,
                DragonAttackCooldowns.startCooldown(cooldowns, definition.id(), gameTime, definition.cooldownTicks())
            );
        }
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

    public static void tick(EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel)) {
            return;
        }

        DragonAttackController controller = existingForDragon(dragon);
        if (controller == null) {
            return;
        }
        if (dragon.isDeadOrDying() || !DragonFightDirector.isActiveDragon(dragon)) {
            controller.lifecycle.cancel();
            return;
        }
        controller.lifecycle.tick();
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

    public enum StartStatus {
        STARTED,
        CLIENT_SIDE,
        DRAGON_DEAD,
        NOT_ACTIVE_DRAGON,
        ALREADY_ACTIVE,
        COOLDOWN_ACTIVE,
        PHASE_NOT_ALLOWED
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
