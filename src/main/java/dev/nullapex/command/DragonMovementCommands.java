package dev.nullapex.command;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Comparator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.movement.DragonMovementController;
import dev.nullapex.dragon.movement.RoutineStartResult;
import dev.nullapex.dragon.movement.TestDiveRoutine;
import dev.nullapex.dragon.movement.VerticalImpactRoutine;

@EventBusSubscriber(modid = NullApex.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DragonMovementCommands {
    private DragonMovementCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("nullapex")
                .then(
                    Commands.literal("test-dive")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> startTestDive(context.getSource(), null))
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes(context -> startTestDive(context.getSource(), EntityArgument.getPlayer(context, "player")))
                        )
                )
                .then(
                    Commands.literal("vertical-impact")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> startVerticalImpact(context.getSource()))
                )
        );
    }

    private static int startTestDive(CommandSourceStack source, ServerPlayer requestedTarget) {
        ServerLevel level = source.getLevel();
        EnderDragon dragon = findDragon(source, level);
        if (dragon == null) {
            return 0;
        }

        Player target = requestedTarget;
        if (target == null) {
            target = level.players().stream()
                .filter(player -> player.isAlive() && !player.isSpectator())
                .min(Comparator.comparingDouble(dragon::distanceToSqr))
                .orElse(null);
        }

        if (target == null || !target.isAlive() || target.isSpectator() || target.level() != level) {
            source.sendFailure(Component.literal("Choose a living player in the End dimension."));
            return 0;
        }

        RoutineStartResult result = DragonMovementController.tryStartRoutine(dragon, new TestDiveRoutine(target));
        if (result != RoutineStartResult.STARTED) {
            reportStartFailure(source, result, "test dive");
            return 0;
        }

        String targetName = target.getGameProfile().getName();
        source.sendSuccess(() -> Component.literal("Started the Ender Dragon test dive toward " + targetName + "."), false);
        return 1;
    }

    private static int startVerticalImpact(CommandSourceStack source) {
        EnderDragon dragon = findDragon(source, source.getLevel());
        if (dragon == null) {
            return 0;
        }

        if (!VerticalImpactRoutine.canStart(dragon)) {
            source.sendFailure(Component.literal("The dragon needs more vertical clearance from the ground and world ceiling."));
            return 0;
        }

        RoutineStartResult result = DragonMovementController.tryStartRoutine(dragon, new VerticalImpactRoutine());
        if (result != RoutineStartResult.STARTED) {
            reportStartFailure(source, result, "vertical impact flight");
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Started the Ender Dragon vertical impact flight."), false);
        return 1;
    }

    private static void reportStartFailure(
        CommandSourceStack source,
        RoutineStartResult result,
        String routineName
    ) {
        String reason = switch (result) {
            case CLIENT_SIDE -> "movement routines can only start on the server";
            case DRAGON_DEAD -> "the dragon is dead or dying";
            case PROTECTED_PHASE -> "the dragon is in a protected vanilla phase";
            case ALREADY_ACTIVE -> "another movement routine is already active";
            case INITIALIZATION_FAILED -> "the routine failed during initialization";
            case STARTED -> "";
        };
        source.sendFailure(Component.literal("Cannot start " + routineName + ": " + reason + "."));
    }

    private static EnderDragon findDragon(CommandSourceStack source, ServerLevel level) {
        EndDragonFight fight = level.getDragonFight();
        if (fight == null || fight.getDragonUUID() == null) {
            source.sendFailure(Component.literal("No active Ender Dragon fight was found in this dimension."));
            return null;
        }

        Entity entity = level.getEntity(fight.getDragonUUID());
        if (!(entity instanceof EnderDragon dragon)) {
            source.sendFailure(Component.literal("The Ender Dragon is not currently loaded."));
            return null;
        }
        return dragon;
    }
}
