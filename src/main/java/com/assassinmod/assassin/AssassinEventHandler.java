package com.assassinmod.assassin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Random;

public final class AssassinEventHandler {
    private static final Random RANDOM = new Random();

    private AssassinEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(final LivingDamageEvent.Pre event) {
        if (event.getEntity() == null) return;
        if (event.getEntity() instanceof Player defender && AssassinAbilityManager.hasClass(defender)) {
            long gameTime = defender.level().getGameTime();
            defender.getPersistentData().putLong("assassin_combat_until", gameTime + 100L);
            double evasionChance = AssassinProgression.getResourceValue(defender, 1, 0) / 100.0D;
            if (AssassinAbilityManager.isPerfectDodgeActive(defender)
                    || (evasionChance > 0.0D && RANDOM.nextDouble() < evasionChance)) {
                event.setNewDamage(0.0F);
                if (defender instanceof ServerPlayer serverDefender) EvasionAfterimage.launch(serverDefender);
                defender.level().playSound(null, defender.blockPosition(),
                        net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.4F);
                return;
            }
        }
        if (event.getSource().getEntity() == null) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!AssassinAbilityManager.hasClass(attacker)) return;
        attacker.getPersistentData().putLong("assassin_combat_until", attacker.level().getGameTime() + 100L);
        if (AssassinAbilityManager.isShadowVeilActive(attacker)) {
            AssassinAbilityManager.endShadowVeil(attacker);
            attacker.getPersistentData().remove(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL);
        }
        AssassinAbilityManager.applyAssassinDamage(attacker, target, event);
        if (AssassinAbilityManager.isRapidAssaultActive(attacker)) {
            event.setNewDamage(event.getNewDamage() * 0.1F);
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarn(final AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AssassinProgression.awardMilestone(player, event.getAdvancement().id().toString());
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(final LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob)
                || !(event.getNewAboutToBeSetTarget() instanceof Player target)
                || !AssassinAbilityManager.isShadowVeilActive(target)) return;
        // Replace a newly acquired veiled player with no target. This also drops
        // targets assigned through vanilla mob and brain behavior code paths.
        event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    public static void onLivingVisibility(final LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player target
                && event.getLookingEntity() instanceof net.minecraft.world.entity.Mob) {
            if (AssassinAbilityManager.isShadowVeilActive(target)) {
                event.modifyVisibility(0.0D);
            } else if (AssassinProgression.hasSkill(target, "quiet_step")
                    && target.getPersistentData().getLong("assassin_combat_until") < target.level().getGameTime()) {
                double visibility = 1.0D - AssassinProgression.getResourceValue(target, 7, 0) / 100.0D;
                event.modifyVisibility(visibility);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(final LivingDamageEvent.Post event) {
        AssassinAbilityManager.clearMirageForClone(event.getEntity());
        if (event.getEntity() instanceof ShadowCloneEntity clone
                && event.getSource().getEntity() instanceof LivingEntity attacker) {
            VeilDecoy.onCloneHit(clone, attacker);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && AssassinAbilityManager.hasClass(attacker)
                && event.getNewDamage() > 0.0F) {
            String amount = String.format(java.util.Locale.ROOT, "%.1f", event.getNewDamage());
            attacker.displayClientMessage(Component.literal("Damage: " + amount + " ❤")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide) return;
        Player player = event.getEntity();
        AssassinAbilityManager.tickCooldowns(player);
        if (player instanceof ServerPlayer serverPlayer) {
            AssassinProgression.migrateSkillTree(serverPlayer);
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
        long now = player.level().getGameTime();
        boolean sprinting = player.getPersistentData().getLong(AssassinAbilityManager.TAG_SPRINT_UNTIL) > now;
        double swiftAmount = AssassinProgression.getResourceValue(player, 2, 0) / 100.0D;
        if (hasSkill) {
            speed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    id, swiftAmount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!hasSkill) {
            speed.removeModifier(id);
        }
        double sprintAmount = AssassinProgression.getResourceValue(player, 5, 0) / 100.0D;
        applySpeedModifier(speed, "assassin_sprinting", sprinting, sprintAmount);

        var attackSpeed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;
        var attackSpeedId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "rapid_assault");
        boolean hasRapidAssault = AssassinAbilityManager.isRapidAssaultActive(player);
        if (hasRapidAssault) {
            attackSpeed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    attackSpeedId, 9.0D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!hasRapidAssault) {
            attackSpeed.removeModifier(attackSpeedId);
        }
    }

    private static void applySpeedModifier(
            net.minecraft.world.entity.ai.attributes.AttributeInstance speed, String name, boolean active, double amount) {
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, name);
        if (active && !speed.hasModifier(id)) {
            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    id, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!active) {
            speed.removeModifier(id);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        AssassinAbilityManager.clearAllMirages();
        ShadowTwinStrike.clearAll();
        ShadowEchoHit.clearAll();
        EvasionAfterimage.clearAll();
        VeilDecoy.dismiss(event.getEntity());
        AssassinAbilityManager.endShadowVeil(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        AssassinAbilityManager.tickMirages();
        ShadowTwinStrike.tickAll();
        ShadowEchoHit.tickAll();
        EvasionAfterimage.tickAll();
        VeilDecoy.tickAll();
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
