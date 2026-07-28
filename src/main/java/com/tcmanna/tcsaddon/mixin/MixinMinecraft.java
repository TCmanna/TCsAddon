package com.tcmanna.tcsaddon.mixin;

import com.tcmanna.tcsaddon.features.impl.boss.LeapMid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Shadow public @Nullable LocalPlayer player;

    @Redirect(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;grabMouse()V"))
    private void redirectGrab(MouseHandler mouseHandler) {
        if (LeapMid.INSTANCE.getLeaped()) {
            return;
        }
        mouseHandler.grabMouse();
    }
}
