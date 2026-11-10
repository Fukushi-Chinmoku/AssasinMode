package com.assassinmod.assassin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MirageEffect {
    private static final int CLONE_COUNT = 84;
    private static final int EFFECT_DURATION = 600;
    private static final double RUN_SPEED = 0.28D;
    private final Player source;
    private final LivingEntity target;
    private final List<FakePlayer> clones = new ArrayList<>();
    private int ticks;
    private int nextAttackerIndex;
    private int nextAttackTick = 40;
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
    }

    public void start() {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }
        var skinProperties = source instanceof ServerPlayer serverPlayer
                ? serverPlayer.getGameProfile().getProperties() : null;
        String playerName = source instanceof ServerPlayer serverPlayer
                ? serverPlayer.getGameProfile().getName() : "Player";
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_DURATION, 0, false, false, true));
        for (int i = 0; i < CLONE_COUNT; i++) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), playerName);
            if (skinProperties != null) {
                profile.getProperties().putAll(skinProperties);
            }
            FakePlayer clone = FakePlayerFactory.get(level, profile);
            double angle = crowdAngle(i, 0);
            double radius = crowdRadius(i, 0);
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
            clone.setPos(x, y, z);
            clone.setDeltaMovement(0, 0, 0);
            clone.setInvisible(false);
            clone.setGlowingTag(true);
            clone.setCustomName(null);
            clone.setCustomNameVisible(false);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                clone.setItemSlot(slot, source.getItemBySlot(slot).copy());
            }
            var playerInfo = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(clone));
            level.players().forEach(viewer -> viewer.connection.send(playerInfo));
            var unlistClone = new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, clone);
            level.players().forEach(viewer -> viewer.connection.send(unlistClone));
            level.addFreshEntity(clone);
            clones.add(clone);
            AssassinAbilityManager.markMirageClone(clone);
        }
    }

    public void tick() {
        if (target == null || !target.isAlive()) {
            remove();
            return;
        }

        ticks++;
        if (ticks >= EFFECT_DURATION) {
            remove();
            return;
        }

        if (target instanceof Mob mob && !clones.isEmpty()) {
            FakePlayer decoy = clones.stream().filter(FakePlayer::isAlive)
                    .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            if (decoy != null && mob.getTarget() != decoy) {
                mob.setTarget(decoy);
            }
        }

        Vec3 center = target.position();
        FakePlayer activeAttacker = findActiveAttacker();
        if (activeAttacker == null && ticks >= nextAttackTick) {
            activeAttacker = chooseNextAttacker(center);
        }

        for (int i = 0; i < clones.size(); i++) {
            FakePlayer clone = clones.get(i);
            if (clone == null || !clone.isAlive()) {
                continue;
            }
            double angle = crowdAngle(i, ticks);
            double radius = crowdRadius(i, ticks);
            double targetX = center.x + Math.cos(angle) * radius;
            double targetZ = center.z + Math.sin(angle) * radius;
            double jump = 0.0D;
            boolean isAttacker = activeAttacker == clone;
            if (isAttacker && !returningToCrowd) {
                attackTravelled = Math.min(attackDistance, attackTravelled + RUN_SPEED);
                double progress = attackDistance <= 0.0D ? 1.0D : attackTravelled / attackDistance;
                targetX = attackStart.x + attackDirection.x * attackTravelled;
                targetZ = attackStart.z + attackDirection.z * attackTravelled;
                jump = Math.sin(progress * Math.PI) * 1.2D;
                if (!attackSwingPlayed && attackTravelled >= distanceToTarget) {
                    clone.swing(InteractionHand.MAIN_HAND);
                    attackSwingPlayed = true;
                }
                if (attackTravelled >= attackDistance) {
                    returningToCrowd = true;
                }
            } else {
                clone.setSprinting(true);
            }
            int blockX = net.minecraft.util.Mth.floor(targetX);
            int blockZ = net.minecraft.util.Mth.floor(targetZ);
            double targetY = target.level() instanceof ServerLevel serverLevel
                    ? serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ)
                    : target.getY();
            targetY += jump;

            clone.setSprinting(true);
            moveClone(clone, targetX, targetY, targetZ, RUN_SPEED);
            clone.setXRot(0.0F);
            if (isAttacker && returningToCrowd && clone.distanceToSqr(targetX, targetY, targetZ) < 0.8D * 0.8D) {
                resetActiveAttack();
                nextAttackTick = ticks + 8;
            }
            if (i % 10 == ticks % 10 && target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, targetX, targetY + 0.9D, targetZ,
                        1, 0.12D, 0.18D, 0.12D, 0.005D);
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
        return ticks >= EFFECT_DURATION || target == null || !target.isAlive();
    }

    private double crowdAngle(int index, int tick) {
        double individualSpeed = 0.007D + ((index * 37) % 7) * 0.0012D;
        double direction = (index % 2 == 0) ? 1.0D : -1.0D;
        double occasionalTurn = Math.sin(tick * 0.004D + index * 1.71D) * 0.003D;
        return index * 2.399963229728653D + tick * (individualSpeed * direction + occasionalTurn)
                + Math.sin(tick * 0.027D + index * 2.13D) * 0.42D
                + Math.sin(tick * 0.071D + index * 4.93D) * 0.22D
                + Math.sin(tick * 0.013D + index * 0.37D) * 0.14D;
    }

    private double crowdRadius(int index, int tick) {
        return Math.max(4.0D, 5.3D + ((index * 17) % 11) * 0.16D
                + Math.sin(tick * 0.017D + index * 1.77D) * 0.75D
                + Math.sin(tick * 0.043D + index * 2.91D) * 0.48D
                + Math.sin(tick * 0.006D + index * 0.41D) * 0.38D);
    }

    private FakePlayer findActiveAttacker() {
        if (activeAttackerId == null) return null;
        for (FakePlayer clone : clones) {
            if (clone.getUUID().equals(activeAttackerId) && clone.isAlive()) return clone;
        }
        resetActiveAttack();
        nextAttackTick = ticks + 8;
        return null;
    }

    private FakePlayer chooseNextAttacker(Vec3 center) {
        if (clones.isEmpty()) return null;
        for (int offset = 0; offset < clones.size(); offset++) {
            int index = Math.floorMod(nextAttackerIndex + offset, clones.size());
            FakePlayer clone = clones.get(index);
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
            attackDistance = distanceToTarget + 3.5D;
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

    private void moveClone(FakePlayer clone, double x, double y, double z, double speed) {
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
        clone.setPos(nextX, nextY, nextZ);
    }

    public void remove() {
        for (FakePlayer clone : new ArrayList<>(clones)) {
            removeClone(clone);
        }
        clones.clear();
    }

    public boolean isClone(Entity entity) {
        for (FakePlayer clone : clones) {
            if (clone == entity) return true;
        }
        return false;
    }

    public boolean removeClone(Entity entity) {
        for (int i = 0; i < clones.size(); i++) {
            FakePlayer clone = clones.get(i);
            if (clone == entity) {
                clones.remove(i);
                if (clone.getUUID().equals(activeAttackerId)) {
                    resetActiveAttack();
                    nextAttackTick = ticks + 8;
                }
                if (clone.isAlive()) {
                    clone.discard();
                }
                if (clone.level() instanceof ServerLevel level) {
                    var removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(clone.getUUID()));
                    level.players().forEach(viewer -> viewer.connection.send(removeInfo));
                }
                return true;
            }
        }
        return false;
    }
}
