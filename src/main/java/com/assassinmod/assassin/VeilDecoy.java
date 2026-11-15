package com.assassinmod.assassin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Deceptive Shadow upgrade: while the caster is veiled, a shadow clone stands in
 * their place, wanders chaotically around the cast point and pulls mob aggro.
 */
public final class VeilDecoy {
    private static final Map<UUID, VeilDecoy> ACTIVE = new HashMap<>();
    private static final double WANDER_RADIUS = 6.0D;
    private static final int MAX_TICKS = 400;

    private final ServerPlayer owner;
    private final ServerLevel level;
    private final ShadowCloneEntity clone;
    private final Vec3 origin;
    private final int lifetime;
    private Vec3 wanderTarget;
    private int nextWanderTick;
    private int idleFacing;
    private int ticks;
    private boolean removed;

    private VeilDecoy(ServerPlayer owner, ServerLevel level, ShadowCloneEntity clone, int lifetime) {
        this.owner = owner;
        this.level = level;
        this.clone = clone;
        this.origin = clone.position();
        this.lifetime = lifetime;
    }

    public static void spawn(ServerPlayer owner, int veilDuration) {
        spawn(owner, veilDuration, owner.position());
    }

    public static void spawn(ServerPlayer owner, int duration, Vec3 position) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        dismiss(owner);
        ItemStack[] gear = ShadowClones.sharedEquipment(owner);
        ShadowCloneEntity clone = ShadowClones.create(level, owner, gear);
        clone.setFullCopy(owner);
        clone.setPos(position.x, position.y, position.z);
        level.addFreshEntity(clone);
        ACTIVE.put(owner.getUUID(), new VeilDecoy(owner, level, clone, duration));
    }

    public static void tickAll() {
        for (VeilDecoy decoy : new ArrayList<>(ACTIVE.values())) decoy.tick();
    }

    public static void dismiss(Player owner) {
        if (owner == null) return;
        VeilDecoy decoy = ACTIVE.remove(owner.getUUID());
        if (decoy != null) decoy.despawn();
    }

    public static void clearAll() {
        for (VeilDecoy decoy : new ArrayList<>(ACTIVE.values())) decoy.despawn();
        ACTIVE.clear();
    }

    public static boolean onCloneHit(ShadowCloneEntity clone, LivingEntity attacker) {
        if (clone == null) return false;
        VeilDecoy decoy = ACTIVE.values().stream()
                .filter(active -> active.clone == clone)
                .findFirst().orElse(null);
        if (decoy == null) return false;
        if (attacker != null && attacker.isAlive()) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 7, false, false, true));
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, false, false, true));
        }
        for (Mob mob : decoy.level.getEntitiesOfClass(Mob.class, clone.getBoundingBox().inflate(32.0D))) {
            if (mob.getTarget() == clone) mob.setTarget(null);
        }
        decoy.despawn();
        return true;
    }

    private void tick() {
        if (removed) return;
        ticks++;
        if (!owner.isAlive() || ticks >= Math.min(lifetime, MAX_TICKS) || !clone.isAlive()) {
            despawn();
            return;
        }
        wanderChaotically();
        pullAggro();
    }

    private void wanderChaotically() {
        if (ticks >= nextWanderTick) {
            // A third of the time the clone just stops, facing wherever it feels like.
            if (level.random.nextInt(3) == 0) {
                wanderTarget = null;
                idleFacing = level.random.nextInt(360);
                nextWanderTick = ticks + 10 + level.random.nextInt(25);
            } else {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                double radius = 1.0D + level.random.nextDouble() * WANDER_RADIUS;
                wanderTarget = new Vec3(origin.x + Math.cos(angle) * radius, 0.0D,
                        origin.z + Math.sin(angle) * radius);
                nextWanderTick = ticks + 25 + level.random.nextInt(40);
            }
        }
        if (wanderTarget != null) {
            double dx = wanderTarget.x - clone.getX();
            double dz = wanderTarget.z - clone.getZ();
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr > 0.4D * 0.4D) {
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        Mth.floor(wanderTarget.x), Mth.floor(wanderTarget.z));
                boolean sprinting = shouldSprint();
                double speed = Math.max(0.04D, owner.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        * (sprinting ? 1.3D : 1.0D));
                double distance = Math.sqrt(distanceSqr);
                double step = Math.min(speed, distance);
                double nextX = clone.getX() + dx / distance * step;
                double nextZ = clone.getZ() + dz / distance * step;
                double nextY = clone.getY() + Mth.clamp(groundY - clone.getY(), -0.3D, 0.3D);
                clone.setSprinting(sprinting);
                float facing = (float) Math.toDegrees(Math.atan2(-dx, dz));
                clone.setYRot(facing);
                clone.setYHeadRot(facing);
                clone.setYBodyRot(facing);
                clone.setAnimatedPos(nextX, nextY, nextZ);
            } else {
                wanderTarget = null;
                nextWanderTick = ticks;
                clone.setSprinting(false);
                clone.setAnimatedPos(clone.getX(), clone.getY(), clone.getZ());
            }
        } else {
            clone.setSprinting(false);
            clone.setAnimatedPos(clone.getX(), clone.getY(), clone.getZ());
            clone.setYRot(idleFacing);
            clone.setYHeadRot(idleFacing);
            clone.setYBodyRot(idleFacing);
        }
    }

    private boolean shouldSprint() {
        return Math.sin(ticks * 0.047D + owner.getId() * 1.31D)
                + Math.sin(ticks * 0.013D + owner.getId() * 0.37D) * 0.35D > 0.15D;
    }

    /** While veiled the owner cannot be targeted — funnel that aggro into the decoy. */
    private void pullAggro() {
        if (ticks % 10 != 0) return;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, clone.getBoundingBox().inflate(24.0D))) {
            if (mob.getTarget() == owner || mob.getTarget() == null) {
                mob.setTarget(clone);
            }
        }
    }

    private void despawn() {
        if (removed) return;
        removed = true;
        if (clone.isAlive()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                clone.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        ShadowClones.discard(level, clone);
        ACTIVE.remove(owner.getUUID(), this);
    }
}
