package com.tcmanna.tcsaddon.mixin;

import com.tcmanna.tcsaddon.events.MouseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
    @Inject(
            method = "onMove",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onMouseMove(long window, double mx, double my, CallbackInfo ci) {
        if (checkShit(window)) return;
        if (new MouseEvent.Move(mx, my).postAndCatch()) ci.cancel();
    }

    @Unique
    private boolean checkShit(long window) {
        Minecraft mc = Minecraft.getInstance();
        return window != mc.getWindow().handle() || mc.screen != null;
    }
}
