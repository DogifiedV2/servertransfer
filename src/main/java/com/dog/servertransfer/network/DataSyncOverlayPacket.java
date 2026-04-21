package com.dog.servertransfer.network;

import com.dog.servertransfer.client.ClientDataSyncOverlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DataSyncOverlayPacket {
    private final boolean show;
    private final String message;

    public DataSyncOverlayPacket(boolean show, String message) {
        this.show = show;
        this.message = message != null ? message : "";
    }

    public static void encode(DataSyncOverlayPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.show);
        buf.writeUtf(msg.message, 255);
    }

    public static DataSyncOverlayPacket decode(FriendlyByteBuf buf) {
        boolean show = buf.readBoolean();
        String message = buf.readUtf(255);
        return new DataSyncOverlayPacket(show, message);
    }

    public static void handle(DataSyncOverlayPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientDataSyncOverlayHandler.handleOverlay(msg.show, msg.message)
        ));
        ctx.get().setPacketHandled(true);
    }

    public boolean isShow() {
        return show;
    }

    public String getMessage() {
        return message;
    }
}
