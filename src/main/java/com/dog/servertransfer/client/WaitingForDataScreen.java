package com.dog.servertransfer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaitingForDataScreen extends Screen {
    private static final long SAFETY_TIMEOUT_MS = 30_000L;
    private static final String ANIMATION_DOTS = "...";

    private final String message;
    private final long openedAtMillis;

    public WaitingForDataScreen(String message) {
        super(new TextComponent("Waiting for data"));
        this.message = message == null || message.isEmpty() ? "Waiting on your data" : message;
        this.openedAtMillis = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        if (System.currentTimeMillis() - this.openedAtMillis > SAFETY_TIMEOUT_MS) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.screen == this) {
                minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        long elapsedMillis = System.currentTimeMillis() - this.openedAtMillis;
        int dotCount = (int) ((elapsedMillis / 400L) % 4L);
        String animatedDots = ANIMATION_DOTS.substring(0, dotCount);
        Component line = new TextComponent(this.message + animatedDots);

        drawCenteredString(poseStack, this.font, line, this.width / 2, this.height / 2 - 10, 0xFFFFFF);
        drawCenteredString(poseStack,
                this.font,
                new TextComponent("Please wait while your data is synced from the other server"),
                this.width / 2,
                this.height / 2 + 10,
                0xAAAAAA);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
