package com.dog.servertransfer.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMANDS;

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
                        "The |targetServer suffix is optional and used for Velocity proxy routing"
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
}
