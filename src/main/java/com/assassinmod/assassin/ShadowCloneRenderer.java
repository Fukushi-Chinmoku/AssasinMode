package com.assassinmod.assassin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class ShadowCloneRenderer extends LivingEntityRenderer<ShadowCloneEntity, ShadowCloneModel> {
    private final ShadowCloneModel wideModel;
    private final ShadowCloneModel slimModel;

    public ShadowCloneRenderer(EntityRendererProvider.Context context) {
        this(context,
                new ShadowCloneModel(context.bakeLayer(ModelLayers.PLAYER)),
                new ShadowCloneModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
    }

    private ShadowCloneRenderer(EntityRendererProvider.Context context,
                               ShadowCloneModel wideModel, ShadowCloneModel slimModel) {
        super(context, wideModel, 0.5F);
        this.wideModel = wideModel;
        this.slimModel = slimModel;
        addLayer(new SelectiveArmorLayer(
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                false, context));
        addLayer(new SelectiveArmorLayer(
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
                true, context));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    private final class SelectiveArmorLayer extends HumanoidArmorLayer<
            ShadowCloneEntity, ShadowCloneModel, HumanoidArmorModel<ShadowCloneEntity>> {
        private final boolean slim;

        private SelectiveArmorLayer(HumanoidArmorModel<ShadowCloneEntity> inner,
                                    HumanoidArmorModel<ShadowCloneEntity> outer,
                                    boolean slim, EntityRendererProvider.Context context) {
            super(ShadowCloneRenderer.this, inner, outer, context.getModelManager());
            this.slim = slim;
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource buffer,
                           int packedLight, ShadowCloneEntity clone,
                           float limbSwing, float limbSwingAmount, float partialTick,
                           float ageInTicks, float netHeadYaw, float headPitch) {
            if (isSlim(clone) == slim) {
                super.render(poseStack, buffer, packedLight, clone, limbSwing, limbSwingAmount,
                        partialTick, ageInTicks, netHeadYaw, headPitch);
            }
        }
    }

    @Override
    public void render(ShadowCloneEntity clone, float entityYaw, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        AbstractClientPlayer owner = getOwner(clone);
        this.model = isSlim(clone) ? slimModel : wideModel;
        super.render(clone, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private boolean isSlim(ShadowCloneEntity clone) {
        AbstractClientPlayer owner = getOwner(clone);
        return owner != null
                && owner.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowCloneEntity clone) {
        AbstractClientPlayer owner = getOwner(clone);
        if (owner != null) return owner.getSkin().texture();
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }

    @Override
    protected boolean shouldShowName(ShadowCloneEntity clone) {
        return clone.isFullCopy() && super.shouldShowName(clone);
    }

    private AbstractClientPlayer getOwner(ShadowCloneEntity clone) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || clone.getOwnerId() == null) return null;
        Player owner = minecraft.level.getPlayerByUUID(clone.getOwnerId());
        return owner instanceof AbstractClientPlayer clientPlayer ? clientPlayer : null;
    }

    @Override
    protected RenderType getRenderType(ShadowCloneEntity clone, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        ResourceLocation texture = getTextureLocation(clone);
        return clone.isFullCopy()
                ? RenderType.entityCutoutNoCull(texture)
                : RenderType.entityTranslucent(texture);
    }
}
