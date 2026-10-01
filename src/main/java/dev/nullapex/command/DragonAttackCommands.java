package dev.nullapex.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.attack.DragonAttackController;
import dev.nullapex.dragon.attack.DragonAttackRegistry;
import dev.nullapex.dragon.attack.DragonAttackStage;
import dev.nullapex.dragon.attack.SwoopAttack;
import dev.nullapex.dragon.fight.DragonFightDirector;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = NullApex.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DragonAttackCommands {
    private DragonAttackCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("nullapex")
                .then(
                    Commands.literal("attack")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> showStatus(context.getSource(), SwoopAttack.DEFINITION.id()))
                        .then(
                            Commands.literal("status")
                                .executes(context -> showStatus(context.getSource(), SwoopAttack.DEFINITION.id()))
                                .then(
                                    Commands.argument("attack", StringArgumentType.word())
                                        .executes(context -> showStatus(
                                            context.getSource(), StringArgumentType.getString(context, "attack")
                                        ))
                                )
                        )
                        .then(
                            Commands.literal("test")
                                .executes(context -> startTest(context.getSource(), SwoopAttack.DEFINITION.id()))
                                .then(
                                    Commands.argument("attack", StringArgumentType.word())
                                        .executes(context -> startTest(
                                            context.getSource(), StringArgumentType.getString(context, "attack")
                                        ))
                                )
                        )
                )
        );
    }

    private static int showStatus(CommandSourceStack source, String attackId) {
        EnderDragon dragon = findDragon(source, source.getLevel());
        if (dragon == null) {
            return 0;
        }

        if (DragonAttackRegistry.find(attackId).isEmpty()) {
            source.sendFailure(Component.literal("No registered attack has ID '" + attackId + "'."));
            return 0;
        }

        DragonAttackController.Status status = DragonAttackController.getStatus(dragon, attackId);
        String activeAttack = status.activeAttackId() == null ? "none" : status.activeAttackId();
        String stageTicks = status.stage() == DragonAttackStage.IDLE
            ? "—"
            : Long.toString(status.remainingStageTicks());
        String message = String.format(
            Locale.ROOT,
            "Dragon attack: %s (attack: %s, stage ticks remaining: %s); %s cooldown: %d ticks.",
            status.stage().displayName(),
            activeAttack,
            stageTicks,
            attackId,
            status.cooldownTicksRemaining()
        );
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int startTest(CommandSourceStack source, String attackId) {
        EnderDragon dragon = findDragon(source, source.getLevel());
        if (dragon == null) {
            return 0;
        }

        DragonAttackController.StartResult result = DragonAttackController.tryStart(dragon, attackId);
        if (result.status() == DragonAttackController.StartStatus.STARTED) {
            source.sendSuccess(
                () -> Component.literal("Started registered dragon attack '" + attackId
                    + "'. Inspect it with /nullapex attack."),
                false
            );
            return 1;
        }

        String reason = switch (result.status()) {
            case CLIENT_SIDE -> "attack lifecycles can only start on the server";
            case DRAGON_DEAD -> "the dragon is dead or dying";
            case NOT_ACTIVE_DRAGON -> "this is not the active dragon for the End fight";
            case ALREADY_ACTIVE -> "another attack lifecycle is already active";
            case COOLDOWN_ACTIVE -> String.format(
                Locale.ROOT,
                "attack '%s' is on cooldown for %d more ticks",
                attackId,
                result.cooldownTicksRemaining()
            );
            case PHASE_NOT_ALLOWED -> "attack '" + attackId + "' is not allowed during phase " + result.fightPhase();
            case UNKNOWN_ATTACK -> "no registered attack has ID '" + attackId + "'";
            case NO_TARGET -> "attack '" + attackId + "' could not find a valid target nearby";
            case BEHAVIOR_UNAVAILABLE -> "attack '" + attackId
                + "' could not start; it was cancelled without starting its cooldown";
            case STARTED -> "";
        };
        source.sendFailure(Component.literal("Cannot start attack '" + attackId + "': " + reason + "."));
        return 0;
    }

    private static EnderDragon findDragon(CommandSourceStack source, ServerLevel level) {
        EnderDragon dragon = DragonFightDirector.getActiveDragon(level);
        if (dragon == null) {
            source.sendFailure(Component.literal(
                "No loaded active Ender Dragon fight was found in this dimension."
            ));
        }
        return dragon;
    }
}
