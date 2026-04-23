package com.dog.servertransfer.network;

import com.dog.servertransfer.ServerTransferMod;
import com.dog.servertransfer.command.TransferCommandRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestTransferPacket {
    public static final int MAX_COMMAND_NAME_LENGTH = 255;

    private final String commandName;

    public RequestTransferPacket(String commandName) {
        this.commandName = commandName != null ? commandName : "";
    }

    public static void encode(RequestTransferPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.commandName, MAX_COMMAND_NAME_LENGTH);
    }

    public static RequestTransferPacket decode(FriendlyByteBuf buffer) {
        return new RequestTransferPacket(buffer.readUtf(MAX_COMMAND_NAME_LENGTH));
    }

    public static void handle(RequestTransferPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context networkContext = context.get();
        networkContext.enqueueWork(() -> handleOnServerThread(packet, networkContext));
        networkContext.setPacketHandled(true);
    }

    private static void handleOnServerThread(RequestTransferPacket packet, NetworkEvent.Context networkContext) {
        ServerPlayer sender = networkContext.getSender();
        if (sender == null) {
            ServerTransferMod.LOGGER.warn("Received RequestTransferPacket without a sender, ignoring");
            return;
        }

        String requestedCommand = packet.commandName;
        if (requestedCommand.isEmpty()) {
            ServerTransferMod.LOGGER.warn(
                    "Player {} sent RequestTransferPacket with an empty command name, rejecting",
                    sender.getName().getString()
            );
            return;
        }

        if (!TransferCommandRegistry.isKnownCommand(requestedCommand)) {
            ServerTransferMod.LOGGER.warn(
                    "Player {} requested transfer to unknown command '{}', rejecting",
                    sender.getName().getString(),
                    requestedCommand
            );
            return;
        }

        TransferCommandRegistry.transferPlayer(sender, requestedCommand);
    }
}
