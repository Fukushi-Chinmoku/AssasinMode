package com.assassinmod.assassin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MirageEffect {
    private final Player source;
    private final LivingEntity target;
    private final List<FakePlayer> clones = new ArrayList<>();
    private int ticks;
    private int attackTimer;

    public MirageEffect(Player source, LivingEntity target) {
        this.source = source;
        this.target = target;
    }

    public void start() {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "Mirage_" + i);
            FakePlayer clone = FakePlayerFactory.get(level, profile);
            clone.setPos(target.getX() + (i % 2 == 0 ? 1.5 : -1.5), target.getY(), target.getZ() + (i % 2 == 0 ? 1.5 : -1.5));
            clone.setDeltaMovement(0, 0, 0);
            clone.setInvisible(false);
            clone.setCustomNameVisible(false);
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
        attackTimer--;
        if (ticks >= 140) {
            remove();
            return;
        }

        Vec3 center = new Vec3(target.getX(), target.getY() + 0.8D, target.getZ());
        for (int i = 0; i < clones.size(); i++) {
            FakePlayer clone = clones.get(i);
            if (clone == null || !clone.isAlive()) {
                continue;
            }
            double angle = (ticks * 0.12D) + i * 1.9D;
            double radius = 2.3D + (i * 0.5D);
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double targetX = center.x + offsetX;
            double targetY = center.y + Math.sin((ticks + i) * 0.28D) * 0.4D;
            double targetZ = center.z + offsetZ;
            clone.setPos(targetX, targetY, targetZ);
            clone.setYRot((float) (Math.toDegrees(Math.atan2(targetZ - center.z, targetX - center.x)) + 180.0D));
            clone.setXRot(0.0F);
        }

        if (attackTimer <= 0) {
            attackTimer = 50 + (int) (Math.random() * 60);
            if (!clones.isEmpty()) {
                FakePlayer selected = clones.get((int) (Math.random() * clones.size()));
                if (selected != null && selected.isAlive()) {
                    Vec3 dir = target.position().subtract(selected.position()).normalize();
                    selected.setDeltaMovement(dir.scale(0.45D));
                    selected.setPos(selected.getX() + dir.x * 0.35D, selected.getY(), selected.getZ() + dir.z * 0.35D);
                }
            }
        }
    }

    public boolean expired() {
        return ticks >= 140 || target == null || !target.isAlive();
    }

    public void remove() {
        for (FakePlayer clone : new ArrayList<>(clones)) {
            if (clone != null && clone.isAlive()) {
                clone.discard();
            }
        }
        clones.clear();
    }

    public boolean isClone(Entity entity) {
        for (FakePlayer clone : clones) {
            if (clone == entity) return true;
        }
        return false;
    }
}
