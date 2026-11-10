package com.assassinmod.assassin;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AssassinMod.MOD_ID)
public class AssassinMod {
    public static final String MOD_ID = "assassin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AssassinMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::commonSetup);
        modBus.addListener(AssassinNetwork::register);

        NeoForge.EVENT_BUS.addListener(AssassinEventHandler::onLivingHurt);
        NeoForge.EVENT_BUS.addListener(AssassinEventHandler::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(AssassinEventHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(AssassinEventHandler::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(AssassinEventHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(AssassinCommands::register);
        NeoForge.EVENT_BUS.addListener(AssassinEventHandler::onPlayerClone);
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.addListener(AssassinKeyMappings::onClientTick);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Assassin mod configured.");
    }

    public static boolean isAssassinPlayer(ServerPlayer player) {
        return AssassinAbilityManager.hasClass(player);
    }
}
