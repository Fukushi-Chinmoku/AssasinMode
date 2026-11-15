package com.assassinmod.assassin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class AssassinAbilityManager {
    public static final String TAG_ASSASSIN = "assassin_class";
    public static final String TAG_SHADOW_STEP_CD = "shadow_step_cd";
    public static final String TAG_SHADOW_TWIN_CD = "shadow_twin_cd";
    public static final String TAG_SHADOW_VEIL_CD = "shadow_veil_cd";
    public static final String TAG_MIRAGE_CD = "mirage_cd";
    public static final String TAG_RAPID_ASSAULT_CD = "rapid_assault_cd";
    public static final String TAG_RAPID_ASSAULT_UNTIL = "rapid_assault_until";
    public static final String TAG_SHADOW_VEIL_UNTIL = "shadow_veil_until";
    public static final String TAG_SPRINT_CD = "assassin_sprint_cd";
    public static final String TAG_SPRINT_UNTIL = "assassin_sprint_until";
    public static final String TAG_BACKSTEP_CD = "assassin_backstep_cd";
    public static final String TAG_PERFECT_DODGE_CD = "assassin_perfect_dodge_cd";
    public static final String TAG_SHADOW_DANCE_CD = "assassin_shadow_dance_cd";
    public static final String TAG_PERFECT_DODGE_UNTIL = "assassin_perfect_dodge_until";
    public static final String TAG_MIRAGE_EFFECT = "mirage_effect";
    public static final String TAG_MIRAGE_CLONE = "mirage_clone";
    public static final String TAG_SHADOW_DANCE_UNTIL = "shadow_dance_until";

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
    }

    public static void resetCooldowns(Player player) {
        if (player == null) return;
        player.getPersistentData().remove(TAG_SHADOW_STEP_CD);
        player.getPersistentData().remove(TAG_SHADOW_TWIN_CD);
        player.getPersistentData().remove(TAG_SHADOW_VEIL_CD);
        player.getPersistentData().remove(TAG_MIRAGE_CD);
        player.getPersistentData().remove(TAG_RAPID_ASSAULT_CD);
        player.getPersistentData().remove(TAG_RAPID_ASSAULT_UNTIL);
        player.getPersistentData().remove(TAG_SPRINT_CD);
        player.getPersistentData().remove(TAG_SPRINT_UNTIL);
        player.getPersistentData().remove(TAG_BACKSTEP_CD);
        player.getPersistentData().remove(TAG_PERFECT_DODGE_CD);
        player.getPersistentData().remove(TAG_SHADOW_DANCE_CD);
        player.getPersistentData().remove(TAG_PERFECT_DODGE_UNTIL);
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
        if (player.getPersistentData().contains(TAG_SHADOW_TWIN_CD)) {
            int cd = player.getPersistentData().getInt(TAG_SHADOW_TWIN_CD) - 1;
            if (cd <= 0) player.getPersistentData().remove(TAG_SHADOW_TWIN_CD); else player.getPersistentData().putInt(TAG_SHADOW_TWIN_CD, cd);
        }
        if (player.getPersistentData().contains(TAG_SHADOW_VEIL_CD)) {
            int cd = player.getPersistentData().getInt(TAG_SHADOW_VEIL_CD) - 1;
            if (cd <= 0) player.getPersistentData().remove(TAG_SHADOW_VEIL_CD); else player.getPersistentData().putInt(TAG_SHADOW_VEIL_CD, cd);
        }
        if (player.getPersistentData().contains(TAG_MIRAGE_CD)) {
            int cd = player.getPersistentData().getInt(TAG_MIRAGE_CD) - 1;
            if (cd <= 0) player.getPersistentData().remove(TAG_MIRAGE_CD); else player.getPersistentData().putInt(TAG_MIRAGE_CD, cd);
        }
        if (player.getPersistentData().contains(TAG_RAPID_ASSAULT_CD)) {
            int cd = player.getPersistentData().getInt(TAG_RAPID_ASSAULT_CD) - 1;
            if (cd <= 0) player.getPersistentData().remove(TAG_RAPID_ASSAULT_CD);
            else player.getPersistentData().putInt(TAG_RAPID_ASSAULT_CD, cd);
        }
        tickCooldown(player, TAG_SPRINT_CD);
        tickCooldown(player, TAG_BACKSTEP_CD);
        tickCooldown(player, TAG_PERFECT_DODGE_CD);
        tickCooldown(player, TAG_SHADOW_DANCE_CD);
        if (player.getPersistentData().contains(TAG_RAPID_ASSAULT_UNTIL)
                && player.level().getGameTime() >= player.getPersistentData().getLong(TAG_RAPID_ASSAULT_UNTIL)) {
            player.getPersistentData().remove(TAG_RAPID_ASSAULT_UNTIL);
        }
        if (player.getPersistentData().contains(TAG_SHADOW_VEIL_UNTIL)) {
            long until = player.getPersistentData().getLong(TAG_SHADOW_VEIL_UNTIL);
            if (player.level().getGameTime() >= until) {
                endShadowVeil(player);
            } else {
                player.setInvisible(true);
            }
        }
        long now = player.level().getGameTime();
        if (player.getPersistentData().contains(TAG_SPRINT_UNTIL)
                && now >= player.getPersistentData().getLong(TAG_SPRINT_UNTIL)) {
            player.getPersistentData().remove(TAG_SPRINT_UNTIL);
        }
        if (player.getPersistentData().contains(TAG_PERFECT_DODGE_UNTIL)
                && now >= player.getPersistentData().getLong(TAG_PERFECT_DODGE_UNTIL)) {
            player.getPersistentData().remove(TAG_PERFECT_DODGE_UNTIL);
        }
    }

    public static boolean isPerfectDodgeActive(Player player) {
        return player != null
                && player.getPersistentData().getLong(TAG_PERFECT_DODGE_UNTIL) > player.level().getGameTime();
    }

    public static void activatePerfectDodge(Player player) {
        if (!AssassinProgression.hasSkill(player, "perfect_dodge")) {
            return;
        }
        if (!hasClass(player) || !canUse(player, TAG_PERFECT_DODGE_CD)) return;
        player.getPersistentData().putLong(TAG_PERFECT_DODGE_UNTIL, player.level().getGameTime() + 100L);
        setCooldown(player, TAG_PERFECT_DODGE_CD, 1200);
        if (player instanceof ServerPlayer serverPlayer) AssassinNetwork.syncAbilityStateNow(serverPlayer);
    }

    private static void tickCooldown(Player player, String key) {
        if (!player.getPersistentData().contains(key)) return;
        int remaining = player.getPersistentData().getInt(key) - 1;
        if (remaining <= 0) player.getPersistentData().remove(key);
        else player.getPersistentData().putInt(key, remaining);
    }

    public static boolean canUse(Player player, String key) {
        if (!hasClass(player)) {
            return false;
        }
        if (player.getPersistentData().contains(key)) {
            return false;
        }
        return true;
    }

    public static boolean performShadowStep(Player player, Entity target) {
        return performShadowStep(player, target, false);
    }

    /** Plain teleport behind the target, or — with {@code twinStrike} — the Shadow Twin upgrade. */
    public static boolean performShadowStep(Player player, Entity target, boolean twinStrike) {
        if (target == null || target.level() != player.level() || !hasClass(player)) return false;
        if (twinStrike && !AssassinProgression.hasSkill(player, "shadow_twin")) {
            return false;
        }
        if (!twinStrike && !AssassinProgression.hasSkill(player, "shadow_step")) {
            return false;
        }
        if (!canUse(player, twinStrike ? TAG_SHADOW_TWIN_CD : TAG_SHADOW_STEP_CD)) return false;
        int skillIndex = twinStrike ? 6 : 9;
        int rangeResource = twinStrike ? 1 : 0;
        double maxRange = Math.min(100.0D,
                AssassinProgression.getResourceValue(player, skillIndex, rangeResource));
        if (target.distanceTo(player) > maxRange || !isTargetInView(player, target, maxRange)) {
            return false;
        }
        Vec3 behind = getSafeLocationBehindTarget(player, target);
        if (behind == null) {
            return false;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.level().addParticle(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 0, 0.2D, 0);
        player.teleportTo(behind.x, behind.y, behind.z);
        // Aim at the target's back rather than its eyes so the camera centers on the body.
        Vec3 aimPoint = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.75D, target.getZ());
        Vec3 playerEyes = player.getEyePosition();
        Vec3 look = aimPoint.subtract(playerEyes);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(-look.x, look.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(look.y, horizontal)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        if (player instanceof ServerPlayer serverPlayer) {
            // Server-side rotations do not move the local camera; sync it explicitly.
            AssassinNetwork.sendShadowStepCamera(serverPlayer, yaw, pitch);
            if (twinStrike && target instanceof LivingEntity living) {
                ShadowTwinStrike.launch(serverPlayer, living);
            }
        }
        player.level().playSound(null, behind.x, behind.y, behind.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.level().addParticle(ParticleTypes.PORTAL, behind.x, behind.y + 1.0D, behind.z, 0, 0.2D, 0);
        if (twinStrike) {
            setCooldown(player, TAG_SHADOW_TWIN_CD, secondsToTicks(
                    AssassinProgression.getResourceValue(player, 6, 2)));
        } else {
            setCooldown(player, TAG_SHADOW_STEP_CD, secondsToTicks(
                    AssassinProgression.getResourceValue(player, 9, 1)));
        }
        return true;
    }

    public static void activateShadowDance(Player player) {
        if (!hasClass(player)) return;
        int rank = AssassinProgression.getRank(player, "shadow_dance");
        if (rank <= 0) {
            return;
        }
        if (!canUse(player, TAG_SHADOW_DANCE_CD)) return;
        player.getPersistentData().putLong(TAG_SHADOW_DANCE_UNTIL,
                player.level().getGameTime() + rank * 20L);
        setCooldown(player, TAG_SHADOW_DANCE_CD, 90 * 20);
        if (player instanceof ServerPlayer serverPlayer) {
            AssassinNetwork.syncAbilityStateNow(serverPlayer);
        }
    }

    public static void activateSprint(Player player) {
        if (!AssassinProgression.hasSkill(player, "sprinting")) {
            return;
        }
        if (!hasClass(player) || !canUse(player, TAG_SPRINT_CD)) return;
        player.getPersistentData().putLong(TAG_SPRINT_UNTIL, player.level().getGameTime()
                + secondsToTicks(AssassinProgression.getResourceValue(player, 5, 1)));
        setCooldown(player, TAG_SPRINT_CD, secondsToTicks(
                AssassinProgression.getResourceValue(player, 5, 2)));
    }

    public static void activateBackstep(Player player) {
        if (!AssassinProgression.hasSkill(player, "backstep")) {
            return;
        }
        if (!hasClass(player) || !canUse(player, TAG_BACKSTEP_CD)) return;
        Vec3 backward = player.getLookAngle().multiply(-1.0D, 0.0D, -1.0D).normalize();
        if (backward.lengthSqr() < 1.0E-6D) return;
        double distance = AssassinProgression.getResourceValue(player, 11, 0);
        Vec3 destination = player.position().add(backward.scale(distance));
        BlockPos feet = BlockPos.containing(destination);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        if (!player.level().getBlockState(feet).getCollisionShape(player.level(), feet).isEmpty()
                || !player.level().getBlockState(head).getCollisionShape(player.level(), head).isEmpty()
                || player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty()) {
            return;
        }
        Vec3 origin = player.position();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.level().playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        player.level().addParticle(ParticleTypes.SMOKE, origin.x, origin.y + 1.0D, origin.z, 0, 0.1D, 0);
        if (AssassinProgression.hasSkill(player, "decoy") && player instanceof ServerPlayer serverPlayer) {
            int lifetime = secondsToTicks(AssassinProgression.getResourceValue(player, 13, 0));
            VeilDecoy.spawn(serverPlayer, lifetime, origin);
        }
        setCooldown(player, TAG_BACKSTEP_CD, secondsToTicks(
                AssassinProgression.getResourceValue(player, 11, 1)));
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }

    public static void activateShadowVeil(Player player) {
        if (!AssassinProgression.hasSkill(player, "shadow_veil")) {
            return;
        }
        if (!hasClass(player) || !canUse(player, TAG_SHADOW_VEIL_CD)) return;
        player.setInvisible(true);
        int duration = secondsToTicks(AssassinProgression.getResourceValue(player, 4, 0));
        player.getPersistentData().putLong(TAG_SHADOW_VEIL_UNTIL, player.level().getGameTime() + duration);
        clearHostileTargets(player);
        // Deceptive Shadow: the player vanishes and a chaotically wandering decoy takes their place.
        if (AssassinProgression.hasSkill(player, "veil_double") && player instanceof ServerPlayer serverPlayer) {
            VeilDecoy.spawn(serverPlayer, duration);
        }
        setCooldown(player, TAG_SHADOW_VEIL_CD, secondsToTicks(
                AssassinProgression.getResourceValue(player, 4, 1)));
    }

    public static void activateRapidAssault(Player player) {
        if (!AssassinProgression.hasSkill(player, "rapid_assault")) {
            return;
        }
        if (!hasClass(player) || !canUse(player, TAG_RAPID_ASSAULT_CD)) return;
        player.getPersistentData().putLong(TAG_RAPID_ASSAULT_UNTIL, player.level().getGameTime()
                + secondsToTicks(AssassinProgression.getResourceValue(player, 12, 0)));
        setCooldown(player, TAG_RAPID_ASSAULT_CD, secondsToTicks(
                AssassinProgression.getResourceValue(player, 12, 1)));
        if (player instanceof ServerPlayer serverPlayer) {
            AssassinNetwork.syncAbilityStateNow(serverPlayer);
        }
    }

    public static boolean isRapidAssaultActive(Player player) {
        return player != null
                && player.getPersistentData().contains(TAG_RAPID_ASSAULT_UNTIL)
                && player.level().getGameTime() < player.getPersistentData().getLong(TAG_RAPID_ASSAULT_UNTIL);
    }

    public static void endShadowVeil(Player player) {
        if (player == null) return;
        player.setInvisible(false);
        player.getPersistentData().remove(TAG_SHADOW_VEIL_UNTIL);
        VeilDecoy.dismiss(player);
    }

    public static void activateMirage(Player player, LivingEntity target) {
        if (!AssassinProgression.hasSkill(player, "mirage")) {
            return;
        }
        if (target == null || !hasClass(player) || !canUse(player, TAG_MIRAGE_CD)) return;
        if (target == player) {
            return;
        }
        double maxRange = 30.0D;
        if (player.distanceTo(target) > maxRange) {
            return;
        }
        // Recasting on the same target must not orphan the previous crowd.
        MirageEffect existing = MIRAGES.get(target.getUUID());
        if (existing != null) {
            existing.remove();
            MIRAGES.remove(target.getUUID());
        }
        int duration = secondsToTicks(AssassinProgression.getResourceValue(player, 10, 0) + 6.0D);
        MirageEffect effect = new MirageEffect(player, target);
        effect.setEffectDuration(duration);
        effect.start();
        MIRAGES.put(target.getUUID(), effect);
        // Cooldown is five times the effect duration.
        setCooldown(player, TAG_MIRAGE_CD, secondsToTicks(
                AssassinProgression.getResourceValue(player, 10, 1)));
    }

    public static void tickMirages() {
        for (Map.Entry<UUID, MirageEffect> entry : new HashMap<>(MIRAGES).entrySet()) {
            MirageEffect effect = entry.getValue();
            if (effect == null || effect.expired()) {
                MIRAGES.remove(entry.getKey());
                // Despawn the crowd; the map entry alone did not remove the clones.
                if (effect != null) effect.remove();
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
        return player != null && player.isInvisible()
                && player.getPersistentData().contains(TAG_SHADOW_VEIL_UNTIL);
    }

    public static void clearHostileTargets(Player player) {
        if (player == null || player.level().isClientSide) return;
        AABB area = player.getBoundingBox().inflate(64.0D);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area)) {
            if (mob.getTarget() == player) mob.setTarget(null);
        }
    }

    public static boolean isBehindTarget(LivingEntity attacker, LivingEntity target) {
        Vec3 targetToAttacker = attacker.position().subtract(target.position()).normalize();
        Vec3 targetForward = target.getLookAngle().normalize();
        return targetToAttacker.dot(targetForward) < -0.15D;
    }

    public static void applyAssassinDamage(LivingEntity attacker, LivingEntity target, LivingDamageEvent.Pre event) {
        if (!(attacker instanceof Player player) || !hasClass(player)) return;
        float damage = event.getNewDamage();
        if (AssassinProgression.hasSkill(player, "backstab")) {
            damage *= isBehindTarget(attacker, target) ? 2.0F : 0.5F;
        }
        long now = player.level().getGameTime();
        double echoChance = AssassinProgression.getResourceValue(player, 3, 0) / 100.0D;
        boolean shadowDanceActive = player.getPersistentData().getLong(TAG_SHADOW_DANCE_UNTIL) > now;
        if (shadowDanceActive) echoChance = 1.0D;
        echoChance = Math.min(echoChance, 1.0D);
        if (echoChance > 0.0D && RANDOM.nextDouble() < echoChance) {
            float cloneDamage = event.getOriginalDamage();
            if (AssassinProgression.hasSkill(player, "backstab")) {
                cloneDamage *= isBehindTarget(attacker, target) ? 2.0F : 0.5F;
            }
            if (isRapidAssaultActive(player)) {
                cloneDamage *= 0.1F;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                ShadowEchoHit.launch(serverPlayer, target, cloneDamage,
                        AssassinProgression.hasSkill(player, "shadow_echo_followup"));
            }
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                        target.getZ(), 6, 0.25D, 0.3D, 0.25D, 0.01D);
            }
        }
        event.setNewDamage(damage);
    }

    public static void onSuccessfulAttack(Player attacker, LivingEntity target) {
        if (!hasClass(attacker)) return;
        if (isShadowVeilActive(attacker) && !attacker.getPersistentData().contains(TAG_SHADOW_VEIL_UNTIL)) {
            endShadowVeil(attacker);
        }
        if (isShadowVeilActive(attacker)) {
            endShadowVeil(attacker);
            attacker.getPersistentData().remove(TAG_SHADOW_VEIL_UNTIL);
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

    private static boolean isTargetInView(Player player, Entity target, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
        var targetHit = target.getBoundingBox().inflate(target.getPickRadius()).clip(start, end);
        if (targetHit.isEmpty()) return false;

        var blockHit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.MISS
                || start.distanceToSqr(targetHit.get()) <= start.distanceToSqr(blockHit.getLocation());
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
