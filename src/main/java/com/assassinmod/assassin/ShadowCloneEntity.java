package com.assassinmod.assassin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class ShadowCloneEntity extends ArmorStand {
    private static final EntityDataAccessor<String> OWNER_ID =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANIMATION_PHASE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FULL_COPY =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LEFT_HANDED =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.BOOLEAN);

    public ShadowCloneEntity(net.minecraft.world.entity.EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
        setNoBasePlate(true);
        setShowArms(true);
        setNoGravity(true);
        setSilent(true);
        setInvulnerable(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, "");
        builder.define(ANIMATION_SPEED, 0.0F);
        builder.define(ANIMATION_PHASE, 0.0F);
        builder.define(FULL_COPY, false);
        builder.define(LEFT_HANDED, false);
    }

    public void setOwner(ServerPlayer owner) {
        entityData.set(OWNER_ID, owner.getUUID().toString());
        entityData.set(LEFT_HANDED, owner.getMainArm() == HumanoidArm.LEFT);
        setCustomName(null);
        setCustomNameVisible(false);
    }

    @Override
    public HumanoidArm getMainArm() {
        return entityData.get(LEFT_HANDED) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public UUID getOwnerId() {
        String ownerId = entityData.get(OWNER_ID);
        if (ownerId.isEmpty()) return null;
        try {
            return UUID.fromString(ownerId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public void showOwnerName(ServerPlayer owner) {
        setCustomName(owner.getDisplayName());
        setCustomNameVisible(true);
    }

    public void setFullCopy(ServerPlayer owner) {
        entityData.set(FULL_COPY, true);
        showOwnerName(owner);
    }

    public boolean isFullCopy() {
        return entityData.get(FULL_COPY);
    }

    public float getAnimationSpeed() {
        return entityData.get(ANIMATION_SPEED);
    }

    public float getAnimationPhase() {
        return entityData.get(ANIMATION_PHASE);
    }

    public void setAnimatedPos(double x, double y, double z) {
        double dx = x - getX();
        double dz = z - getZ();
        float speed = (float) Math.min(Math.sqrt(dx * dx + dz * dz) * 4.0D, 1.0D);
        entityData.set(ANIMATION_SPEED, speed);
        entityData.set(ANIMATION_PHASE, entityData.get(ANIMATION_PHASE) + speed);
        setPos(x, y, z);
    }

    public void copyEquipment(ItemStack[] gear) {
        if (gear == null) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, gear[slot.ordinal()].copy());
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker
                && VeilDecoy.onCloneHit(this, attacker)) {
            return true;
        }
        if (amount > 0.0F && !isInvulnerableTo(source)) {
            discard();
            return true;
        }
        return super.hurt(source, amount);
    }
}
