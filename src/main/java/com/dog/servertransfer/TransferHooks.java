package com.dog.servertransfer;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TransferHooks {

    public interface PreTransferHandler {
        void onPreTransfer(ServerPlayer player, String host, int port, String targetServer);
    }

    private static final List<PreTransferHandler> PRE_TRANSFER_HANDLERS = new CopyOnWriteArrayList<>();

    private TransferHooks() {
    }

    public static void registerPreTransferHandler(PreTransferHandler handler) {
        PRE_TRANSFER_HANDLERS.add(handler);
    }

    public static void firePreTransfer(ServerPlayer player, String host, int port, String targetServer) {
        for (PreTransferHandler handler : PRE_TRANSFER_HANDLERS) {
            try {
                handler.onPreTransfer(player, host, port, targetServer);
            } catch (Throwable throwable) {
                ServerTransferMod.LOGGER.error(
                        "Pre-transfer handler {} failed for player {}",
                        handler.getClass().getName(),
                        player.getName().getString(),
                        throwable);
            }
        }
    }
}
