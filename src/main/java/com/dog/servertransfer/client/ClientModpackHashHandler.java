package com.dog.servertransfer.client;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientModpackHashHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void handleHash(String hash, boolean preTransfer) {
        if (preTransfer) {
            TransferState.setModpackHash(hash);
            TransferState.setTransferring(true);
            LOGGER.info("Received pre-transfer modpack hash, JEI lifecycle suppression armed");
        } else {
            if (TransferState.isTransferring()) {
                String previousHash = TransferState.getModpackHash();
                if (!previousHash.isEmpty() && !previousHash.equals(hash)) {
                    LOGGER.warn("Modpack hash mismatch after transfer! Previous: {}, New: {}. JEI state may be stale.", previousHash, hash);
                } else {
                    LOGGER.info("Modpack hash verified after transfer, JEI state preserved successfully");
                }
                TransferState.setTransferring(false);
            }
            TransferState.setModpackHash(hash);
        }
    }
}
