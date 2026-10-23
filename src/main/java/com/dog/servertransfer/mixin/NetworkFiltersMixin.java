package com.dog.servertransfer.mixin;

import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraftforge.network.filters.NetworkFilters;
import net.minecraftforge.network.filters.VanillaPacketFilter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Function;

/**
 * A Velocity server switch keeps the same connection, and ForgeClientResetPacket does not remove Forge's packet
 * filters from the previous server. Forge then adds them again on the next login, which throws "Duplicate handler
 * name" and skips sending the client's channel list to the new server.
 */
@Mixin(value = NetworkFilters.class, remap = false)
public abstract class NetworkFiltersMixin {
    @Shadow @Final private static Map<String, Function<Connection, VanillaPacketFilter>> instances;

    @Inject(method = "injectIfNecessary", at = @At("HEAD"), remap = false)
    private static void removeFiltersFromPreviousServer(Connection connection, CallbackInfo ci) {
        ChannelPipeline pipeline = connection.channel().pipeline();
        for (String filterName : instances.keySet()) {
            if (pipeline.get(filterName) != null) {
                pipeline.remove(filterName);
            }
        }
    }
}
