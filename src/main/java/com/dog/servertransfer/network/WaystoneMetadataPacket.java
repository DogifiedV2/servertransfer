package com.dog.servertransfer.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record WaystoneMetadataPacket(CompoundTag data) {
    public static void encode(WaystoneMetadataPacket message, FriendlyByteBuf buffer) { buffer.writeNbt(message.data()); }
    public static WaystoneMetadataPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag=buffer.readNbt();
        if(tag==null || tag.getAllKeys().size()>10000) throw new IllegalArgumentException("Invalid waystone metadata.");
        return new WaystoneMetadataPacket(tag);
    }
    public static void handle(WaystoneMetadataPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.dog.servertransfer.client.NetworkWaystones.accept(message.data())));
        context.get().setPacketHandled(true);
    }
}
