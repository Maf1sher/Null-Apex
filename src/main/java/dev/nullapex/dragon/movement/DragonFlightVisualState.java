package dev.nullapex.dragon.movement;

import dev.nullapex.attachment.ModAttachments;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** Access to the client-synchronized state used by custom flight animation rules. */
public final class DragonFlightVisualState {
    private DragonFlightVisualState() {
    }

    public static boolean isCustomDirectFlight(EnderDragon dragon) {
        return Boolean.TRUE.equals(dragon.getExistingDataOrNull(ModAttachments.DRAGON_DIRECT_FLIGHT));
    }

    public static void setCustomDirectFlight(EnderDragon dragon, boolean active) {
        if (dragon.level().isClientSide) {
            return;
        }

        Boolean current = dragon.getExistingDataOrNull(ModAttachments.DRAGON_DIRECT_FLIGHT);
        if (active) {
            if (!Boolean.TRUE.equals(current)) {
                dragon.setData(ModAttachments.DRAGON_DIRECT_FLIGHT, true);
            }
        } else if (current != null) {
            dragon.removeData(ModAttachments.DRAGON_DIRECT_FLIGHT);
        }
    }
}
