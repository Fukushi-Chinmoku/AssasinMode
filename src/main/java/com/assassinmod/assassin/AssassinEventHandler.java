package com.assassinmod.assassin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class AssassinEventHandler {
    private AssassinEventHandler() {
    }

    public static void onLivingHurt(final LivingDamageEvent.Pre event) {
        if (event.getEntity() == null || event.getSource().getEntity() == null) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!AssassinAbilityManager.hasClass(attacker)) return;
        if (AssassinAbilityManager.isShadowVeilActive(attacker)) {
            event.setNewDamage(event.getNewDamage() * 1.75F);
            AssassinAbilityManager.endShadowVeil(attacker);
            attacker.getPersistentData().remove(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL);
            if (attacker instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("Shadow Veil resolved in a critical strike."));
            }
        }
        AssassinAbilityManager.applyAssassinDamage(attacker, target, event);
    }

    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide) return;
        Player player = event.getEntity();
        AssassinAbilityManager.tickCooldowns(player);
        if (AssassinAbilityManager.isShadowVeilActive(player)
                && player.getPersistentData().contains(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL)
                && player.level().getGameTime() > player.getPersistentData().getLong(AssassinAbilityManager.TAG_SHADOW_VEIL_UNTIL)) {
            AssassinAbilityManager.endShadowVeil(player);
        }
    }

    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        AssassinAbilityManager.clearAllMirages();
        AssassinAbilityManager.endShadowVeil(event.getEntity());
    }

    public static void onEntityJoin(final EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AssassinAbilityManager.tickMirages(serverLevel);
        }
    }

    public static void onPlayerClone(final PlayerEvent.Clone event) {
        if (!event.isWasDeath() && event.getEntity() != null) {
            AssassinAbilityManager.endShadowVeil(event.getEntity());
        }
    }
}
