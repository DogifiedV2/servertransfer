package com.dog.servertransfer.client;

import net.blay09.mods.waystones.api.IWaystone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;

/** Loaded only by the optional Waystones mixins, not by the client event subscriber. */
public final class WaystoneLabels {
    private WaystoneLabels() {}

    public static MutableComponent fullName(IWaystone stone) {
        String name = stone.getName().isBlank() ? I18n.get("gui.waystones.waystone_selection.unnamed_waystone") : stone.getName();
        return new TextComponent(name + " (" + NetworkWaystones.label(stone.getWaystoneUid()) + ")");
    }

    /** Reserve space for the server suffix instead of trimming it off long names. */
    public static MutableComponent buttonName(IWaystone stone, int width) {
        var font = Minecraft.getInstance().font;
        int available = Math.max(32, width - 40); // Keep the XP icon clear.
        String name = stone.getName().isBlank() ? I18n.get("gui.waystones.waystone_selection.unnamed_waystone") : stone.getName();
        int serverWidth = Math.max(16, available - Math.min(font.width(name), 32) - font.width(" ()"));
        String serverName = fit(NetworkWaystones.displayName(stone.getWaystoneUid()), serverWidth);
        String suffix = " (" + serverName + ")";
        MutableComponent text = new TextComponent(fit(name, Math.max(0, available - font.width(suffix))));
        if (stone.isGlobal()) text.withStyle(ChatFormatting.AQUA);
        return text.append(new TextComponent(suffix).withStyle(NetworkWaystones.online(stone.getWaystoneUid()) ? ChatFormatting.GRAY : ChatFormatting.RED));
    }
    private static String fit(String text, int width) {
        var font = Minecraft.getInstance().font;
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

}
