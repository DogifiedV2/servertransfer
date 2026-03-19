package com.dog.servertransfer.mixin;

import com.dog.servertransfer.client.TransferState;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.forge.startup.StartEventObserver", remap = false)
public class JeiStartEventObserverMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "transitionState", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressStateTransitionDuringTransfer(CallbackInfo ci) {
        if (TransferState.isTransferring()) {
            LOGGER.info("Suppressing JEI state transition during server transfer");
            ci.cancel();
        }
    }

    @Inject(method = "restart", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressRestartDuringTransfer(CallbackInfo ci) {
        if (TransferState.isTransferring()) {
            LOGGER.info("Suppressing JEI restart during server transfer");
            ci.cancel();
        }
    }

    @Inject(method = "onEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressEventDuringTransfer(CallbackInfo ci) {
        if (TransferState.isTransferring()) {
            LOGGER.info("Suppressing JEI event processing during server transfer");
            ci.cancel();
        }
    }
}
