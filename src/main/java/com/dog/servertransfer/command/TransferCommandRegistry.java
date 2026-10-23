package com.dog.servertransfer.command;

import com.dog.servertransfer.ServerTransferMod;
import com.dog.servertransfer.TransferHooks;
import com.dog.servertransfer.TransferAPI;
import net.minecraft.network.chat.TextComponent;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.dog.servertransfer.config.TransferConfig;
import com.dog.servertransfer.menu.MenuEntry;
import com.dog.servertransfer.network.ModpackHashPacket;
import com.dog.servertransfer.network.NetworkHandler;
import com.dog.servertransfer.network.OpenServerMenuPacket;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferCommandRegistry {
    private static final String SERVERS_MENU_COMMAND_NAME = "servers";
    private static final ResourceLocation BUNGEECORD_CHANNEL = ResourceLocation.fromNamespaceAndPath("bungeecord", "main");
    private static final Map<UUID, PendingTransfer> PENDING = new HashMap<>();
    private record PendingTransfer(ServerPlayer player, long deadline, java.util.concurrent.CompletableFuture<Boolean> result) {}
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

        registerServersMenuCommand(dispatcher);
        // Preserve /servers, and expose the menu under /server unless that name is configured for travel.
        if (!PARSED_ADDRESSES.containsKey("server")) dispatcher.register(Commands.literal("server")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> executeOpenServersMenu(context.getSource())));
    }

    private static void registerTransferCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String name,
            ParsedAddress parsed) {

        dispatcher.register(
                Commands.literal(name)
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> executeTransfer(context.getSource(), name, parsed))
        );
    }

    private static void registerServersMenuCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(SERVERS_MENU_COMMAND_NAME)
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> executeOpenServersMenu(context.getSource()))
        );
        ServerTransferMod.LOGGER.info("Registered /{} command", SERVERS_MENU_COMMAND_NAME);
    }

    private static int executeOpenServersMenu(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<MenuEntry> validatedEntries = buildValidatedMenuEntries();
        if (PENDING.containsKey(player.getUUID())) return 0;
        player.closeContainer(); // Return a carried cursor stack before taking the optional menu snapshot.
        NetworkHandler.sendToPlayer(player, new OpenServerMenuPacket(validatedEntries));
        TransferHooks.menuOpened(player);
        return 1;
    }

    private static List<MenuEntry> buildValidatedMenuEntries() {
        List<MenuEntry> parsedEntries = TransferConfig.getMenuEntries();
        List<MenuEntry> validatedEntries = new ArrayList<>(parsedEntries.size());

        for (MenuEntry entry : parsedEntries) {
            if (entry.disabled()) {
                validatedEntries.add(entry);
                continue;
            }

            if (!PARSED_ADDRESSES.containsKey(entry.commandName())) {
                ServerTransferMod.LOGGER.warn(
                        "Menu entry at position {} references unknown command '{}'; skipping",
                        entry.position(),
                        entry.commandName()
                );
                continue;
            }

            validatedEntries.add(entry);
        }

        return validatedEntries;
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

    private static int executeTransfer(CommandSourceStack source, String name, ParsedAddress parsed) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        transferPlayer(player, parsed.host, parsed.port, parsed.targetServer, displayName(name));
        return 1;
    }

    public static boolean isKnownCommand(String commandName) {
        if (commandName == null) {
            return false;
        }
        return PARSED_ADDRESSES.containsKey(commandName.toLowerCase());
    }

    public static boolean transferPlayer(ServerPlayer player, String serverName) {
        ParsedAddress parsed = PARSED_ADDRESSES.get(serverName.toLowerCase());
        if (parsed == null) {
            ServerTransferMod.LOGGER.warn("No transfer entry configured for server '{}'", serverName);
            return false;
        }
        transferPlayer(player, parsed.host, parsed.port, parsed.targetServer, displayName(serverName));
        return true;
    }

    public static java.util.concurrent.CompletionStage<Boolean> transferAsync(ServerPlayer player, String name) {
        if (!player.getServer().isSameThread()) return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Transfer must start on the server thread."));
        ParsedAddress parsed = PARSED_ADDRESSES.get(name.toLowerCase(java.util.Locale.ROOT));
        if (parsed == null) return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("No route for " + name));
        return transferPlayer(player, parsed.host, parsed.port, parsed.targetServer, displayName(name));
    }

    /** A display label is never used as the proxy handshake or connection address. */
    public static String displayName(String routeName) {
        for (MenuEntry entry : TransferConfig.getMenuEntries()) {
            if (!entry.disabled() && entry.commandName().equalsIgnoreCase(routeName)) return entry.displayName();
        }
        return routeName;
    }

    private static java.util.concurrent.CompletionStage<Boolean> transferPlayer(ServerPlayer player, String host, int port, String targetServer, String displayName) {
        if (PENDING.containsKey(player.getUUID())) return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("A transfer is already pending."));
        if (host.equalsIgnoreCase("current") && targetServer.isEmpty()) {
            player.sendMessage(new TextComponent("This server destination is not configured correctly."), player.getUUID());
            return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Destination is not configured."));
        }
        PendingTransfer request = new PendingTransfer(player, System.nanoTime() + TimeUnit.SECONDS.toNanos(45), new java.util.concurrent.CompletableFuture<>());
        PENDING.put(player.getUUID(), request);
        TransferAPI.showDataSyncOverlay(player, "Preparing your transfer");
        TransferHooks.prepare(player, host, port, targetServer).whenComplete((receipt, failure) -> player.getServer().execute(() -> {
            if (PENDING.get(player.getUUID()) != request || player.getServer().getPlayerList().getPlayer(player.getUUID()) != player) {
                TransferHooks.cancelled(player);
                request.result().completeExceptionally(new IllegalStateException("Transfer interrupted."));
                return;
            }
            if (failure != null || receipt == null || System.nanoTime() > request.deadline()) {
                PENDING.remove(player.getUUID(), request);
                TransferHooks.cancelled(player);
                Throwable cause = failure;
                while (cause != null && cause.getCause() != null) cause = cause.getCause();
                String message = cause instanceof IllegalStateException ? cause.getMessage() : "Your data could not be prepared safely.";
                request.result().completeExceptionally(new IllegalStateException(message));
                player.sendMessage(new TextComponent("Transfer cancelled. " + message), player.getUUID());
                return;
            }
            TransferAPI.showDataSyncOverlay(player, "Connecting to " + displayName);
            try { routePlayer(player, host, port, targetServer, displayName); request.result().complete(true); }
            catch (Exception e) { PENDING.remove(player.getUUID(), request); TransferHooks.cancelled(player); request.result().completeExceptionally(e); }
        }));
        return request.result();
    }

    public static void tick(MinecraftServer server) {
        for (PendingTransfer request : List.copyOf(PENDING.values())) {
            ServerPlayer player = request.player();
            if (server.getPlayerList().getPlayer(player.getUUID()) != player) {
                PENDING.remove(player.getUUID(), request);
                request.result().completeExceptionally(new IllegalStateException("Player disconnected."));
            } else if (System.nanoTime() > request.deadline()) {
                PENDING.remove(player.getUUID(), request);
                TransferHooks.cancelled(player);
                request.result().completeExceptionally(new IllegalStateException("Transfer timed out."));
                player.sendMessage(new TextComponent("The transfer timed out. Please wait for your data to finish saving."), player.getUUID());
            }
        }
    }

    private static void routePlayer(ServerPlayer player, String host, int port, String targetServer, String displayName) {
        String modpackHash = ModpackHashPacket.computeServerHash();
        NetworkHandler.sendToPlayer(player, new ModpackHashPacket(modpackHash, true));

        if (host.equalsIgnoreCase("current")) {
            if (targetServer.isEmpty()) {
                ServerTransferMod.LOGGER.warn("Cannot use 'current' host without a target server name (e.g. echo=current|echo)");
                return;
            }
            sendVelocityTransfer(player, targetServer);
            ServerTransferMod.LOGGER.info("Velocity-transferring player {} to server '{}'", player.getName().getString(), targetServer);
        } else {
            NetworkHandler.sendToPlayer(player, new TransferPacket(host, port, targetServer, displayName));
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
