package com.dog.servertransfer;

import com.dog.servertransfer.command.TransferCommandRegistry;
import net.minecraft.server.level.ServerPlayer;

public class TransferAPI {

    public static boolean transfer(ServerPlayer player, String serverName) {
        return TransferCommandRegistry.transferPlayer(player, serverName);
    }
}
