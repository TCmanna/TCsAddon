package com.tcmanna.tcsaddon.mixin.skiesstarredlibrary;

import com.tcmanna.tcsaddon.features.impl.render.DisablePranks;
import foo.starred.snowbird.internal.misc.DonatorSize;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(DonatorSize.class)
public class MixinDonatorSizeNew {
    @Inject(method = "fn", at = @At("HEAD"), cancellable = true)
    private static void cancelFn(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (DisablePranks.INSTANCE.getEnabled() && DisablePranks.INSTANCE.getStarredDonators()) cir.setReturnValue(false);
    }
}
