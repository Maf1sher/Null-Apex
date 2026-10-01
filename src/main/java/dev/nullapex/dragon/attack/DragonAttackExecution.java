package dev.nullapex.dragon.attack;

import dev.nullapex.dragon.effect.DragonEffectScope;
import java.util.function.Consumer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** Mutable state belonging to one running attack, never shared through the registry. */
public interface DragonAttackExecution {
    /**
     * Starts attack-specific behavior; a failed start must leave no active behavior behind.
     * Retain the per-execution effect scope when later ticks need to create or stop effects.
     * The controller closes the scope after every stop path, including failed starts.
     */
    boolean tryStart(
        EnderDragon dragon,
        DragonEffectScope effects,
        Consumer<DragonAttackEndReason> behaviorEnded
    );

    /** Runs once per server entity tick while the attack lifecycle is active. */
    void tick(EnderDragon dragon, DragonAttackStage stage);

    /** Stops all attack-specific behavior after completion, cancellation, or failure. */
    void stop(EnderDragon dragon, DragonAttackEndReason reason);
}
