package com.tcmanna.tcsaddon.mixin.odin;

import com.odtheking.odin.utils.ui.rendering.Font;
import com.odtheking.odin.utils.ui.rendering.NVGRenderer;
import com.tcmanna.tcsaddon.TCsAddon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.nanovg.NanoVG.nvgAddFallbackFontId;

@Mixin(value = NVGRenderer.class, remap = false)
public class MixinNVGRenderer {
    @Shadow private static long vg;

    @Inject(method = "text", at = @At(value = "INVOKE", target = "Lorg/lwjgl/nanovg/NanoVG;nvgFontSize(JF)V"))
    private void injectFont(String text, float x, float y, float size, int color, Font font, CallbackInfo ci) {
        nvgAddFallbackFontId(vg, getFontID(font), getFontID(TCsAddon.getSystemFont()));
    }

    @Shadow @Final
    private int getFontID(Font font) {
        return 0;
    }
}
