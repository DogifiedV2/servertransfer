package com.dog.servertransfer.command;

import com.dog.servertransfer.ServerTransferMod;
import com.dog.servertransfer.config.TransferConfig;
import com.dog.servertransfer.network.NetworkHandler;
import com.dog.servertransfer.network.TransferPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class TransferCommandRegistry {

    public static void registerCommands(MinecraftServer server) {
        Map<String, String> commands = TransferConfig.getCommandMap();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();

        for (Map.Entry<String, String> entry : commands.entrySet()) {
            String commandName = entry.getKey();
            String targetAddress = entry.getValue();

            registerTransferCommand(dispatcher, commandName, targetAddress);
            ServerTransferMod.LOGGER.info("Registered transfer command: /{} -> {}", commandName, targetAddress);
        }
    }

    private static void registerTransferCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String name,
            String address) {

        ParsedAddress parsed = parseAddress(address);

        dispatcher.register(
                Commands.literal(name)
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> executeTransfer(context.getSource(), parsed.host, parsed.port, parsed.targetServer))
        );
    }

    private static ParsedAddress parseAddress(String address) {
        String targetServer = "";
        int pipeIndex = address.indexOf('|');
        if (pipeIndex != -1) {
            targetServer = address.substring(pipeIndex + 1).trim();
            address = address.substring(0, pipeIndex);
        }

        String host;
        int port = 25565;

        int colonIndex = address.lastIndexOf(':');
        if (colonIndex != -1 && colonIndex < address.length() - 1) {
            host = address.substring(0, colonIndex);
            String portStr = address.substring(colonIndex + 1);
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                host = address;
            }
        } else {
            host = address;
        }

        return new ParsedAddress(host, port, targetServer);
    }

    private static int executeTransfer(CommandSourceStack source, String host, int port, String targetServer) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        NetworkHandler.sendToPlayer(player, new TransferPacket(host, port, targetServer));

        if (targetServer.isEmpty()) {
            ServerTransferMod.LOGGER.info("Transferring player {} to {}:{}", player.getName().getString(), host, port);
        } else {
            ServerTransferMod.LOGGER.info("Transferring player {} to {}:{} (target: {})", player.getName().getString(), host, port, targetServer);
        }
        return 1;
    }

    private record ParsedAddress(String host, int port, String targetServer) {}
}
