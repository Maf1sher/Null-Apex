package dev.nullapex.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.fight.DragonFightDirector;
import dev.nullapex.dragon.fight.DragonFightPhase;
import dev.nullapex.dragon.fight.HealthThresholdPhasePolicy;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = NullApex.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DragonFightCommands {
    private DragonFightCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("nullapex")
                .then(
                    Commands.literal("phase")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> showPhase(context.getSource()))
                )
        );
    }

    private static int showPhase(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        EndDragonFight fight = level.getDragonFight();
        UUID dragonId = fight == null ? null : fight.getDragonUUID();
        if (dragonId == null) {
            source.sendFailure(Component.literal("No active Ender Dragon fight was found in this dimension."));
            return 0;
        }

        Entity entity = level.getEntity(dragonId);
        if (!(entity instanceof EnderDragon dragon)) {
            source.sendFailure(Component.literal("The active Ender Dragon is not currently loaded."));
            return 0;
        }

        DragonFightPhase phase = DragonFightDirector.getCurrentPhase(dragon);
        double maxHealth = dragon.getMaxHealth();
        double health = dragon.getHealth();
        double healthPercent = maxHealth > 0.0 ? health / maxHealth * 100.0 : 0.0;
        String nextThreshold = switch (phase) {
            case OPENING -> String.format(
                Locale.ROOT,
                "next: ESCALATION at or below %.0f%% health",
                HealthThresholdPhasePolicy.ESCALATION_HEALTH_RATIO * 100.0
            );
            case ESCALATION -> String.format(
                Locale.ROOT,
                "next: FINAL at or below %.0f%% health",
                HealthThresholdPhasePolicy.FINAL_HEALTH_RATIO * 100.0
            );
            case FINAL -> "no further phase transition";
        };
        String message = String.format(
            Locale.ROOT,
            "Dragon fight phase: %s — %.1f / %.1f HP (%.1f%%); %s.",
            phase,
            health,
            maxHealth,
            healthPercent,
            nextThreshold
        );
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
