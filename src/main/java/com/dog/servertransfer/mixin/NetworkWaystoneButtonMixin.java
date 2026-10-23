package com.dog.servertransfer.mixin;

import com.dog.servertransfer.client.NetworkWaystones;
import com.dog.servertransfer.client.WaystoneLabels;
import net.blay09.mods.waystones.api.IWaystone;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="net.blay09.mods.waystones.client.gui.widget.WaystoneButton",remap=false)
public abstract class NetworkWaystoneButtonMixin {
    @Inject(method="<init>",at=@At("RETURN"),require=1)
    private void transfer$label(int x,int y,IWaystone stone,int xp,Button.OnPress onPress,CallbackInfo ci) {
        if(NetworkWaystones.remote(stone.getWaystoneUid())) {
            Button button=(Button)(Object)this;
            button.setMessage(WaystoneLabels.buttonName(stone, button.getWidth()));
            if(!NetworkWaystones.online(stone.getWaystoneUid())) button.active=false;
        }
    }
}
