package com.dog.servertransfer;

import com.dog.servertransfer.command.TransferCommandRegistry;
import com.dog.servertransfer.config.TransferConfig;
import com.dog.servertransfer.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(ServerTransferMod.MOD_ID)
public class ServerTransferMod {
    public static final String MOD_ID = "servertransfer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ServerTransferMod() {
        NetworkHandler.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TransferConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        TransferCommandRegistry.registerCommands(event.getServer());
    }
}
