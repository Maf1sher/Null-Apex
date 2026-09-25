package dev.nullapex.dragon.movement;

import dev.nullapex.attachment.ModAttachments;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** Access to the client-synchronized visual state for custom dragon flight. */
public final class DragonFlightVisualState {
    private DragonFlightVisualState() {
    }

    public static float ascentPitch(EnderDragon dragon) {
        Float pitch = dragon.getExistingDataOrNull(ModAttachments.DRAGON_ASCENT_PITCH);
        return pitch == null ? 0.0F : pitch;
    }

    public static void setAscentPitch(EnderDragon dragon, float pitch) {
        if (dragon.level().isClientSide) {
            return;
        }

        float clampedPitch = DragonFlightVisualMath.clampAscentPitch(pitch);
        Float currentPitch = dragon.getExistingDataOrNull(ModAttachments.DRAGON_ASCENT_PITCH);
        if (clampedPitch == 0.0F) {
            if (currentPitch != null) {
                dragon.removeData(ModAttachments.DRAGON_ASCENT_PITCH);
            }
        } else if (currentPitch == null || Float.compare(currentPitch, clampedPitch) != 0) {
            dragon.setData(ModAttachments.DRAGON_ASCENT_PITCH, clampedPitch);
        }
    }

    public static Float flightPitchDegrees(EnderDragon dragon) {
        return dragon.getExistingDataOrNull(ModAttachments.DRAGON_FLIGHT_PITCH);
    }

    public static void setFlightPitchDegrees(EnderDragon dragon, Float pitchDegrees) {
        if (dragon.level().isClientSide) {
            return;
        }

        Float currentPitch = dragon.getExistingDataOrNull(ModAttachments.DRAGON_FLIGHT_PITCH);
        if (pitchDegrees == null) {
            if (currentPitch != null) {
                dragon.removeData(ModAttachments.DRAGON_FLIGHT_PITCH);
            }
            return;
        }

        float clampedPitch = (float)FlightMath.clamp(pitchDegrees, -90.0, 90.0);
        if (currentPitch == null || Float.compare(currentPitch, clampedPitch) != 0) {
            dragon.setData(ModAttachments.DRAGON_FLIGHT_PITCH, clampedPitch);
        }
    }
}
