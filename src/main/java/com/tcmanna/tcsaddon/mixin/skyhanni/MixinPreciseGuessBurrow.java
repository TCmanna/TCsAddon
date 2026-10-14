package com.tcmanna.tcsaddon.mixin.skyhanni;

import at.hannibal2.skyhanni.events.ItemClickEvent;
import at.hannibal2.skyhanni.features.event.diana.PreciseGuessBurrow;
import com.tcmanna.tcsaddon.features.impl.skyblock.FixSHDiana;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(PreciseGuessBurrow.class)
public class MixinPreciseGuessBurrow {
    @Redirect(method = "onItemClick", at = @At(value = "INVOKE", target = "Lat/hannibal2/skyhanni/events/ItemClickEvent;cancel()V"))
    private void cancel(ItemClickEvent instance) {
        if (!FixSHDiana.INSTANCE.getCancelRCBlock()) instance.cancel();
    }
}
