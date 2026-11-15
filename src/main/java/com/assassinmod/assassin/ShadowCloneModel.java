package com.assassinmod.assassin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class ShadowCloneModel extends PlayerModel<ShadowCloneEntity> {
    private static final float WALK_CYCLE_SPEED = 0.6662F;

    public ShadowCloneModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(ShadowCloneEntity clone, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float speed = clone.getAnimationSpeed();
        float sprintMultiplier = clone.isSprinting() ? 1.35F : 1.0F;
        float phase = clone.getAnimationPhase() * sprintMultiplier;
        super.setupAnim(clone, phase, speed,
                ageInTicks, netHeadYaw, headPitch);
        float walkAmount = Mth.clamp(speed * 1.6F, 0.0F, 1.0F);
        float walkCycle = phase * WALK_CYCLE_SPEED;
        this.rightArm.xRot += Mth.cos(walkCycle + Mth.PI) * walkAmount * 0.5F;
        this.leftArm.xRot += Mth.cos(walkCycle) * walkAmount * 0.5F;
        this.rightLeg.xRot = Mth.cos(walkCycle) * walkAmount * 1.4F;
        this.leftLeg.xRot = Mth.cos(walkCycle + Mth.PI) * walkAmount * 1.4F;
        this.fullCopy = clone.isFullCopy();
    }

    private boolean fullCopy;

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay,
                fullCopy ? 0xFFFFFFFF : 0x99000000);
    }
}
