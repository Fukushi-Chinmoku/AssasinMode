package com.assassinmod.assassin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class AssassinKeyMappings {
    public static final KeyMapping SHADOW_STEP = new KeyMapping("key.assassin.shadow_step", GLFW.GLFW_KEY_Z, "key.category.assassin");
    public static final KeyMapping SHADOW_TWIN = new KeyMapping("key.assassin.shadow_twin", GLFW.GLFW_KEY_R, "key.category.assassin");
    public static final KeyMapping SHADOW_VEIL = new KeyMapping("key.assassin.shadow_veil", GLFW.GLFW_KEY_X, "key.category.assassin");
    public static final KeyMapping MIRAGE = new KeyMapping("key.assassin.mirage", GLFW.GLFW_KEY_C, "key.category.assassin");
    public static final KeyMapping RAPID_ASSAULT = new KeyMapping("key.assassin.rapid_assault", GLFW.GLFW_KEY_G, "key.category.assassin");
    public static final KeyMapping SPRINT = new KeyMapping("key.assassin.sprint", GLFW.GLFW_KEY_H, "key.category.assassin");
    public static final KeyMapping BACKSTEP = new KeyMapping("key.assassin.backstep", GLFW.GLFW_KEY_N, "key.category.assassin");
    public static final KeyMapping PERFECT_DODGE = new KeyMapping("key.assassin.perfect_dodge", GLFW.GLFW_KEY_B, "key.category.assassin");
    public static final KeyMapping SHADOW_DANCE = new KeyMapping("key.assassin.shadow_dance", GLFW.GLFW_KEY_J, "key.category.assassin");
    public static final KeyMapping SKILL_TREE = new KeyMapping("key.assassin.skill_tree", GLFW.GLFW_KEY_V, "key.category.assassin");

    private static boolean lastShadowStepDown;
    private static boolean lastShadowTwinDown;
    private static boolean lastShadowVeilDown;
    private static boolean lastMirageDown;
    private static boolean lastRapidAssaultDown;
    private static boolean lastSprintDown;
    private static boolean lastBackstepDown;
    private static boolean lastPerfectDodgeDown;
    private static boolean lastShadowDanceDown;

    private AssassinKeyMappings() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(SHADOW_STEP);
        event.register(SHADOW_TWIN);
        event.register(SHADOW_VEIL);
        event.register(MIRAGE);
        event.register(RAPID_ASSAULT);
        event.register(SPRINT);
        event.register(BACKSTEP);
        event.register(PERFECT_DODGE);
        event.register(SHADOW_DANCE);
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
        boolean shadowTwinClicked = SHADOW_TWIN.consumeClick();
        boolean shadowVeilClicked = SHADOW_VEIL.consumeClick();
        boolean mirageClicked = MIRAGE.consumeClick();
        boolean rapidAssaultClicked = RAPID_ASSAULT.consumeClick();
        boolean sprintClicked = SPRINT.consumeClick();
        boolean backstepClicked = BACKSTEP.consumeClick();
        boolean perfectDodgeClicked = PERFECT_DODGE.consumeClick();
        boolean shadowDanceClicked = SHADOW_DANCE.consumeClick();
        boolean shadowStepPressed = triggerOnPress(SHADOW_STEP, () -> lastShadowStepDown) || shadowStepClicked;
        boolean shadowTwinPressed = triggerOnPress(SHADOW_TWIN, () -> lastShadowTwinDown) || shadowTwinClicked;
        boolean shadowVeilPressed = triggerOnPress(SHADOW_VEIL, () -> lastShadowVeilDown) || shadowVeilClicked;
        boolean miragePressed = triggerOnPress(MIRAGE, () -> lastMirageDown) || mirageClicked;
        boolean rapidAssaultPressed = triggerOnPress(RAPID_ASSAULT, () -> lastRapidAssaultDown) || rapidAssaultClicked;
        boolean sprintPressed = triggerOnPress(SPRINT, () -> lastSprintDown) || sprintClicked;
        boolean backstepPressed = triggerOnPress(BACKSTEP, () -> lastBackstepDown) || backstepClicked;
        boolean perfectDodgePressed = triggerOnPress(PERFECT_DODGE, () -> lastPerfectDodgeDown) || perfectDodgeClicked;
        boolean shadowDancePressed = triggerOnPress(SHADOW_DANCE, () -> lastShadowDanceDown) || shadowDanceClicked;

        if (shadowStepPressed) {
            double range = AssassinProgression.getResourceValueForRank(9, 0,
                    AssassinClientState.getSkillRank(9));
            Entity target = resolveTargetEntity(player, range);
            if (target != null) AssassinNetwork.sendAbilityRequest(0, target.getId());
        }

        if (shadowTwinPressed) {
            double range = AssassinProgression.getResourceValueForRank(6, 1,
                    AssassinClientState.getSkillRank(6));
            Entity target = resolveTargetEntity(player, range);
            if (target != null) AssassinNetwork.sendAbilityRequest(3, target.getId());
        }

        if (shadowVeilPressed) {
            AssassinNetwork.sendAbilityRequest(1, -1);
        }

        if (miragePressed) {
            Entity target = resolveTargetEntity(player, 45.0D);
            if (target != null) {
                AssassinNetwork.sendAbilityRequest(2, target.getId());
            }
        }

        if (rapidAssaultPressed) {
            AssassinNetwork.sendAbilityRequest(4, -1);
        }

        if (sprintPressed) {
            AssassinNetwork.sendAbilityRequest(5, -1);
        }

        if (backstepPressed) {
            AssassinNetwork.sendAbilityRequest(6, -1);
        }
        if (perfectDodgePressed) {
            AssassinNetwork.sendAbilityRequest(7, -1);
        }
        if (shadowDancePressed) {
            AssassinNetwork.sendAbilityRequest(8, -1);
        }

    }

    private static boolean triggerOnPress(KeyMapping key, java.util.function.BooleanSupplier lastStateSupplier) {
        boolean current = key.isDown();
        boolean triggered = current && !lastStateSupplier.getAsBoolean();
        switch (key.getName()) {
            case "key.assassin.shadow_step" -> lastShadowStepDown = current;
            case "key.assassin.shadow_twin" -> lastShadowTwinDown = current;
            case "key.assassin.shadow_veil" -> lastShadowVeilDown = current;
            case "key.assassin.mirage" -> lastMirageDown = current;
            case "key.assassin.rapid_assault" -> lastRapidAssaultDown = current;
            case "key.assassin.sprint" -> lastSprintDown = current;
            case "key.assassin.backstep" -> lastBackstepDown = current;
            case "key.assassin.perfect_dodge" -> lastPerfectDodgeDown = current;
            case "key.assassin.shadow_dance" -> lastShadowDanceDown = current;
            default -> {
            }
        }
        return triggered;
    }

    private static Entity resolveTargetEntity(LocalPlayer player, double maxRange) {
        if (maxRange <= 0.0D) return null;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(maxRange));
        HitResult blockHit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxHitDistance = blockHit.getType() == HitResult.Type.MISS
                ? maxRange * maxRange : start.distanceToSqr(blockHit.getLocation());
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);

        Entity closest = null;
        double closestDistance = maxHitDistance;
        for (Entity entity : player.level().getEntities(player, searchBox,
                candidate -> candidate instanceof LivingEntity && candidate.isPickable())) {
            var hit = entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        return closest;
    }
}
