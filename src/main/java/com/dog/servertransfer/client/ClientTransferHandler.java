package com.dog.servertransfer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientTransferHandler {

    public static void handleTransfer(String host, int port, String targetServer) {
        Minecraft minecraft = Minecraft.getInstance();

        minecraft.execute(() -> {
            ClientPacketListener oldListener = minecraft.getConnection();
            if (oldListener != null) {
                Connection oldConnection = oldListener.getConnection();
                if (oldConnection.isConnected()) {
                    oldConnection.disconnect(new TextComponent("Transferring to another server"));
                }
            }

            TransferScreen screen = new TransferScreen(host, port, targetServer);
            minecraft.clearLevel(screen);
            minecraft.prepareForMultiplayer();
            screen.connect(minecraft);
        });
    }
}
