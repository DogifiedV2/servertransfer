package com.dog.servertransfer.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="servertransfer",value=Dist.CLIENT)
public final class NetworkWaystones {
    private static CompoundTag metadata = new CompoundTag();
    private static long revision;
    private static boolean showRemote = true;

    public static void accept(CompoundTag tag) { metadata = tag.copy(); revision++; }
    public static long revision() { return revision; }
    public static boolean showRemote() { return showRemote; }
    public static void toggleRemote() { showRemote = !showRemote; }
    public static boolean remote(UUID id) { return metadata.contains(id.toString()); }
    public static String server(UUID id) { return metadata.getCompound(id.toString()).getString("server"); }
    public static String displayName(UUID id) {
        String name = metadata.getCompound(id.toString()).getString("displayName");
        return name.isBlank() ? server(id) : name;
    }
    public static boolean online(UUID id) { return metadata.getCompound(id.toString()).getBoolean("online"); }
    public static String label(UUID id) { return displayName(id)+(metadata.getCompound(id.toString()).getBoolean("removed")?" — removed":online(id)?"":" — unavailable"); }

    @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        CompoundTag tag = event.getItemStack().getTag();
        if (tag != null && tag.hasUUID("AttunedToWaystone") && remote(tag.getUUID("AttunedToWaystone")))
            event.getToolTip().add(new TextComponent(label(tag.getUUID("AttunedToWaystone"))));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        metadata = new CompoundTag(); revision++; showRemote = true;
    }
}
