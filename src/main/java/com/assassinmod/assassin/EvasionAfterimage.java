package com.assassinmod.assassin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** A harmless shadow that bursts out behind the player when an attack is dodged. */
public final class EvasionAfterimage {
    private static final List<EvasionAfterimage> ACTIVE = new ArrayList<>();

    private final ServerLevel level;
    private final ShadowCloneEntity clone;
    private final Vec3 start;
    private final Vec3 direction;
    private int ticks;

    private EvasionAfterimage(ServerLevel level, ShadowCloneEntity clone, Vec3 start, Vec3 direction) {
        this.level = level;
        this.clone = clone;
        this.start = start;
        this.direction = direction;
    }

    public static void launch(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        Vec3 look = owner.getLookAngle();
        Vec3 backward = new Vec3(-look.x, 0.0D, -look.z);
        if (backward.lengthSqr() < 0.01D) backward = new Vec3(0.0D, 0.0D, 1.0D);
        backward = backward.normalize();

        Vec3 start = owner.position().add(0.0D, 0.05D, 0.0D).add(backward.scale(0.8D));
        ShadowCloneEntity clone = ShadowClones.createTwin(level, owner);
        clone.setPos(start.x, start.y, start.z);
        face(clone, backward);
        EvasionAfterimage afterimage = new EvasionAfterimage(level, clone, start, backward);

        level.addFreshEntity(clone);
        level.sendParticles(ParticleTypes.SQUID_INK, start.x, start.y + 0.9D, start.z,
                7, 0.15D, 0.4D, 0.15D, 0.01D);
        ACTIVE.add(afterimage);
    }

    public static void tickAll() {
        for (EvasionAfterimage afterimage : new ArrayList<>(ACTIVE)) afterimage.tick();
    }

    public static void clearAll() {
        for (EvasionAfterimage afterimage : new ArrayList<>(ACTIVE)) afterimage.finish();
        ACTIVE.clear();
    }

    private void tick() {
        if (!clone.isAlive() || ++ticks > 8) {
            finish();
            return;
        }
        double progress = ticks / 8.0D;
        Vec3 position = start.add(direction.scale(progress * 2.2D))
                .add(0.0D, Math.sin(progress * Math.PI) * 0.25D, 0.0D);
        clone.setAnimatedPos(position.x, position.y, position.z);
        face(clone, direction);
        clone.setSprinting(true);
        level.sendParticles(ParticleTypes.SQUID_INK, position.x, position.y + 0.8D, position.z,
                2, 0.08D, 0.15D, 0.08D, 0.01D);
    }

    private static void face(ShadowCloneEntity clone, Vec3 direction) {
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        clone.setYRot(yaw);
        clone.setYHeadRot(yaw);
        clone.setYBodyRot(yaw);
    }

    private void finish() {
        ACTIVE.remove(this);
        if (!clone.isRemoved()) {
            level.sendParticles(ParticleTypes.SQUID_INK, clone.getX(), clone.getY() + 0.8D, clone.getZ(),
                    4, 0.15D, 0.25D, 0.15D, 0.01D);
            ShadowClones.discard(level, clone);
        }
    }
}
