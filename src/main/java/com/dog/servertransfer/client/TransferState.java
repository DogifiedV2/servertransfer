package com.dog.servertransfer.client;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TransferState {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long TRANSFER_TIMEOUT_MS = 30_000;

    private static volatile boolean transferring = false;
    private static volatile String previousModpackHash = "";
    private static volatile long transferStartTime = 0;

    public static void setTransferring(boolean value) {
        transferring = value;
        if (value) {
            transferStartTime = System.currentTimeMillis();
            LOGGER.info("Transfer state enabled, JEI lifecycle suppression active");
        } else {
            LOGGER.info("Transfer state disabled");
        }
    }

    public static boolean isTransferring() {
        if (transferring && System.currentTimeMillis() - transferStartTime > TRANSFER_TIMEOUT_MS) {
            LOGGER.warn("Transfer state timed out after {}ms, resetting to prevent stuck state", TRANSFER_TIMEOUT_MS);
            transferring = false;
        }
        return transferring;
    }

    public static void setModpackHash(String hash) {
        previousModpackHash = hash;
    }

    public static String getModpackHash() {
        return previousModpackHash;
    }

    public static boolean hashMatches(String otherHash) {
        return !previousModpackHash.isEmpty() && previousModpackHash.equals(otherHash);
    }

    public static void reset() {
        transferring = false;
        previousModpackHash = "";
        transferStartTime = 0;
    }
}
