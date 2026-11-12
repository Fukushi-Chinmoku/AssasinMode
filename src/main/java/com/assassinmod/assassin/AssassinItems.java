package com.assassinmod.assassin;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AssassinItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AssassinMod.MOD_ID);
    public static final DeferredItem<AssassinTwinBladesItem> TWIN_BLADES = ITEMS.register("assassin_twin_blades",
            () -> new AssassinTwinBladesItem(new Item.Properties().stacksTo(1).durability(850).rarity(Rarity.RARE)));

    private AssassinItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(AssassinItems::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) event.accept(TWIN_BLADES);
    }
}
