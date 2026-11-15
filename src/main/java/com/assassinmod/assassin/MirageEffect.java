package com.assassinmod.assassin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MirageEffect {
    private static final double ATTACK_REACH = 1.15D;
    private static final double ATTACK_JUMP_HEIGHT = 0.75D;
    private final Player source;
    private final LivingEntity target;
    private static final int CLONE_COUNT = 16;
    private int effectDuration;
    private final List<ShadowCloneEntity> clones = new ArrayList<>();
    private int ticks;
    private int nextAttackerIndex;
    private int nextAttackTick = 16;
    private UUID activeAttackerId;
    private Vec3 attackStart;
    private Vec3 attackDirection;
    private double attackDistance;
    private double distanceToTarget;
    private double attackTravelled;
    private boolean returningToCrowd;
    private boolean attackSwingPlayed;

    public MirageEffect(Player source, LivingEntity target) {
        this.source = source;
        this.target = target;
        this.effectDuration = 120;
    }

    public void setEffectDuration(int effectDuration) {
        this.effectDuration = Math.max(1, effectDuration);
    }

    public void start() {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer sourcePlayer = source instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (sourcePlayer == null) return;
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10, 0, false, false, true));
        List<ShadowCloneEntity> spawned = new ArrayList<>(CLONE_COUNT);
        for (int i = 0; i < CLONE_COUNT; i++) {
            ShadowCloneEntity clone = ShadowClones.createIllusion(level, sourcePlayer);
            double angle = crowdAngle(i, 0);
            double radius = crowdRadius(i, 0);
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
            clone.setPos(x, y, z);
            spawned.add(clone);
            AssassinAbilityManager.markMirageClone(clone);
        }
        for (ShadowCloneEntity clone : spawned) {
            level.addFreshEntity(clone);
        }
        clones.addAll(spawned);
    }

    public void tick() {
        if (target == null || !target.isAlive()) {
            remove();
            return;
        }

        ticks++;
        if (ticks >= effectDuration) {
            remove();
            return;
        }

        if (target instanceof Mob mob && !clones.isEmpty()) {
            ShadowCloneEntity decoy = clones.stream().filter(ShadowCloneEntity::isAlive)
                    .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            if (decoy != null && mob.getTarget() != decoy) {
                mob.setTarget(decoy);
            }
        }

        Vec3 center = target.position();
        ShadowCloneEntity activeAttacker = findActiveAttacker();
        if (activeAttacker == null && ticks >= nextAttackTick) {
            activeAttacker = chooseNextAttacker(center);
        }

        for (int i = 0; i < clones.size(); i++) {
            ShadowCloneEntity clone = clones.get(i);
            if (clone == null || !clone.isAlive()) {
                continue;
            }
            double angle = crowdAngle(i, ticks);
            double radius = crowdRadius(i, ticks);
            double targetX = center.x + Math.cos(angle) * radius;
            double targetZ = center.z + Math.sin(angle) * radius;
            boolean isAttacker = activeAttacker == clone;
            if (isAttacker && !returningToCrowd) {
                attackTravelled = Math.min(attackDistance, attackTravelled + attackMoveSpeed());
                targetX = attackStart.x + attackDirection.x * attackTravelled;
                targetZ = attackStart.z + attackDirection.z * attackTravelled;
            }
            int blockX = net.minecraft.util.Mth.floor(targetX);
            int blockZ = net.minecraft.util.Mth.floor(targetZ);
            double targetY = target.level() instanceof ServerLevel serverLevel
                    ? serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ)
                    : target.getY();
            boolean attacking = isAttacker && !returningToCrowd;
            if (attacking && attackDistance > 0.0D) {
                double progress = attackTravelled / attackDistance;
                targetY += Math.sin(progress * Math.PI) * ATTACK_JUMP_HEIGHT;
            }
            boolean sprinting = attacking || isCloneSprinting(i, ticks);
            clone.setSprinting(sprinting);
            moveClone(clone, targetX, targetY, targetZ, attacking ? attackMoveSpeed() : playerMoveSpeed(sprinting));
            clone.setXRot(0.0F);
            if (attacking && attackTravelled >= attackDistance) {
                if (!attackSwingPlayed) {
                    clone.swing(InteractionHand.MAIN_HAND);
                    attackSwingPlayed = true;
                }
                returningToCrowd = true;
            } else if (isAttacker && returningToCrowd
                    && clone.distanceToSqr(targetX, targetY, targetZ) < 0.8D * 0.8D) {
                resetActiveAttack();
                nextAttackTick = ticks + 4;
            }
            if (i % 16 == ticks % 16 && target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, targetX, targetY + 0.9D, targetZ,
                        1, 0.12D, 0.18D, 0.12D, 0.2D);
            }
        }

        if (ticks % 2 == 0 && target.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 10; i++) {
                double angle = ticks * 0.035D + i * (Math.PI * 2.0D / 10.0D)
                        + Math.sin(ticks * 0.009D + i * 2.3D) * 0.22D;
                double radius = 1.8D + (i % 4) * 1.55D
                        + Math.sin(ticks * 0.013D + i * 1.9D) * 0.55D;
                double fogX = center.x + Math.cos(angle) * radius;
                double fogZ = center.z + Math.sin(angle) * radius;
                double floorY = serverLevel.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        net.minecraft.util.Mth.floor(fogX), net.minecraft.util.Mth.floor(fogZ));
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        fogX, floorY + 0.04D, fogZ, 1,
                        0.45D, 0.025D, 0.45D, 0.002D);
            }
        }

    }

    public boolean expired() {
        return ticks >= effectDuration || target == null || !target.isAlive();
    }

    private double crowdAngle(int index, int tick) {
        // Wider speed spread, alternating directions and several drift terms keep the
        // crowd walking out of step instead of marching in lockstep.
        double individualSpeed = 0.005D + ((index * 37) % 7) * 0.0021D;
        double direction = (index % 2 == 0) ? 1.0D : -1.0D;
        double occasionalTurn = Math.sin(tick * 0.004D + index * 1.71D) * 0.006D;
        return index * 2.399963229728653D + tick * (individualSpeed * direction + occasionalTurn)
                + Math.sin(tick * 0.027D + index * 2.13D) * 0.5D
                + Math.sin(tick * 0.071D + index * 4.93D) * 0.3D
                + Math.sin(tick * 0.013D + index * 0.37D) * 0.22D
                + Math.sin(tick * 0.052D + index * 3.31D) * 0.18D;
    }

    private double crowdRadius(int index, int tick) {
        return Math.max(5.2D, 6.6D + ((index * 17) % 11) * 0.19D
                + Math.sin(tick * 0.017D + index * 1.77D) * 0.95D
                + Math.sin(tick * 0.043D + index * 2.91D) * 0.6D
                + Math.sin(tick * 0.006D + index * 0.41D) * 0.5D);
    }

    private double playerMoveSpeed(boolean sprinting) {
        double baseSpeed = source.getAttributeValue(Attributes.MOVEMENT_SPEED);
        return Math.max(0.04D, baseSpeed * (sprinting ? 1.3D : 1.0D));
    }

    private double attackMoveSpeed() {
        return playerMoveSpeed(true) * 2.0D;
    }

    private boolean isCloneSprinting(int index, int tick) {
        double pace = Math.sin(tick * (0.025D + (index % 5) * 0.003D) + index * 2.17D)
                + Math.sin(tick * 0.009D + index * 0.73D) * 0.45D;
        return pace > 0.48D;
    }

    private ShadowCloneEntity findActiveAttacker() {
        if (activeAttackerId == null) return null;
        for (ShadowCloneEntity clone : clones) {
            if (clone.getUUID().equals(activeAttackerId) && clone.isAlive()) return clone;
        }
        resetActiveAttack();
        nextAttackTick = ticks + 4;
        return null;
    }

    private ShadowCloneEntity chooseNextAttacker(Vec3 center) {
        if (clones.isEmpty()) return null;
        for (int offset = 0; offset < clones.size(); offset++) {
            int index = Math.floorMod(nextAttackerIndex + offset, clones.size());
            ShadowCloneEntity clone = clones.get(index);
            if (clone == null || !clone.isAlive()) continue;
            nextAttackerIndex = (index + 1) % clones.size();
            activeAttackerId = clone.getUUID();
            attackStart = clone.position();
            double dx = center.x - attackStart.x;
            double dz = center.z - attackStart.z;
            distanceToTarget = Math.sqrt(dx * dx + dz * dz);
            if (distanceToTarget < 0.001D) {
                dx = 1.0D;
                distanceToTarget = 1.0D;
            }
            attackDirection = new Vec3(dx / distanceToTarget, 0.0D, dz / distanceToTarget);
            attackDistance = Math.max(0.0D, distanceToTarget - ATTACK_REACH);
            attackTravelled = 0.0D;
            returningToCrowd = false;
            attackSwingPlayed = false;
            return clone;
        }
        return null;
    }

    private void resetActiveAttack() {
        activeAttackerId = null;
        attackStart = null;
        attackDirection = null;
        attackDistance = 0.0D;
        distanceToTarget = 0.0D;
        attackTravelled = 0.0D;
        returningToCrowd = false;
        attackSwingPlayed = false;
    }

    private void moveClone(ShadowCloneEntity clone, double x, double y, double z, double speed) {
        double dx = x - clone.getX();
        double dz = z - clone.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double scale = horizontalDistance > speed && horizontalDistance > 0.0D
                ? speed / horizontalDistance : 1.0D;
        double nextX = clone.getX() + dx * scale;
        double nextZ = clone.getZ() + dz * scale;
        double nextY = clone.getY() + net.minecraft.util.Mth.clamp(y - clone.getY(), -0.28D, 0.28D);
        double movedX = nextX - clone.getX();
        double movedZ = nextZ - clone.getZ();
        if (movedX * movedX + movedZ * movedZ > 0.0001D) {
            float facing = (float) Math.toDegrees(Math.atan2(-movedX, movedZ));
            clone.setYRot(facing);
            clone.setYHeadRot(facing);
            clone.setYBodyRot(facing);
        }
        clone.setAnimatedPos(nextX, nextY, nextZ);
    }

    public void remove() {
        for (ShadowCloneEntity clone : new ArrayList<>(clones)) {
            removeClone(clone);
        }
        clones.clear();
    }

    public boolean isClone(Entity entity) {
        for (ShadowCloneEntity clone : clones) {
            if (clone == entity) return true;
        }
        return false;
    }

    public boolean removeClone(Entity entity) {
        for (int i = 0; i < clones.size(); i++) {
            ShadowCloneEntity clone = clones.get(i);
            if (clone == entity) {
                clones.remove(i);
                if (clone.getUUID().equals(activeAttackerId)) {
                    resetActiveAttack();
                    nextAttackTick = ticks + 4;
                }
                if (clone.isAlive()) {
                    clone.discard();
                }
                return true;
            }
        }
        return false;
    }
}
