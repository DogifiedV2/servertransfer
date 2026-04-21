package com.dog.servertransfer.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientDataSyncOverlayHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handleOverlay(boolean show, String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        minecraft.execute(() -> {
            if (show) {
                if (!(minecraft.screen instanceof WaitingForDataScreen)) {
                    LOGGER.info("Opening data-sync overlay: {}", message);
                    minecraft.setScreen(new WaitingForDataScreen(message));
                }
            } else {
                if (minecraft.screen instanceof WaitingForDataScreen) {
                    LOGGER.info("Closing data-sync overlay");
                    minecraft.setScreen(null);
                }
            }
        });
    }
}
