package com.dog.servertransfer.mixin;

import com.dog.servertransfer.client.NetworkWaystones;
import com.dog.servertransfer.client.WaystoneLabels;
import net.blay09.mods.waystones.api.IWaystone;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.TextComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;

@Pseudo
@Mixin(targets="fr.loxoz.mods.betterwaystonesmenu.gui.widget.BetterWaystoneButton",remap=false)
public abstract class NetworkWaystoneTooltipMixin {
    @Shadow @Final private IWaystone waystone;
    @Shadow @Final @Mutable private Component fullMessage;

    // BetterWaystonesMenu replaces the base button label in its own constructor.
    @Inject(method="<init>",at=@At("RETURN"),require=1)
    private void transfer$label(int x, int y, IWaystone stone, int xp, Button.OnPress press,
            Vec3 origin, ResourceKey<Level> dimension, CallbackInfo ci) {
        if (NetworkWaystones.remote(stone.getWaystoneUid())) {
            Button button = (Button)(Object)this;
            button.setMessage(WaystoneLabels.buttonName(stone, button.getWidth()));
            fullMessage = WaystoneLabels.fullName(stone);
        }
    }

    @Inject(method="computeTooltip",at=@At("HEAD"),cancellable=true,require=1)
    private void transfer$remote(CallbackInfoReturnable<List<Component>> ci) {
        if(NetworkWaystones.remote(waystone.getWaystoneUid())) ci.setReturnValue(List.of(new TextComponent(waystone.getName()),new TextComponent(NetworkWaystones.label(waystone.getWaystoneUid()))));
    }
}
