package com.dog.servertransfer;

import com.dog.servertransfer.client.TransferState;
import com.dog.servertransfer.command.TransferCommandRegistry;
import com.dog.servertransfer.config.TransferConfig;
import com.dog.servertransfer.network.ModpackHashPacket;
import com.dog.servertransfer.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
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

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                MinecraftForge.EVENT_BUS.register(ClientEventHandler.class)
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        TransferCommandRegistry.registerCommands(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            String hash = ModpackHashPacket.computeServerHash();
            NetworkHandler.sendToPlayer(serverPlayer, new ModpackHashPacket(hash, false));
        }
    }

    public static class ClientEventHandler {
        @SubscribeEvent
        public static void onLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
            if (!TransferState.isTransferring()) {
                TransferState.reset();
            }
        }
    }
}
