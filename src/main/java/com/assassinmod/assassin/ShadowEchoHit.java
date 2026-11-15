package com.assassinmod.assassin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** One-clone follow-up attack triggered by the Shadow Echo Hit skill. */
public final class ShadowEchoHit {
    private static final List<ShadowEchoHit> ACTIVE = new ArrayList<>();

    private final LivingEntity target;
    private final ServerLevel level;
    private final ShadowCloneEntity clone;
    private final Vec3 start;
    private final Vec3 direction;
    private final double distance;
    private final float damage;
    private double travelled;
    private int ticks;

    private ShadowEchoHit(LivingEntity target, ServerLevel level,
                          ShadowCloneEntity clone, Vec3 start, Vec3 direction, double distance, float damage) {
        this.target = target;
        this.level = level;
        this.clone = clone;
        this.start = start;
        this.direction = direction;
        this.distance = distance;
        this.damage = damage;
    }

    public static void launch(ServerPlayer owner, LivingEntity target, float damage) {
        if (!(owner.level() instanceof ServerLevel level) || !target.isAlive() || damage <= 0.0F) return;

        Vec3 towardTarget = target.position().subtract(owner.position());
        Vec3 forward = new Vec3(towardTarget.x, 0.0D, towardTarget.z);
        if (forward.lengthSqr() < 0.01D) {
            Vec3 look = owner.getLookAngle();
            forward = new Vec3(look.x, 0.0D, look.z);
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 spawn = owner.position().subtract(forward.scale(0.35D)).add(side.scale(1.25D));
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(spawn.x), Mth.floor(spawn.z));
        spawn = new Vec3(spawn.x, groundY, spawn.z);

        Vec3 run = target.position().subtract(spawn);
        run = new Vec3(run.x, 0.0D, run.z);
        double distance = run.length();
        Vec3 direction = distance > 0.01D ? run.scale(1.0D / distance) : forward;
        ShadowCloneEntity clone = ShadowClones.createTwin(level, owner);
        clone.setPos(spawn.x, spawn.y, spawn.z);
        faceTarget(clone, target);

        ShadowEchoHit hit = new ShadowEchoHit(target, level, clone, spawn, direction, distance, damage);
        level.addFreshEntity(clone);
        level.sendParticles(ParticleTypes.SQUID_INK, spawn.x, spawn.y + 1.0D, spawn.z,
                8, 0.2D, 0.4D, 0.2D, 0.01D);
        ACTIVE.add(hit);
    }

    public static void tickAll() {
        for (ShadowEchoHit hit : new ArrayList<>(ACTIVE)) hit.tick();
    }

    public static void clearAll() {
        for (ShadowEchoHit hit : new ArrayList<>(ACTIVE)) hit.finish();
        ACTIVE.clear();
    }

    private void tick() {
        if (!clone.isAlive() || !target.isAlive() || ++ticks > 12) {
            finish();
            return;
        }

        travelled = Math.min(distance, travelled + 0.6D);
        double progress = distance <= 0.01D ? 1.0D : travelled / distance;
        Vec3 position = start.add(direction.scale(travelled));
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(position.x), Mth.floor(position.z));
        double jump = Math.sin(progress * Math.PI) * 0.45D;
        clone.setAnimatedPos(position.x, groundY + jump, position.z);
        faceTarget(clone, target);
        clone.setSprinting(true);
        level.sendParticles(ParticleTypes.SQUID_INK, position.x, position.y + 0.8D, position.z,
                2, 0.12D, 0.2D, 0.12D, 0.01D);

        if (progress >= 1.0D || clone.distanceToSqr(target) <= 1.0D) {
            clone.swing(InteractionHand.MAIN_HAND);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.15F);
            target.invulnerableTime = Math.min(target.invulnerableTime, 10);
            target.hurt(clone.damageSources().mobAttack(clone), damage);
            finish();
        }
    }

    private static void faceTarget(ShadowCloneEntity clone, LivingEntity target) {
        Vec3 toTarget = target.position().subtract(clone.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        clone.setYRot(yaw);
        clone.setYHeadRot(yaw);
        clone.setYBodyRot(yaw);
    }

    private void finish() {
        ACTIVE.remove(this);
        if (!clone.isRemoved()) {
            level.sendParticles(ParticleTypes.SQUID_INK, clone.getX(), clone.getY() + 1.0D, clone.getZ(),
                    5, 0.2D, 0.35D, 0.2D, 0.01D);
            ShadowClones.discard(level, clone);
        }
    }
}
