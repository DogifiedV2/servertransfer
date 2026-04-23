package com.dog.servertransfer.client;

import com.dog.servertransfer.menu.MenuEntry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientOpenServerMenuHandler {

    public static void openMenu(List<MenuEntry> entries) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.execute(() -> minecraft.setScreen(new ServerMenuScreen(entries)));
    }
}
