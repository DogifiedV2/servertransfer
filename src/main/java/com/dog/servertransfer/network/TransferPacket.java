package com.dog.servertransfer.network;

import com.dog.servertransfer.client.ClientTransferHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TransferPacket {
    private final String host;
    private final int port;
    private final String targetServer;

    public TransferPacket(String host, int port) {
        this(host, port, "");
    }

    public TransferPacket(String host, int port, String targetServer) {
        this.host = host;
        this.port = port;
        this.targetServer = targetServer != null ? targetServer : "";
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.host, 255);
        buf.writeInt(this.port);
        buf.writeUtf(this.targetServer, 255);
    }

    public static TransferPacket decode(FriendlyByteBuf buf) {
        String host = buf.readUtf(255);
        int port = buf.readInt();
        String targetServer = buf.readUtf(255);
        return new TransferPacket(host, port, targetServer);
    }

    public static void handle(TransferPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientTransferHandler.handleTransfer(packet.host, packet.port, packet.targetServer)
            );
        });
        ctx.get().setPacketHandled(true);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getTargetServer() {
        return targetServer;
    }
}
