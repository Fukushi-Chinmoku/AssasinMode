package com.assassinmod.assassin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;

@EventBusSubscriber(modid = AssassinMod.MOD_ID, value = Dist.CLIENT)
public final class AssassinClient {
    private AssassinClient() {
    }

    public static void handleSkillTreeOpen(boolean allowed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!allowed) {
            if (minecraft.player != null) minecraft.player.sendSystemMessage(Component.literal("Become an Assassin to open this skill tree."));
            return;
        }

        minecraft.setScreen(new AssassinSkillTreeScreen());
    }

    public static void registerShadowCloneRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AssassinEntities.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
    }

    /** Snap the local camera after Shadow Step so the player looks straight at the target's back. */
    public static void handleShadowStepCamera(float yaw, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return;
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
    }

    @SubscribeEvent
    public static void renderShadowTwin(final RenderPlayerEvent.Pre event) {
        if (event.getEntity().isInvisible()) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof AbstractClientPlayer clone)
                || !clone.getGameProfile().getProperties().containsKey(ShadowClones.RENDER_MARKER)) {
            return;
        }

        var renderer = event.getRenderer();
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        float partialTick = event.getPartialTick();
        float bodyYaw = Mth.rotLerp(partialTick, clone.yBodyRotO, clone.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, clone.yHeadRotO, clone.yHeadRot);
        float headTurn = Mth.wrapDegrees(headYaw - bodyYaw);
        float pitch = Mth.lerp(partialTick, clone.xRotO, clone.getXRot());
        float age = clone.tickCount + partialTick;
        float walkPosition = clone.walkAnimation.position(partialTick);
        float walkSpeed = clone.walkAnimation.speed(partialTick);

        model.attackTime = clone.getAttackAnim(partialTick);
        model.riding = clone.isPassenger()
                && clone.getVehicle() != null
                && clone.getVehicle().shouldRiderSit();
        model.young = clone.isBaby();
        model.prepareMobModel(clone, walkPosition, walkSpeed, partialTick);
        model.setupAnim(clone, walkPosition, walkSpeed, age, headTurn, pitch);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(
                RenderType.entityTranslucent(renderer.getTextureLocation(clone)));
        model.renderToBuffer(poseStack, buffer, event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                0x99000000);
        poseStack.popPose();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void hideInvisibleFirstPersonHand(final RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.isInvisible()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
        AssassinKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "assassin_hud"), (guiGraphics, deltaTracker) -> {
            AssassinHudRenderer.render(guiGraphics);
        });
    }
}
