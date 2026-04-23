package com.dog.servertransfer.menu;

import net.minecraft.network.FriendlyByteBuf;

public record MenuEntry(int position, String commandName, String displayName, boolean disabled) {

    public static final int MAX_DISPLAY_NAME_LENGTH = 255;
    public static final int MAX_COMMAND_NAME_LENGTH = 255;

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(position);
        buffer.writeUtf(commandName, MAX_COMMAND_NAME_LENGTH);
        buffer.writeUtf(displayName, MAX_DISPLAY_NAME_LENGTH);
        buffer.writeBoolean(disabled);
    }

    public static MenuEntry decode(FriendlyByteBuf buffer) {
        int position = buffer.readVarInt();
        String commandName = buffer.readUtf(MAX_COMMAND_NAME_LENGTH);
        String displayName = buffer.readUtf(MAX_DISPLAY_NAME_LENGTH);
        boolean disabled = buffer.readBoolean();
        return new MenuEntry(position, commandName, displayName, disabled);
    }
}
