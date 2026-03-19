package com.dog.servertransfer.network;

import com.dog.servertransfer.client.ClientModpackHashHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.function.Supplier;

public class ModpackHashPacket {
    private final String hash;
    private final boolean preTransfer;

    public ModpackHashPacket(String hash, boolean preTransfer) {
        this.hash = hash;
        this.preTransfer = preTransfer;
    }

    public static void encode(ModpackHashPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hash, 255);
        buf.writeBoolean(msg.preTransfer);
    }

    public static ModpackHashPacket decode(FriendlyByteBuf buf) {
        return new ModpackHashPacket(buf.readUtf(255), buf.readBoolean());
    }

    public static void handle(ModpackHashPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientModpackHashHandler.handleHash(msg.hash, msg.preTransfer)
            );
        });
        ctx.get().setPacketHandled(true);
    }

    public static String computeServerHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            ModList.get().getMods().stream()
                    .sorted(Comparator.comparing(mod -> mod.getModId()))
                    .forEach(mod -> {
                        digest.update(mod.getModId().getBytes(StandardCharsets.UTF_8));
                        digest.update(mod.getVersion().toString().getBytes(StandardCharsets.UTF_8));
                    });

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
