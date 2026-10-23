package com.dog.servertransfer.client;

import com.dog.servertransfer.menu.MenuEntry;
import com.dog.servertransfer.network.NetworkHandler;
import com.dog.servertransfer.network.RequestTransferPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ServerMenuScreen extends Screen {
    private static final int GRID_COLUMNS = 2;
    private static final int GRID_ROWS = 5;
    private static final int GRID_SLOTS = GRID_COLUMNS * GRID_ROWS;
    private static final int BUTTON_WIDTH = 130;
    private static final int BUTTON_HEIGHT = 32;
    private static final int HORIZONTAL_GAP = 8;
    private static final int VERTICAL_GAP = 4;
    private static final int TITLE_OFFSET_ABOVE_GRID = 20;

    private static final Component SCREEN_TITLE = new TextComponent("Server Selection");
    private static final Component CURRENT_SERVER_TOOLTIP = new TextComponent("You are on this server");

    private final List<MenuEntry> entries;
    /** Buttons of the server the player is on; they are inactive, so the screen draws their tooltip. */
    private final List<Button> currentServerButtons = new ArrayList<>();

    public ServerMenuScreen(List<MenuEntry> entries) {
        super(SCREEN_TITLE);
        this.entries = entries;
    }

    @Override
    protected void init() {
        super.init();
        this.currentServerButtons.clear();

        MenuEntry[] slots = new MenuEntry[GRID_SLOTS];
        for (MenuEntry entry : entries) {
            int slotIndex = entry.position() - 1;
            if (slotIndex < 0 || slotIndex >= GRID_SLOTS) {
                continue;
            }
            slots[slotIndex] = entry;
        }

        int gridWidth = GRID_COLUMNS * BUTTON_WIDTH + (GRID_COLUMNS - 1) * HORIZONTAL_GAP;
        int gridHeight = GRID_ROWS * BUTTON_HEIGHT + (GRID_ROWS - 1) * VERTICAL_GAP;
        int gridStartX = (this.width - gridWidth) / 2;
        int gridStartY = (this.height - gridHeight) / 2;

        for (int slotIndex = 0; slotIndex < GRID_SLOTS; slotIndex++) {
            MenuEntry entry = slots[slotIndex];
            if (entry == null) {
                continue;
            }

            int column = slotIndex / GRID_ROWS;
            int row = slotIndex % GRID_ROWS;
            int buttonX = gridStartX + column * (BUTTON_WIDTH + HORIZONTAL_GAP);
            int buttonY = gridStartY + row * (BUTTON_HEIGHT + VERTICAL_GAP);

            addRenderableWidget(createSlotButton(entry, buttonX, buttonY));
        }
    }

    /** Vanilla buttons only draw 20 pixels of texture; ExtendedButton stretches it to the full height. */
    private Button createSlotButton(MenuEntry entry, int x, int y) {
        Component label = new TextComponent(entry.displayName());
        Button button = new ExtendedButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label, pressed -> onSlotClicked(entry));
        if (entry.disabled()) {
            button.active = false;
            this.currentServerButtons.add(button);
        }
        return button;
    }

    private void onSlotClicked(MenuEntry entry) {
        if (entry.disabled()) {
            return;
        }
        NetworkHandler.sendToServer(new RequestTransferPacket(entry.commandName()));
        onClose();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);

        int gridHeight = GRID_ROWS * BUTTON_HEIGHT + (GRID_ROWS - 1) * VERTICAL_GAP;
        int titleY = ((this.height - gridHeight) / 2) - TITLE_OFFSET_ABOVE_GRID;
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTick);

        for (Button button : this.currentServerButtons) {
            if (mouseX >= button.x && mouseY >= button.y && mouseX < button.x + button.getWidth() && mouseY < button.y + button.getHeight()) {
                renderTooltip(poseStack, CURRENT_SERVER_TOOLTIP, mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
