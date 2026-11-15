package com.assassinmod.assassin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class ShadowCloneModel extends HumanoidModel<ShadowCloneEntity> {
    private static final float WALK_CYCLE_SPEED = 0.6662F;

    private final ModelPart jacket;
    private final ModelPart leftSleeve;
    private final ModelPart rightSleeve;
    private final ModelPart leftPants;
    private final ModelPart rightPants;
    private final ModelPart ear;
    private final ModelPart cloak;

    public ShadowCloneModel(ModelPart root) {
        super(root);
        this.jacket = root.getChild("jacket");
        this.leftSleeve = root.getChild("left_sleeve");
        this.rightSleeve = root.getChild("right_sleeve");
        this.leftPants = root.getChild("left_pants");
        this.rightPants = root.getChild("right_pants");
        this.ear = root.getChild("ear");
        this.cloak = root.getChild("cloak");
    }

    @Override
    public void setupAnim(ShadowCloneEntity clone, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float speed = clone.getAnimationSpeed();
        float cadence = clone.isSprinting() ? 1.35F : 1.0F;
        float phase = clone.getAnimationPhase();
        if (speed > 0.02F) {
            phase += (ageInTicks - clone.tickCount) * cadence;
        }
        float walkAmount = speed > 0.02F || clone.isSprinting()
                ? Mth.clamp(0.65F + speed * 0.35F, 0.0F, 1.0F) : 0.0F;
        super.setupAnim(clone, phase, walkAmount,
                ageInTicks, netHeadYaw, headPitch);
        float walkCycle = phase * WALK_CYCLE_SPEED;
        if (this.attackTime <= 0.0F) {
            this.rightArm.xRot = Mth.cos(walkCycle + Mth.PI) * walkAmount
                    + (clone.isSprinting() ? -0.15F : 0.0F);
            this.leftArm.xRot = Mth.cos(walkCycle) * walkAmount;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }
        this.rightLeg.xRot = Mth.cos(walkCycle) * walkAmount * 1.4F;
        this.leftLeg.xRot = Mth.cos(walkCycle + Mth.PI) * walkAmount * 1.4F;
        this.jacket.copyFrom(this.body);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
        this.fullCopy = clone.isFullCopy();
    }

    private boolean fullCopy;

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        int tint = fullCopy ? 0xFFFFFFFF : 0x99000000;
        this.head.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.hat.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.body.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.rightArm.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.leftArm.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.rightLeg.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.leftLeg.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.ear.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.cloak.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.jacket.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.leftSleeve.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.rightSleeve.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.leftPants.render(poseStack, consumer, packedLight, packedOverlay, tint);
        this.rightPants.render(poseStack, consumer, packedLight, packedOverlay, tint);
    }
}
