package com.tcmanna.tcsaddon.mixin.odinclient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@SuppressWarnings("all")
@Mixin(targets = "foo.starred.odinclient.features.ImportantFeature$fn$1", remap = false)
public class MixinImportantFeature {

    @Inject(method = "invoke", at = @At(value = "HEAD"), cancellable = true)
    private void removeFn(CallbackInfo ci) {
        ci.cancel();
    }
}
