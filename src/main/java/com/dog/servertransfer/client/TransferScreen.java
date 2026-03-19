package com.dog.servertransfer.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.dog.servertransfer.client.TransferState;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class TransferScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String host;
    private final int port;
    private final String targetServer;
    private volatile Connection connection;
    private volatile boolean aborted = false;
    private volatile Component status = new TranslatableComponent("connect.connecting");

    public TransferScreen(String host, int port, String targetServer) {
        super(new TextComponent("Transferring..."));
        this.host = host;
        this.port = port;
        this.targetServer = targetServer != null ? targetServer : "";
    }

    public void connect(Minecraft minecraft) {
        Thread connectionThread = new Thread(() -> connectToServer(minecraft), "Server Transfer");
        connectionThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        connectionThread.start();
    }

    private void connectToServer(Minecraft minecraft) {
        try {
            status = new TranslatableComponent("connect.connecting");

            ServerAddress serverAddress = new ServerAddress(host, port);
            Optional<ResolvedServerAddress> resolved = ServerNameResolver.DEFAULT.resolveAddress(serverAddress);

            if (aborted) {
                return;
            }

            if (resolved.isEmpty()) {
                TransferState.setTransferring(false);
                minecraft.execute(() -> minecraft.setScreen(new DisconnectedScreen(
                        new JoinMultiplayerScreen(new TitleScreen()),
                        CommonComponents.CONNECT_FAILED,
                        new TranslatableComponent("disconnect.genericReason", "Unknown host")
                )));
                return;
            }

            if (aborted) {
                return;
            }

            InetSocketAddress inetAddress = resolved.get().asInetSocketAddress();
            connection = Connection.connectToServer(inetAddress, minecraft.options.useNativeTransport);

            connection.setListener(new ClientHandshakePacketListenerImpl(
                    connection,
                    minecraft,
                    new JoinMultiplayerScreen(new TitleScreen()),
                    this::updateStatus
            ));

            String handshakeHost = serverAddress.getHost();
            if (!targetServer.isEmpty()) {
                handshakeHost = handshakeHost + "///" + targetServer;
            }

            connection.send(new ClientIntentionPacket(
                    handshakeHost,
                    serverAddress.getPort(),
                    ConnectionProtocol.LOGIN
            ));

            GameProfile gameProfile = minecraft.getUser().getGameProfile();
            connection.send(new ServerboundHelloPacket(gameProfile));

        } catch (Exception e) {
            if (aborted) {
                return;
            }

            TransferState.setTransferring(false);
            minecraft.execute(() -> minecraft.setScreen(new DisconnectedScreen(
                    new JoinMultiplayerScreen(new TitleScreen()),
                    CommonComponents.CONNECT_FAILED,
                    new TranslatableComponent("disconnect.genericReason", e.getMessage())
            )));
        }
    }

    private void updateStatus(Component newStatus) {
        this.status = newStatus;
    }

    @Override
    public void tick() {
        if (connection != null) {
            if (connection.isConnected()) {
                connection.tick();
            } else {
                connection.handleDisconnection();
            }
        }
    }

    private Component getTransferMessage() {
        String displayName = !targetServer.isEmpty() ? targetServer : host;
        return new TextComponent("You are being moved to " + displayName);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, getTransferMessage(), this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, this.status, this.width / 2, this.height / 2 - 30, 0xAAAAAA);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        this.aborted = true;
        TransferState.setTransferring(false);
        if (this.connection != null) {
            this.connection.disconnect(new TranslatableComponent("multiplayer.status.cancelled"));
        }
    }
}
