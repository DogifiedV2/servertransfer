package com.dog.servertransfer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class TransferHooks {
    public interface PreTransferHandler {
        void onPreTransfer(ServerPlayer player, String host, int port, String targetServer);
    }
    public interface PreparationHandler {
        CompletionStage<String> prepare(ServerPlayer player, String host, int port, String targetServer);
        void cancelled(ServerPlayer player);
        void menuOpened(ServerPlayer player);
    }
    private static final List<PreTransferHandler> LEGACY = new CopyOnWriteArrayList<>();
    private static PreparationHandler safety;
    private TransferHooks() {}
    public static void registerPreTransferHandler(PreTransferHandler handler) { LEGACY.add(handler); }
    public static void registerPreparationHandler(PreparationHandler handler) {
        if (safety != null) throw new IllegalStateException("A transfer preparation provider is already registered.");
        safety = handler;
    }
    public static CompletionStage<String> prepare(ServerPlayer player, String host, int port, String targetServer) {
        try {
            if (ModList.get().isLoaded("player_data_sync") && safety == null)
                throw new IllegalStateException("PlayerDataSync and ServerTransfer must both be updated before transferring.");
            for (PreTransferHandler handler : LEGACY) handler.onPreTransfer(player, host, port, targetServer);
            return safety == null ? CompletableFuture.completedFuture("standalone") : safety.prepare(player, host, port, targetServer);
        } catch (Exception e) { return CompletableFuture.failedFuture(e); }
    }
    public static void cancelled(ServerPlayer player) { if (safety != null) safety.cancelled(player); else TransferAPI.hideDataSyncOverlay(player); }
    public static void menuOpened(ServerPlayer player) { if (safety != null) safety.menuOpened(player); }
}
