package dev.nullapex.dragon.attack;

import dev.nullapex.attachment.ModAttachments;
import dev.nullapex.dragon.fight.DragonFightDirector;
import dev.nullapex.dragon.fight.DragonFightPhase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** Starts an eligible registered attack when that dragon's randomized interval expires. */
public final class DragonAttackSelector {
    private DragonAttackSelector() {
    }

    public static void tick(EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)
            || !DragonFightDirector.isActiveDragon(dragon)) {
            return;
        }

        long gameTime = serverLevel.getGameTime();
        if (!DragonAttackController.isIdle(dragon)) {
            return;
        }

        DragonFightPhase phase = DragonFightDirector.getCurrentPhase(dragon);
        if (!DragonAttackController.isAutomaticSelectionDue(dragon, phase, gameTime)) {
            return;
        }

        List<DragonAttackDefinition> phaseEligible = phaseEligibleAttackDefinitions(
            DragonAttackRegistry.all().values().stream().map(DragonAttack::definition).toList(), phase
        );
        if (phaseEligible.isEmpty()) {
            DragonAttackController.deferAutomaticSelectionUntilPhaseChange(dragon);
            return;
        }

        Map<String, Long> cooldowns = dragon.getData(ModAttachments.DRAGON_ATTACK_COOLDOWNS);
        List<DragonAttackDefinition> candidates = new ArrayList<>(eligibleAttackDefinitions(
            phaseEligible, cooldowns, gameTime
        ));
        if (candidates.isEmpty()) {
            long earliestCooldownDeadline = phaseEligible.stream()
                .mapToLong(definition -> cooldowns.getOrDefault(definition.id(), Long.MIN_VALUE))
                .filter(deadline -> deadline > gameTime)
                .min()
                .orElseThrow();
            DragonAttackController.deferAutomaticSelectionUntil(dragon, earliestCooldownDeadline);
            return;
        }

        shuffle(candidates, dragon);

        for (DragonAttackDefinition candidate : candidates) {
            DragonAttackController.StartResult result = DragonAttackController.tryStart(dragon, candidate.id());
            if (result.status() == DragonAttackController.StartStatus.STARTED
                || result.status() == DragonAttackController.StartStatus.ALREADY_ACTIVE) {
                return;
            }
        }

        DragonAttackController.retryAutomaticSelection(dragon, gameTime);
    }

    static List<DragonAttackDefinition> phaseEligibleAttackDefinitions(
        Collection<DragonAttackDefinition> definitions,
        DragonFightPhase phase
    ) {
        return definitions.stream()
            .filter(definition -> definition.allowedFightPhases().contains(phase))
            .toList();
    }

    static List<DragonAttackDefinition> eligibleAttackDefinitions(
        Collection<DragonAttackDefinition> definitions,
        Map<String, Long> cooldownDeadlines,
        long gameTime
    ) {
        return definitions.stream()
            .filter(definition -> DragonAttackCooldowns.remainingTicks(
                cooldownDeadlines, definition.id(), gameTime
            ) == 0L)
            .toList();
    }

    private static void shuffle(List<DragonAttackDefinition> definitions, EnderDragon dragon) {
        for (int index = definitions.size() - 1; index > 0; index--) {
            Collections.swap(definitions, index, dragon.getRandom().nextInt(index + 1));
        }
    }
}
