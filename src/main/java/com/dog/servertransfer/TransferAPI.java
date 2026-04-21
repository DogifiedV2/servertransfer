package com.dog.servertransfer;

import com.dog.servertransfer.command.TransferCommandRegistry;
import com.dog.servertransfer.network.DataSyncOverlayPacket;
import com.dog.servertransfer.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;

public class TransferAPI {

    public static boolean transfer(ServerPlayer player, String serverName) {
        return TransferCommandRegistry.transferPlayer(player, serverName);
    }

    public static void showDataSyncOverlay(ServerPlayer player, String message) {
        NetworkHandler.sendToPlayer(player, new DataSyncOverlayPacket(true, message != null ? message : ""));
    }

    public static void hideDataSyncOverlay(ServerPlayer player) {
        NetworkHandler.sendToPlayer(player, new DataSyncOverlayPacket(false, ""));
    }
}
