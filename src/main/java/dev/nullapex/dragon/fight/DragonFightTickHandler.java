package dev.nullapex.dragon.fight;

import dev.nullapex.NullApex;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = NullApex.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DragonFightTickHandler {
    private DragonFightTickHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof EnderDragon dragon) {
            DragonFightDirector.tick(dragon);
        }
    }
}
