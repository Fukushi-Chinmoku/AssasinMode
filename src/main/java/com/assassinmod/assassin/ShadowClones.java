package com.assassinmod.assassin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Shared plumbing for non-player shadow clone entities. */
public final class ShadowClones {
    public static final String TWIN_MARKER = "assassin_shadow_twin";
    public static final String RENDER_MARKER = "assassin_shadow_render";
    private static final String CLONE_MARKER = "assassin_shadow_clone";

    private ShadowClones() {
    }

    public static ItemStack[] sharedEquipment(ServerPlayer source) {
        ItemStack[] gear = new ItemStack[EquipmentSlot.values().length];
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            gear[slot.ordinal()] = source.getItemBySlot(slot).copy();
        }
        return gear;
    }

    public static ShadowCloneEntity create(ServerLevel level, ServerPlayer source, ItemStack[] sharedGear) {
        return create(level, source, sharedGear, false);
    }

    public static ShadowCloneEntity createIllusion(ServerLevel level, ServerPlayer source) {
        ShadowCloneEntity clone = create(level, source, sharedEquipment(source), false);
        clone.setFullCopy(source);
        return clone;
    }

    public static ShadowCloneEntity createTwin(ServerLevel level, ServerPlayer source) {
        return create(level, source, null, true);
    }

    private static ShadowCloneEntity create(ServerLevel level, ServerPlayer source, ItemStack[] sharedGear,
                                            boolean twin) {
        ShadowCloneEntity clone = AssassinEntities.SHADOW_CLONE.get().create(level);
        if (clone == null) throw new IllegalStateException("Could not create shadow clone entity");
        clone.getPersistentData().putBoolean(CLONE_MARKER, true);
        clone.getPersistentData().putBoolean(RENDER_MARKER, true);
        if (twin) clone.getPersistentData().putBoolean(TWIN_MARKER, true);
        clone.setOwner(source);
        if (twin) {
            clone.setCustomName(null);
            clone.setCustomNameVisible(false);
        } else {
            clone.copyEquipment(sharedGear);
        }
        return clone;
    }

    public static boolean isShadowClone(Entity entity) {
        return entity instanceof ShadowCloneEntity
                && entity.getPersistentData().getBoolean(CLONE_MARKER);
    }

    public static void discard(ServerLevel level, ShadowCloneEntity clone) {
        if (!clone.isRemoved()) clone.discard();
    }
}
