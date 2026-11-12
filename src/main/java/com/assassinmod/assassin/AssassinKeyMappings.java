package com.assassinmod.assassin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class AssassinKeyMappings {
    public static final KeyMapping SHADOW_STEP = new KeyMapping("key.assassin.shadow_step", GLFW.GLFW_KEY_Z, "key.category.assassin");
    public static final KeyMapping SHADOW_VEIL = new KeyMapping("key.assassin.shadow_veil", GLFW.GLFW_KEY_X, "key.category.assassin");
    public static final KeyMapping MIRAGE = new KeyMapping("key.assassin.mirage", GLFW.GLFW_KEY_C, "key.category.assassin");
    public static final KeyMapping SKILL_TREE = new KeyMapping("key.assassin.skill_tree", GLFW.GLFW_KEY_V, "key.category.assassin");

    private static boolean lastShadowStepDown;
    private static boolean lastShadowVeilDown;
    private static boolean lastMirageDown;

    private AssassinKeyMappings() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(SHADOW_STEP);
        event.register(SHADOW_VEIL);
        event.register(MIRAGE);
        event.register(SKILL_TREE);
    }

    @OnlyIn(Dist.CLIENT)
    public static void onKeyInput(final InputEvent.Key event) {
        handleKeyBindings();
    }

    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(final ClientTickEvent.Post event) {
        handleKeyBindings();
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleKeyBindings() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) return;

        if (SKILL_TREE.consumeClick()) {
            AssassinNetwork.sendOpenSkillTreeRequest();
            return;
        }

        boolean shadowStepClicked = SHADOW_STEP.consumeClick();
        boolean shadowVeilClicked = SHADOW_VEIL.consumeClick();
        boolean mirageClicked = MIRAGE.consumeClick();
        boolean shadowStepPressed = triggerOnPress(SHADOW_STEP, () -> lastShadowStepDown) || shadowStepClicked;
        boolean shadowVeilPressed = triggerOnPress(SHADOW_VEIL, () -> lastShadowVeilDown) || shadowVeilClicked;
        boolean miragePressed = triggerOnPress(MIRAGE, () -> lastMirageDown) || mirageClicked;

        if (shadowStepPressed) {
            Entity target = resolveTargetEntity(player, minecraft);
            if (target == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("No valid target in range."));
            } else {
                AssassinNetwork.sendAbilityRequest(0, target.getId());
            }
        }

        if (shadowVeilPressed) {
            AssassinNetwork.sendAbilityRequest(1, -1);
        }

        if (miragePressed) {
            Entity target = resolveTargetEntity(player, minecraft);
            if (target != null) {
                AssassinNetwork.sendAbilityRequest(2, target.getId());
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Aim at a living target to use Mirage."));
            }
        }
    }

    private static boolean triggerOnPress(KeyMapping key, java.util.function.BooleanSupplier lastStateSupplier) {
        boolean current = key.isDown();
        boolean triggered = current && !lastStateSupplier.getAsBoolean();
        switch (key.getName()) {
            case "key.assassin.shadow_step" -> lastShadowStepDown = current;
            case "key.assassin.shadow_veil" -> lastShadowVeilDown = current;
            case "key.assassin.mirage" -> lastMirageDown = current;
            default -> {
            }
        }
        return triggered;
    }

    private static Entity resolveTargetEntity(LocalPlayer player, Minecraft minecraft) {
        if (minecraft.hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity();
        }

        return null;
    }
}
