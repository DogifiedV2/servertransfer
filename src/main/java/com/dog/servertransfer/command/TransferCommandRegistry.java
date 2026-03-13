package com.dog.servertransfer.command;

import com.dog.servertransfer.ServerTransferMod;
import com.dog.servertransfer.config.TransferConfig;
import com.dog.servertransfer.network.NetworkHandler;
import com.dog.servertransfer.network.TransferPacket;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class TransferCommandRegistry {
    private static final ResourceLocation BUNGEECORD_CHANNEL = ResourceLocation.fromNamespaceAndPath("bungeecord", "main");
    private static final Map<String, ParsedAddress> PARSED_ADDRESSES = new HashMap<>();

    public static void registerCommands(MinecraftServer server) {
        PARSED_ADDRESSES.clear();
        Map<String, String> commands = TransferConfig.getCommandMap();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();

        for (Map.Entry<String, String> entry : commands.entrySet()) {
            String commandName = entry.getKey();
            String targetAddress = entry.getValue();

            ParsedAddress parsed = parseAddress(targetAddress);
            PARSED_ADDRESSES.put(commandName.toLowerCase(), parsed);

            registerTransferCommand(dispatcher, commandName, parsed);
            ServerTransferMod.LOGGER.info("Registered transfer command: /{} -> {}", commandName, targetAddress);
        }
    }

    private static void registerTransferCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String name,
            ParsedAddress parsed) {

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
        transferPlayer(player, host, port, targetServer);
        return 1;
    }

    public static boolean transferPlayer(ServerPlayer player, String serverName) {
        ParsedAddress parsed = PARSED_ADDRESSES.get(serverName.toLowerCase());
        if (parsed == null) {
            ServerTransferMod.LOGGER.warn("No transfer entry configured for server '{}'", serverName);
            return false;
        }
        transferPlayer(player, parsed.host, parsed.port, parsed.targetServer);
        return true;
    }

    private static void transferPlayer(ServerPlayer player, String host, int port, String targetServer) {
        if (host.equalsIgnoreCase("current")) {
            if (targetServer.isEmpty()) {
                ServerTransferMod.LOGGER.warn("Cannot use 'current' host without a target server name (e.g. echo=current|echo)");
                return;
            }
            sendVelocityTransfer(player, targetServer);
            ServerTransferMod.LOGGER.info("Velocity-transferring player {} to server '{}'", player.getName().getString(), targetServer);
        } else {
            NetworkHandler.sendToPlayer(player, new TransferPacket(host, port, targetServer));
            if (targetServer.isEmpty()) {
                ServerTransferMod.LOGGER.info("Transferring player {} to {}:{}", player.getName().getString(), host, port);
            } else {
                ServerTransferMod.LOGGER.info("Transferring player {} to {}:{} (target: {})", player.getName().getString(), host, port, targetServer);
            }
        }
    }

    private static void sendVelocityTransfer(ServerPlayer player, String targetServer) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(targetServer);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(out.toByteArray()));
        player.connection.send(new ClientboundCustomPayloadPacket(BUNGEECORD_CHANNEL, buf));
    }

    private record ParsedAddress(String host, int port, String targetServer) {}
}
