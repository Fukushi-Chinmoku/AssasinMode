package com.assassinmod.assassin;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = AssassinMod.MOD_ID, value = Dist.CLIENT)
public final class AssassinClient {
    private AssassinClient() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
        AssassinKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "assassin_hud"), (guiGraphics, deltaTracker) -> {
            AssassinHudRenderer.render(guiGraphics);
        });
    }
}
