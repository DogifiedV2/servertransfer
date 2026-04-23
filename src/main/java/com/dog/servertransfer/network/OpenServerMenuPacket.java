package com.dog.servertransfer.network;

import com.dog.servertransfer.client.ClientOpenServerMenuHandler;
import com.dog.servertransfer.menu.MenuEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenServerMenuPacket {
    private final List<MenuEntry> entries;

    public OpenServerMenuPacket(List<MenuEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static void encode(OpenServerMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entries.size());
        for (MenuEntry entry : packet.entries) {
            entry.encode(buffer);
        }
    }

    public static OpenServerMenuPacket decode(FriendlyByteBuf buffer) {
        int entryCount = buffer.readVarInt();
        List<MenuEntry> entries = new ArrayList<>(entryCount);
        for (int index = 0; index < entryCount; index++) {
            entries.add(MenuEntry.decode(buffer));
        }
        return new OpenServerMenuPacket(entries);
    }

    public static void handle(OpenServerMenuPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientOpenServerMenuHandler.openMenu(packet.entries)
                )
        );
        context.get().setPacketHandled(true);
    }
}
