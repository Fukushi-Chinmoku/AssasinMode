package com.assassinmod.assassin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Shadow Twin Strike: teleport behind the target summons two shadow clones that
 * leap at it from the left and right, strike for 150% of the owner's hit damage
 * and dissolve into dark purple smoke.
 */
public final class ShadowTwinStrike {
    private static final double STRIKE_SPEED = 0.42D;
    private static final double SPAWN_OFFSET = 2.7D;
    private static final double STRIKE_REACH = 0.8D;
    private static final double JUMP_HEIGHT = 1.1D;
    private static final List<ShadowTwinStrike> ACTIVE = new ArrayList<>();

    private final ServerPlayer source;
    private final LivingEntity target;
    private final ServerLevel level;
    private final ShadowCloneEntity[] clones = new ShadowCloneEntity[2];
    private final Vec3[] startPositions = new Vec3[2];
    private final Vec3[] directions = new Vec3[2];
    private final double[] strikeDistance = new double[2];
    private final double[] travelled = new double[2];
    private final int[] strikeDelay = {2, 4};
    private final float strikeDamage;
    private final int maxTicks;
    private int ticks;

    private ShadowTwinStrike(ServerPlayer source, LivingEntity target, ServerLevel level, float strikeDamage, int maxTicks) {
        this.source = source;
        this.target = target;
        this.level = level;
        this.strikeDamage = strikeDamage;
        this.maxTicks = maxTicks;
    }

    public static void launch(ServerPlayer source, LivingEntity target) {
        if (!(source.level() instanceof ServerLevel level)) return;
        Vec3 toTarget = target.position().subtract(source.position());
        Vec3 forward = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (forward.lengthSqr() < 0.01D) {
            Vec3 look = target.getLookAngle();
            forward = new Vec3(look.x, 0.0D, look.z);
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        float damage = (float) (source.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * AssassinProgression.getResourceValue(source, 6, 0) / 100.0D);
        int maxTicks = Math.max(1, (int) Math.round(
                AssassinProgression.getResourceValue(source, 6, 3) * 20.0D));
        ShadowTwinStrike strike = new ShadowTwinStrike(source, target, level, damage, maxTicks);
        List<ShadowCloneEntity> spawned = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            double sideSign = i == 0 ? -1.0D : 1.0D;
            Vec3 spawn = target.position()
                    .add(side.scale(sideSign * SPAWN_OFFSET))
                    .subtract(forward.scale(0.6D));
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(spawn.x), Mth.floor(spawn.z));
            ShadowCloneEntity clone = ShadowClones.createTwin(level, source);
            clone.setPos(spawn.x, groundY, spawn.z);
            strike.clones[i] = clone;
            strike.startPositions[i] = new Vec3(spawn.x, groundY, spawn.z);
            Vec3 run = target.position().subtract(spawn);
            run = new Vec3(run.x, 0.0D, run.z);
            double distance = run.length();
            if (distance < 0.01D) {
                run = forward;
                distance = 1.0D;
            }
            strike.directions[i] = run.scale(1.0D / distance);
            strike.strikeDistance[i] = Math.max(0.0D, distance - STRIKE_REACH);
            spawned.add(clone);
        }
        for (ShadowCloneEntity clone : spawned) {
            level.addFreshEntity(clone);
            level.sendParticles(ParticleTypes.PORTAL, clone.getX(), clone.getY() + 1.0D, clone.getZ(),
                    14, 0.3D, 0.5D, 0.3D, 0.3D);
            level.sendParticles(ParticleTypes.SQUID_INK, clone.getX(), clone.getY() + 1.0D, clone.getZ(),
                    4, 0.25D, 0.4D, 0.25D, 0.01D);
        }
        ACTIVE.add(strike);
    }

    public static void tickAll() {
        for (ShadowTwinStrike strike : new ArrayList<>(ACTIVE)) strike.tick();
    }

    public static void clearAll() {
        for (ShadowTwinStrike strike : new ArrayList<>(ACTIVE)) strike.finish();
        ACTIVE.clear();
    }

    private void tick() {
        ticks++;
        if (ticks >= maxTicks || !target.isAlive()) {
            finish();
            return;
        }
        for (int i = 0; i < clones.length; i++) {
            ShadowCloneEntity clone = clones[i];
            if (clone == null || ticks < strikeDelay[i]) continue;
            travelled[i] = Math.min(strikeDistance[i], travelled[i] + STRIKE_SPEED);
            double progress = strikeDistance[i] <= 0.0D ? 1.0D : travelled[i] / strikeDistance[i];
            double jump = Math.sin(progress * Math.PI) * JUMP_HEIGHT;
            Vec3 pos = startPositions[i].add(directions[i].scale(travelled[i]));
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(pos.x), Mth.floor(pos.z));
            clone.setAnimatedPos(pos.x, groundY + jump, pos.z);
            faceTarget(clone);
            clone.setSprinting(true);
            level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.9D, pos.z,
                    2, 0.15D, 0.3D, 0.15D, 0.01D);
            if (travelled[i] >= strikeDistance[i]) {
                clone.swing(InteractionHand.MAIN_HAND);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.SQUID_INK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                        8, 0.35D, 0.45D, 0.35D, 0.01D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.75F);
                target.hurt(clone.damageSources().mobAttack(clone), strikeDamage);
                // The jump ends at ground level on this same tick, so remove the twin as it lands.
                dissolve(i);
            }
        }
        if (clones[0] == null && clones[1] == null) {
            ACTIVE.remove(this);
        }
    }

    private void faceTarget(ShadowCloneEntity clone) {
        Vec3 toTarget = target.position().subtract(clone.position());
        float facing = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        clone.setYRot(facing);
        clone.setYHeadRot(facing);
        clone.setYBodyRot(facing);
    }

    private void dissolve(int i) {
        ShadowCloneEntity clone = clones[i];
        clones[i] = null;
        if (clone == null) return;
        level.sendParticles(ParticleTypes.SQUID_INK, clone.getX(), clone.getY() + 1.0D, clone.getZ(),
                5, 0.25D, 0.4D, 0.25D, 0.01D);
        ShadowClones.discard(level, clone);
    }

    private void finish() {
        for (int i = 0; i < clones.length; i++) {
            if (clones[i] != null) dissolve(i);
        }
        ACTIVE.remove(this);
    }
}
