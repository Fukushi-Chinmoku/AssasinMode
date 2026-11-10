package com.assassinmod.assassin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class AssassinKeyMappings {
    public static final KeyMapping SHADOW_STEP = new KeyMapping("key.assassin.shadow_step", GLFW.GLFW_KEY_Z, "key.category.assassin");
    public static final KeyMapping SHADOW_VEIL = new KeyMapping("key.assassin.shadow_veil", GLFW.GLFW_KEY_X, "key.category.assassin");
    public static final KeyMapping MIRAGE = new KeyMapping("key.assassin.mirage", GLFW.GLFW_KEY_C, "key.category.assassin");

    private AssassinKeyMappings() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(SHADOW_STEP);
        event.register(SHADOW_VEIL);
        event.register(MIRAGE);
    }

    @OnlyIn(Dist.CLIENT)
    public static void onKeyInput(final InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) return;

        if (SHADOW_STEP.consumeClick()) {
            if (minecraft.hitResult instanceof EntityHitResult entityHitResult) {
                Entity target = entityHitResult.getEntity();
                assassinShadowStep(player, target);
            }
        }
        if (SHADOW_VEIL.consumeClick()) {
            AssassinAbilityManager.activateShadowVeil(player);
        }
        if (MIRAGE.consumeClick()) {
            if (minecraft.hitResult instanceof EntityHitResult entityHitResult) {
                Entity target = entityHitResult.getEntity();
                if (target instanceof LivingEntity living) {
                    AssassinAbilityManager.activateMirage(player, living);
                }
            }
        }
    }

    private static void assassinShadowStep(LocalPlayer player, Entity target) {
        if (target == null || target.distanceTo(player) > 100.0D) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("No valid target in range."));
            return;
        }
        AssassinAbilityManager.performShadowStep(player, target);
    }
}
