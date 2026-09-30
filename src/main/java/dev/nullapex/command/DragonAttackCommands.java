package dev.nullapex.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.nullapex.NullApex;
import dev.nullapex.dragon.attack.DragonAttackController;
import dev.nullapex.dragon.attack.DragonAttackDefinitions;
import dev.nullapex.dragon.attack.DragonAttackStage;
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
                        .executes(context -> showStatus(context.getSource()))
                        .then(Commands.literal("status").executes(context -> showStatus(context.getSource())))
                        .then(Commands.literal("test").executes(context -> startTest(context.getSource())))
                )
        );
    }

    private static int showStatus(CommandSourceStack source) {
        EnderDragon dragon = findDragon(source, source.getLevel());
        if (dragon == null) {
            return 0;
        }

        DragonAttackController.Status status = DragonAttackController.getStatus(
            dragon,
            DragonAttackDefinitions.LIFECYCLE_TEST.id()
        );
        String activeAttack = status.activeAttackId() == null ? "none" : status.activeAttackId();
        String stageTicks = status.stage() == DragonAttackStage.IDLE
            ? "—"
            : Long.toString(status.remainingStageTicks());
        String message = String.format(
            Locale.ROOT,
            "Dragon attack: %s (attack: %s, stage ticks remaining: %s); lifecycle-test cooldown: %d ticks.",
            status.stage().displayName(),
            activeAttack,
            stageTicks,
            status.cooldownTicksRemaining()
        );
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int startTest(CommandSourceStack source) {
        EnderDragon dragon = findDragon(source, source.getLevel());
        if (dragon == null) {
            return 0;
        }

        DragonAttackController.StartResult result = DragonAttackController.tryStart(
            dragon,
            DragonAttackDefinitions.LIFECYCLE_TEST
        );
        if (result.status() == DragonAttackController.StartStatus.STARTED) {
            source.sendSuccess(
                () -> Component.literal(
                    "Started the harmless attack lifecycle test (40-tick windup, 20-tick active stage, "
                        + "20-tick recovery, 200-tick cooldown). Inspect it with /nullapex attack."
                ),
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
                "the test attack is on cooldown for %d more ticks",
                result.cooldownTicksRemaining()
            );
            case PHASE_NOT_ALLOWED -> "the test attack is not allowed during phase " + result.fightPhase();
            case STARTED -> "";
        };
        source.sendFailure(Component.literal("Cannot start the attack lifecycle test: " + reason + "."));
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
