package com.assassinmod.assassin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class AssassinEventHandler {
    private AssassinEventHandler() {
    }

    public static void onLivingHurt(final LivingDamageEvent.Pre event) {
        if (event.getEntity() == null || event.getSource().getEntity() == null) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!AssassinAbilityManager.hasClass(attacker)) return;
        if (AssassinProgression.hasSkill(attacker, "twin_rhythm")
                && attacker.getMainHandItem().is(AssassinItems.TWIN_BLADES.get())) {
            event.setNewDamage(event.getNewDamage() * 1.15F);
        }
        if (AssassinAbilityManager.isShadowVeilActive(attacker)) {
            float veilMultiplier = AssassinProgression.hasSkill(attacker, "veil_strike") ? 2.25F : 1.75F;
            event.setNewDamage(event.getNewDamage() * veilMultiplier);
            AssassinAbilityManager.endShadowVeil(attacker);
            attacker.getPersistentData().remove(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL);
            if (attacker instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("Shadow Veil resolved in a critical strike."));
            }
        }
        AssassinAbilityManager.applyAssassinDamage(attacker, target, event);
    }

    public static void onAdvancementEarn(final AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AssassinProgression.awardMilestone(player, event.getAdvancement().id().toString());
        }
    }

    public static void onLivingChangeTarget(final LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob)
                || !(event.getNewAboutToBeSetTarget() instanceof Player target)
                || !AssassinAbilityManager.isShadowVeilActive(target)) return;
        // Replace a newly acquired veiled player with no target. This also drops
        // targets assigned through vanilla mob and brain behavior code paths.
        event.setNewAboutToBeSetTarget(null);
    }

    public static void onLivingVisibility(final LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player target
                && event.getLookingEntity() instanceof net.minecraft.world.entity.Mob
                && AssassinAbilityManager.isShadowVeilActive(target)) {
            event.modifyVisibility(0.0D);
        }
    }

    public static void onLivingDamagePost(final LivingDamageEvent.Post event) {
        AssassinAbilityManager.clearMirageForClone(event.getEntity());
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && AssassinAbilityManager.hasClass(attacker)
                && event.getNewDamage() > 0.0F) {
            String amount = String.format(java.util.Locale.ROOT, "%.1f", event.getNewDamage());
            attacker.displayClientMessage(Component.literal("Damage: " + amount + " ❤")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide) return;
        Player player = event.getEntity();
        AssassinAbilityManager.tickCooldowns(player);
        if (player instanceof ServerPlayer serverPlayer) {
            AssassinNetwork.syncAbilityState(serverPlayer);
            applyMovementSkill(serverPlayer);
        }
        if (AssassinAbilityManager.isShadowVeilActive(player)
                && player.getPersistentData().contains(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL)
                && player.level().getGameTime() > player.getPersistentData().getLong(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL)) {
            AssassinAbilityManager.endShadowVeil(player);
        }
    }

    private static void applyMovementSkill(ServerPlayer player) {
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "swift_step");
        boolean hasSkill = AssassinProgression.hasSkill(player, "swift_step");
        if (hasSkill && !speed.hasModifier(id)) {
            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    id, 0.08D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!hasSkill) {
            speed.removeModifier(id);
        }
    }

    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        AssassinAbilityManager.clearAllMirages();
        AssassinAbilityManager.endShadowVeil(event.getEntity());
    }

    public static void onServerTick(final ServerTickEvent.Post event) {
        AssassinAbilityManager.tickMirages();
    }

    public static void onPlayerClone(final PlayerEvent.Clone event) {
        if (event.getEntity() == null) return;
        event.getEntity().getPersistentData().merge(event.getOriginal().getPersistentData().copy());
        if (event.getEntity().getPersistentData().contains(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL)) {
            AssassinAbilityManager.endShadowVeil(event.getEntity());
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            AssassinNetwork.syncAbilityStateNow(player);
        }
    }
}
