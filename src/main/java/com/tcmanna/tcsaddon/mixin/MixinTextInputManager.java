package com.tcmanna.tcsaddon.mixin;

import com.mojang.blaze3d.platform.TextInputManager;
import com.tcmanna.tcsaddon.features.impl.skyblock.InputFix;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextInputManager.class)
public class MixinTextInputManager {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    void handleTick(CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null && InputFix.INSTANCE.getEnabled()) ci.cancel();
    }

    @Inject(method = "setIMEInputMode", at = @At("HEAD"), cancellable = true)
    void handleSet(boolean value, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null && InputFix.INSTANCE.getEnabled()) ci.cancel();
    }
}
