package com.dog.servertransfer.mixin;

import com.dog.servertransfer.client.NetworkWaystones;
import com.dog.servertransfer.client.WaystoneLabels;
import fr.loxoz.mods.betterwaystonesmenu.gui.widget.BetterWaystoneButton;
import fr.loxoz.mods.betterwaystonesmenu.gui.widget.ScrollableContainerWidget;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.menu.WaystoneSelectionMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;

/** Filter only the presentation list. The saved discovery/sort list is never changed. */
@Pseudo
@Mixin(targets="fr.loxoz.mods.betterwaystonesmenu.gui.screen.BetterWaystoneSelectionScreenBase",remap=false)
public abstract class NetworkWaystoneMenuMixin extends AbstractContainerScreen<WaystoneSelectionMenu> {
    @Shadow @Final private List<IWaystone> visibleWaystones;
    @Shadow protected ScrollableContainerWidget scrollable;
    @Shadow protected abstract void updateFilters();
    @Shadow public abstract void updateList();
    @Unique private long transfer$metadataRevision;

    protected NetworkWaystoneMenuMixin(WaystoneSelectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // Exact Mojang/SRG override names from the installed BetterWaystonesMenu 1.2.0 jar.
    @Inject(method={"init","m_7856_"},at=@At("RETURN"),require=1)
    private void transfer$toggleButton(CallbackInfo ci) {
        int y = scrollable.getY();
        if (scrollable.getHeight() < 46 && y + scrollable.getHeight() + 40 <= height - 8) {
            // Keep at least one complete row at small GUI sizes and low menu-height settings.
            y += scrollable.getHeight() + 20;
        } else {
            scrollable.setY(y + 24);
            scrollable.setHeight(Math.max(0, scrollable.getHeight() - 24));
        }
        addRenderableWidget(new Button(scrollable.getX(), y, scrollable.getWidth(), 20,
                transfer$toggleLabel(), button -> {
                    NetworkWaystones.toggleRemote();
                    button.setMessage(transfer$toggleLabel());
                    updateFilters();
                    updateList();
                    scrollable.setScrollY(0);
                }));
        transfer$metadataRevision = NetworkWaystones.revision();
    }

    @Unique private Component transfer$toggleLabel() {
        return new TextComponent(NetworkWaystones.showRemote() ? "Hide cross-server" : "Show cross-server");
    }

    @Inject(method="updateFilters",at=@At("RETURN"),require=1)
    private void transfer$filter(CallbackInfo ci) {
        if (!NetworkWaystones.showRemote()) visibleWaystones.removeIf(stone -> NetworkWaystones.remote(stone.getWaystoneUid()));
    }

    @Inject(method="createWaystoneButton",at=@At("RETURN"),require=1)
    private void transfer$fitLabel(int y, IWaystone stone, CallbackInfoReturnable<BetterWaystoneButton> ci) {
        // The menu narrows the button after its constructor. Fit against its final width.
        if (NetworkWaystones.remote(stone.getWaystoneUid())) {
            BetterWaystoneButton button = ci.getReturnValue();
            button.setMessage(WaystoneLabels.buttonName(stone, button.getWidth()));
        }
    }

    @Inject(method={"containerTick","m_181908_"},at=@At("RETURN"),require=1)
    private void transfer$refreshMetadata(CallbackInfo ci) {
        if (transfer$metadataRevision != NetworkWaystones.revision()) {
            transfer$metadataRevision = NetworkWaystones.revision();
            updateFilters();
            updateList();
        }
    }
}
