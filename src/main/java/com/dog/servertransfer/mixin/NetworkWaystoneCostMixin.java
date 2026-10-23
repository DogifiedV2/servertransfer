package com.dog.servertransfer.mixin;

import com.dog.servertransfer.client.NetworkWaystones;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.blay09.mods.waystones.core.WarpMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="net.blay09.mods.waystones.core.PlayerWaystoneManager",remap=false)
public abstract class NetworkWaystoneCostMixin {
    @Inject(method="predictExperienceLevelCost",at=@At("HEAD"),cancellable=true)
    private static void transfer$price(Entity entity,IWaystone stone,WarpMode mode,IWaystone from,CallbackInfoReturnable<Integer> ci) {
        if(!NetworkWaystones.remote(stone.getWaystoneUid())) return;
        var config=WaystonesConfig.getActive();
        int price=entity instanceof Player player && player.getAbilities().instabuild ? 0 : Math.max(0,(int)Math.round(config.dimensionalWarpXpCost()*mode.getXpCostMultiplier()*(stone.isGlobal()?config.globalWaystoneXpCostMultiplier():1)));
        ci.setReturnValue(price);
    }
}
