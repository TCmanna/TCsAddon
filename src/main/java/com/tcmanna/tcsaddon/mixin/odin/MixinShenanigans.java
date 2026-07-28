package com.tcmanna.tcsaddon.mixin.odin;

import com.odtheking.odin.events.RenderEvent;
import com.tcmanna.tcsaddon.features.impl.render.DisablePranks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("all")
@Mixin(targets = "com.odtheking.odin.features.impl.render.Shenanigans$1", remap = false)
public abstract class MixinShenanigans {

    @Inject(method = "invoke(Lcom/odtheking/odin/events/RenderEvent$Extract;)V", at = @At("HEAD"), cancellable = true)
    private void inject(RenderEvent.Extract event, CallbackInfo ci) {
        if (DisablePranks.INSTANCE.getEnabled() && DisablePranks.INSTANCE.getOdinDungeon()) {
            ci.cancel();
        }
    }
}
