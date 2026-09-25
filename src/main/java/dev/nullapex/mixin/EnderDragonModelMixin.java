package dev.nullapex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nullapex.dragon.movement.DragonFlightVisualState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.entity.EnderDragonRenderer$DragonModel")
abstract class EnderDragonModelMixin {
    @Shadow
    @Final
    private ModelPart head;

    @Shadow
    @Final
    private ModelPart neck;

    @Unique
    private float nullApex$ascentPitch;

    @Unique
    private int nullApex$entityId = Integer.MIN_VALUE;

    @Unique
    private int nullApex$neckSegmentIndex;

    @Inject(method = "prepareMobModel", at = @At("TAIL"))
    private void nullApex$updateAscentPitch(
        EnderDragon dragon,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        CallbackInfo callbackInfo
    ) {
        float targetPitch = DragonFlightVisualState.ascentPitch(dragon);
        if (this.nullApex$entityId != dragon.getId()) {
            this.nullApex$entityId = dragon.getId();
            this.nullApex$ascentPitch = targetPitch;
        } else {
            this.nullApex$ascentPitch += (targetPitch - this.nullApex$ascentPitch) * 0.2F;
        }
    }

    @Inject(method = "renderToBuffer", at = @At("HEAD"))
    private void nullApex$resetNeckSegmentIndex(
        PoseStack poseStack,
        VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        int color,
        CallbackInfo callbackInfo
    ) {
        this.nullApex$neckSegmentIndex = 0;
    }

    @Redirect(
        method = "renderToBuffer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
        )
    )
    private void nullApex$renderWithAscentPitch(
        ModelPart part,
        PoseStack poseStack,
        VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        int color
    ) {
        float additionalPitch = 0.0F;
        if (part == this.head) {
            additionalPitch = this.nullApex$ascentPitch;
        } else if (part == this.neck) {
            int segmentIndex = this.nullApex$neckSegmentIndex++;
            if (segmentIndex < 5) {
                float segmentWeight = (5 - segmentIndex) / 5.0F * 0.65F;
                additionalPitch = this.nullApex$ascentPitch * segmentWeight;
            }
        }

        if (additionalPitch == 0.0F) {
            part.render(poseStack, buffer, packedLight, packedOverlay, color);
            return;
        }

        float originalPitch = part.xRot;
        part.xRot = originalPitch + additionalPitch;
        try {
            part.render(poseStack, buffer, packedLight, packedOverlay, color);
        } finally {
            part.xRot = originalPitch;
        }
    }
}
