package com.dog.servertransfer.config;

import com.dog.servertransfer.ServerTransferMod;
import com.dog.servertransfer.menu.MenuEntry;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferConfig {
    public static final String DISABLED_SLOT_KEYWORD = "disabled";
    public static final int MINIMUM_MENU_POSITION = 1;
    public static final int MAXIMUM_MENU_POSITION = 10;

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMANDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MENU_ENTRIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Transfer Command Configuration");
        builder.push("commands");

        COMMANDS = builder
                .comment(
                        "List of transfer commands in format: \"commandname=host:port\" or \"commandname=host:port|targetServer\"",
                        "Examples:",
                        "  \"lobby=hub.example.com:25565\" - Direct connection to server",
                        "  \"survival=proxy.example.com:25565|survival\" - Connect to proxy, route to 'survival' backend",
                        "  \"echo=current|echo\" - Velocity-internal transfer (no reconnect, requires bungee-plugin-message-channel in velocity.toml)",
                        "The |targetServer suffix is optional and used for Velocity proxy routing.",
                        "Use 'current' as host for same-proxy Velocity transfers (sends BungeeCord Connect message instead of reconnecting)."
                )
                .defineList(
                        "entries",
                        List.of(
                                "lobby=lobby.example.com:25565",
                                "hub=hub.example.com:25565"
                        ),
                        entry -> entry instanceof String && ((String) entry).contains("=")
                );

        builder.pop();

        builder.comment("Server selection menu opened by /servers");
        builder.push("menu");

        MENU_ENTRIES = builder
                .comment(
                        "Menu entries shown by the /servers command in a 2-row by 5-column grid.",
                        "Format per entry: \"position=commandName|displayName\"",
                        "  position: integer " + MINIMUM_MENU_POSITION + "-" + MAXIMUM_MENU_POSITION
                                + " (1-5 = top row left-to-right, 6-10 = bottom row left-to-right). Gaps allowed.",
                        "  commandName: either the literal string '" + DISABLED_SLOT_KEYWORD
                                + "' (greyed slot representing the current server) or a name matching an entry in commands.entries.",
                        "  displayName: free text label shown on the button.",
                        "Entries with position outside " + MINIMUM_MENU_POSITION + "-" + MAXIMUM_MENU_POSITION
                                + " are ignored with a log warning (reserved for future scrolling).",
                        "Example:",
                        "  \"1=lobby|Main Lobby\"",
                        "  \"2=" + DISABLED_SLOT_KEYWORD + "|Survival\"",
                        "  \"3=creative|Creative\""
                )
                .defineList(
                        "entries",
                        List.of(),
                        entry -> entry instanceof String && ((String) entry).contains("=")
                );

        builder.pop();
        SPEC = builder.build();
    }

    public static Map<String, String> getCommandMap() {
        Map<String, String> result = new HashMap<>();
        for (String entry : COMMANDS.get()) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                String commandName = parts[0].trim().toLowerCase();
                String address = parts[1].trim();
                result.put(commandName, address);
            }
        }
        return result;
    }

    public static List<MenuEntry> getMenuEntries() {
        List<MenuEntry> result = new ArrayList<>();
        for (String rawEntry : MENU_ENTRIES.get()) {
            MenuEntry parsed = parseMenuEntry(rawEntry);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return result;
    }

    private static MenuEntry parseMenuEntry(String rawEntry) {
        String[] positionSplit = rawEntry.split("=", 2);
        if (positionSplit.length != 2) {
            ServerTransferMod.LOGGER.warn("Ignoring malformed menu entry (missing '='): {}", rawEntry);
            return null;
        }

        int position;
        try {
            position = Integer.parseInt(positionSplit[0].trim());
        } catch (NumberFormatException exception) {
            ServerTransferMod.LOGGER.warn("Ignoring menu entry with non-integer position: {}", rawEntry);
            return null;
        }

        if (position < MINIMUM_MENU_POSITION || position > MAXIMUM_MENU_POSITION) {
            ServerTransferMod.LOGGER.warn(
                    "Ignoring menu entry with out-of-range position {} (expected {}-{}): {}",
                    position, MINIMUM_MENU_POSITION, MAXIMUM_MENU_POSITION, rawEntry
            );
            return null;
        }

        String remainder = positionSplit[1];
        int pipeIndex = remainder.indexOf('|');
        if (pipeIndex == -1) {
            ServerTransferMod.LOGGER.warn("Ignoring menu entry (missing '|' separator between command and display name): {}", rawEntry);
            return null;
        }

        String commandName = remainder.substring(0, pipeIndex).trim().toLowerCase();
        String displayName = remainder.substring(pipeIndex + 1).trim();

        if (commandName.isEmpty()) {
            ServerTransferMod.LOGGER.warn("Ignoring menu entry with empty command name: {}", rawEntry);
            return null;
        }

        if (displayName.isEmpty()) {
            ServerTransferMod.LOGGER.warn("Ignoring menu entry with empty display name: {}", rawEntry);
            return null;
        }

        boolean disabled = commandName.equals(DISABLED_SLOT_KEYWORD);
        String effectiveCommandName = disabled ? "" : commandName;

        return new MenuEntry(position, effectiveCommandName, displayName, disabled);
    }
}
