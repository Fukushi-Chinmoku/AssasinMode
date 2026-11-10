package com.assassinmod.assassin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class AssassinAbilityManager {
    public static final String TAG_ASSASSIN = "assassin_class";
    public static final String TAG_SHADOW_STEP_CD = "shadow_step_cd";
    public static final String TAG_SHADOW_VEIL_CD = "shadow_veil_cd";
    public static final String TAG_MIRAGE_CD = "mirage_cd";
    public static final String TAG_SHADOW_VEIL_UNTIL = "shadow_veil_until";
    public static final String TAG_MIRAGE_EFFECT = "mirage_effect";
    public static final String TAG_MIRAGE_CLONE = "mirage_clone";

    private static final Map<UUID, MirageEffect> MIRAGES = new HashMap<>();
    private static final Random RANDOM = new Random();

    private AssassinAbilityManager() {
    }

    public static boolean hasClass(Player player) {
        return player != null && player.getPersistentData().getBoolean(TAG_ASSASSIN);
    }

    public static void giveClass(Player player) {
        if (player == null) return;
        player.getPersistentData().putBoolean(TAG_ASSASSIN, true);
        player.sendSystemMessage(Component.literal("You have become an Assassin."));
    }

    public static void resetCooldowns(Player player) {
        if (player == null) return;
        player.getPersistentData().remove(TAG_SHADOW_STEP_CD);
        player.getPersistentData().remove(TAG_SHADOW_VEIL_CD);
        player.getPersistentData().remove(TAG_MIRAGE_CD);
    }

    public static int getCooldown(Player player, String key) {
        if (player == null) return 0;
        return player.getPersistentData().getInt(key);
    }

    public static void setCooldown(Player player, String key, int ticks) {
        if (player == null) return;
        player.getPersistentData().putInt(key, ticks);
    }

    public static void tickCooldowns(Player player) {
        if (player == null || player.level().isClientSide) return;
        if (player.getPersistentData().contains(TAG_SHADOW_STEP_CD)) {
            int cd = player.getPersistentData().getInt(TAG_SHADOW_STEP_CD) - 1;
            if (cd <= 0) player.getPersistentData().remove(TAG_SHADOW_STEP_CD); else player.getPersistentData().putInt(TAG_SHADOW_STEP_CD, cd);
        }
        if (player.getPersistentData().contains(TAG_SHADOW_VEIL_CD)) {
            int cd = player.getPersistentData().getInt(TAG_SHADOW_VEIL_CD) - 1;
            if (cd <= 0) player.getPersistentData().remove(TAG_SHADOW_VEIL_CD); else player.getPersistentData().putInt(TAG_SHADOW_VEIL_CD, cd);
        }
        if (player.getPersistentData().contains(TAG_MIRAGE_CD)) {
            int cd = player.getPersistentData().getInt(TAG_MIRAGE_CD) - 1;
            if (cd <= 0) player.getPersistentData().remove(TAG_MIRAGE_CD); else player.getPersistentData().putInt(TAG_MIRAGE_CD, cd);
        }
        if (player.getPersistentData().contains(TAG_SHADOW_VEIL_UNTIL)) {
            long until = player.getPersistentData().getLong(TAG_SHADOW_VEIL_UNTIL);
            if (player.level().getGameTime() >= until) {
                endShadowVeil(player);
            }
        }
    }

    public static boolean canUse(Player player, String key) {
        if (!hasClass(player)) {
            player.sendSystemMessage(Component.literal("You are not an Assassin."));
            return false;
        }
        if (player.getPersistentData().contains(key)) {
            player.sendSystemMessage(Component.literal("Ability is on cooldown."));
            return false;
        }
        return true;
    }

    public static boolean performShadowStep(Player player, Entity target) {
        if (target == null || target.level() != player.level() || !hasClass(player)) return false;
        if (!canUse(player, TAG_SHADOW_STEP_CD)) return false;
        if (target.distanceTo(player) > 100.0D) {
            player.sendSystemMessage(Component.literal("Target is out of range."));
            return false;
        }
        Vec3 behind = getSafeLocationBehindTarget(player, target);
        if (behind == null) {
            player.sendSystemMessage(Component.literal("No safe position behind the target."));
            return false;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.level().addParticle(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 0, 0.2D, 0);
        player.teleportTo(behind.x, behind.y, behind.z);
        player.level().playSound(null, behind.x, behind.y, behind.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.level().addParticle(ParticleTypes.PORTAL, behind.x, behind.y + 1.0D, behind.z, 0, 0.2D, 0);
        setCooldown(player, TAG_SHADOW_STEP_CD, 120);
        return true;
    }

    public static void activateShadowVeil(Player player) {
        if (!hasClass(player) || !canUse(player, TAG_SHADOW_VEIL_CD)) return;
        player.setInvisible(true);
        player.getPersistentData().putLong(TAG_SHADOW_VEIL_UNTIL, player.level().getGameTime() + 120L);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.level().addParticle(ParticleTypes.SMOKE, player.getX(), player.getEyeY(), player.getZ(), 0, 0.2D, 0);
        setCooldown(player, TAG_SHADOW_VEIL_CD, 240);
    }

    public static void endShadowVeil(Player player) {
        if (player == null) return;
        player.setInvisible(false);
        player.getPersistentData().remove(TAG_SHADOW_VEIL_UNTIL);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GHAST_WARN, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void activateMirage(Player player, LivingEntity target) {
        if (target == null || !hasClass(player) || !canUse(player, TAG_MIRAGE_CD)) return;
        if (target == player) {
            player.sendSystemMessage(Component.literal("Mirage cannot target yourself."));
            return;
        }
        if (player.distanceTo(target) > 30.0D) {
            player.sendSystemMessage(Component.literal("Target is too far away."));
            return;
        }
        MirageEffect effect = new MirageEffect(player, target);
        effect.start();
        MIRAGES.put(target.getUUID(), effect);
        setCooldown(player, TAG_MIRAGE_CD, 300);
    }

    public static void tickMirages() {
        for (Map.Entry<UUID, MirageEffect> entry : new HashMap<>(MIRAGES).entrySet()) {
            MirageEffect effect = entry.getValue();
            if (effect == null || effect.expired()) {
                MIRAGES.remove(entry.getKey());
                continue;
            }
            effect.tick();
        }
    }

    public static void clearMirageForClone(Entity clone) {
        if (clone == null || !isMirageClone(clone)) return;
        for (MirageEffect effect : MIRAGES.values()) {
            if (effect.removeClone(clone)) {
                return;
            }
        }
    }

    public static void clearMirage(UUID targetId) {
        MirageEffect effect = MIRAGES.remove(targetId);
        if (effect != null) {
            effect.remove();
        }
    }

    public static void clearAllMirages() {
        for (MirageEffect effect : new HashMap<>(MIRAGES).values()) {
            effect.remove();
        }
        MIRAGES.clear();
    }

    public static boolean isShadowVeilActive(Player player) {
        return player != null && player.isInvisible();
    }

    public static boolean isBehindTarget(LivingEntity attacker, LivingEntity target) {
        Vec3 targetToAttacker = attacker.position().subtract(target.position()).normalize();
        Vec3 targetForward = target.getLookAngle().normalize();
        return targetToAttacker.dot(targetForward) < -0.15D;
    }

    public static void applyAssassinDamage(LivingEntity attacker, LivingEntity target, LivingDamageEvent.Pre event) {
        if (!(attacker instanceof Player player) || !hasClass(player)) return;
        if (!isBehindTarget(attacker, target)) return;
        float amount = event.getNewDamage() * 2.0F;
        event.setNewDamage(amount);
    }

    public static void onSuccessfulAttack(Player attacker, LivingEntity target) {
        if (!hasClass(attacker)) return;
        if (isShadowVeilActive(attacker) && !attacker.getPersistentData().contains(TAG_SHADOW_VEIL_UNTIL)) {
            endShadowVeil(attacker);
        }
        if (isShadowVeilActive(attacker)) {
            endShadowVeil(attacker);
            attacker.getPersistentData().remove(TAG_SHADOW_VEIL_UNTIL);
            attacker.sendSystemMessage(Component.literal("Shadow Veil has ended."));
        }
        if (target != null && target instanceof net.minecraft.world.entity.player.Player targetPlayer) {
            MirageEffect effect = MIRAGES.get(targetPlayer.getUUID());
            if (effect != null && effect.isClone(targetPlayer)) {
                effect.remove();
                MIRAGES.remove(targetPlayer.getUUID());
            }
        }
    }

    public static Vec3 getSafeLocationBehindTarget(Entity player, Entity target) {
        if (player == null || target == null) return null;
        Vec3 targetBase = target.position();
        Vec3 forward = target.getLookAngle().normalize();
        Vec3 behind = targetBase.add(forward.scale(-1.8D));
        Vec3 safe = new Vec3(behind.x, target.getY() + 0.75D, behind.z);
        BlockPos pos = BlockPos.containing(safe);
        Level level = player.level();
        AABB box = new AABB(safe.x - 0.35D, safe.y - 0.1D, safe.z - 0.35D, safe.x + 0.35D, safe.y + 1.8D, safe.z + 0.35D);
        boolean clear = level.getBlockStates(box).allMatch(state -> state.isAir() || state.getCollisionShape(level, pos).isEmpty());
        if (!clear) return null;
        return safe;
    }

    public static boolean isMirageClone(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(TAG_MIRAGE_CLONE);
    }

    public static void markMirageClone(Entity entity) {
        if (entity != null) {
            entity.getPersistentData().putBoolean(TAG_MIRAGE_CLONE, true);
        }
    }
}
