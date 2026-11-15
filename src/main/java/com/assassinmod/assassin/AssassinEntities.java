package com.assassinmod.assassin;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AssassinEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AssassinMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ShadowCloneEntity>> SHADOW_CLONE =
            ENTITY_TYPES.register("shadow_clone", () -> EntityType.Builder
                    .<ShadowCloneEntity>of(ShadowCloneEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("shadow_clone"));

    private AssassinEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(AssassinEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = ArmorStand.createAttributes().build();
        event.put(SHADOW_CLONE.get(), attributes);
    }
}
