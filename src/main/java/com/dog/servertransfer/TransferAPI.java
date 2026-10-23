package com.dog.servertransfer;

import com.dog.servertransfer.command.TransferCommandRegistry;
import com.dog.servertransfer.network.DataSyncOverlayPacket;
import com.dog.servertransfer.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;

public class TransferAPI {

    public static boolean transfer(ServerPlayer player, String serverName) {
        return TransferCommandRegistry.transferPlayer(player, serverName);
    }

    /** Completes when routing starts, not when destination loading finishes. */
    public static java.util.concurrent.CompletionStage<Boolean> transferAsync(ServerPlayer player, String serverName) {
        return TransferCommandRegistry.transferAsync(player, serverName);
    }
    public static boolean canTransfer(String serverName) { return TransferCommandRegistry.isKnownCommand(serverName); }
    public static void sendWaystoneMetadata(ServerPlayer player, net.minecraft.nbt.CompoundTag data) {
        net.minecraft.nbt.CompoundTag labelled = data.copy();
        for (String id : labelled.getAllKeys()) {
            net.minecraft.nbt.CompoundTag destination = labelled.getCompound(id);
            destination.putString("displayName", TransferCommandRegistry.displayName(destination.getString("server")));
        }
        NetworkHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.dog.servertransfer.network.WaystoneMetadataPacket(labelled));
    }

    public static void showDataSyncOverlay(ServerPlayer player, String message) {
        NetworkHandler.sendToPlayer(player, new DataSyncOverlayPacket(true, message != null ? message : ""));
    }

    public static void hideDataSyncOverlay(ServerPlayer player) {
        NetworkHandler.sendToPlayer(player, new DataSyncOverlayPacket(false, ""));
    }
}
