package dev.nullapex.dragon.movement;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.neoforge.entity.PartEntity;

/** Repositions the main multipart hitboxes to follow the active custom flight pitch. */
public final class DragonFlightHitboxAlignment {
    private static final int HEAD_PART = 0;
    private static final int NECK_PART = 1;
    private static final int BODY_PART = 2;
    private static final double HEAD_FORWARD_DISTANCE = 6.5;
    private static final double NECK_FORWARD_DISTANCE = 5.5;
    private static final double BODY_FORWARD_DISTANCE = 0.5;

    private DragonFlightHitboxAlignment() {
    }

    public static void align(EnderDragon dragon, float pitchDegrees) {
        PartEntity<?>[] parts = dragon.getParts();
        if (parts.length <= BODY_PART) {
            return;
        }

        FlightVector bodyOffset = DragonFlightPoseMath.forwardOffset(
            dragon.getYRot(), pitchDegrees, BODY_FORWARD_DISTANCE
        );
        setPartPosition(parts[BODY_PART], dragon.getX(), dragon.getY(), dragon.getZ(), bodyOffset, 0.0);

        float headYaw = dragon.getYRot() - dragon.yRotA * 0.01F;
        FlightVector neckOffset = DragonFlightPoseMath.forwardOffset(
            headYaw, pitchDegrees, NECK_FORWARD_DISTANCE
        );
        FlightVector headOffset = DragonFlightPoseMath.forwardOffset(
            headYaw, pitchDegrees, HEAD_FORWARD_DISTANCE
        );
        setPartPosition(parts[NECK_PART], dragon.getX(), dragon.getY(), dragon.getZ(), neckOffset, 0.0);
        setPartPosition(parts[HEAD_PART], dragon.getX(), dragon.getY(), dragon.getZ(), headOffset, 0.0);
    }

    private static void setPartPosition(
        PartEntity<?> part,
        double dragonX,
        double dragonY,
        double dragonZ,
        FlightVector offset,
        double baseYOffset
    ) {
        double x = dragonX + offset.x();
        double y = dragonY + baseYOffset + offset.y();
        double z = dragonZ + offset.z();
        part.setPos(x, y, z);
        part.xo = x;
        part.yo = y;
        part.zo = z;
        part.xOld = x;
        part.yOld = y;
        part.zOld = z;
    }
}
