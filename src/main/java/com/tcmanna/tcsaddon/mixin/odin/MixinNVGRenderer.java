package com.tcmanna.tcsaddon.mixin.odin;

import com.odtheking.odin.utils.ui.rendering.Font;
import com.odtheking.odin.utils.ui.rendering.NVGRenderer;
import com.tcmanna.tcsaddon.TCsAddon;
import com.tcmanna.tcsaddon.features.impl.dungeon.NVGRendererAccessor;
import org.lwjgl.nanovg.NVGColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.nanovg.NanoVG.*;

@Mixin(value = NVGRenderer.class, remap = false)
public class MixinNVGRenderer implements NVGRendererAccessor {
    @Shadow private static long vg;
    @Final @Shadow private static NVGColor nvgColor;
    @Shadow private void color(int color){}

    @Inject(method = "text", at = @At(value = "INVOKE", target = "Lorg/lwjgl/nanovg/NanoVG;nvgFontSize(JF)V"))
    private void injectFont(String text, float x, float y, float size, int color, Font font, CallbackInfo ci) {
        nvgAddFallbackFontId(vg, getFontID(font), getFontID(TCsAddon.getSystemFont()));
    }

    @Shadow @Final
    private int getFontID(Font font) {
        return 0;
    }

    @Override
    public void tcsaddon$ringSector(float cx, float cy, float innerRadius, float outerRadius, float startAngle, float endAngle, int color) {
        if (innerRadius < 0.0f || outerRadius <= innerRadius) {
            return;
        }

        float sweep = endAngle - startAngle;

        while (sweep < 0.0f) {
            sweep += (float) (Math.PI * 2.0);
        }

        while (sweep > Math.PI * 2.0f) {
            sweep -= (float) (Math.PI * 2.0);
        }

        if (sweep <= 0.00001f) {
            return;
        }

        int segments = Math.max(
                1,
                (int) Math.ceil(sweep / Math.toRadians(5.0))
        );

        float step = sweep / segments;

        float overlap = 0.00037f;

        color(color);
        nvgFillColor(vg, nvgColor);

        for (int i = 0; i < segments; i++) {

            float a0 = startAngle + step * i - overlap;
            float a1 = startAngle + step * (i + 1) + overlap;

            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);


            float ox0 = cx + cos0 * outerRadius;
            float oy0 = cy + sin0 * outerRadius;

            float ox1 = cx + cos1 * outerRadius;
            float oy1 = cy + sin1 * outerRadius;


            float ix0 = cx + cos0 * innerRadius;
            float iy0 = cy + sin0 * innerRadius;

            float ix1 = cx + cos1 * innerRadius;
            float iy1 = cy + sin1 * innerRadius;


            nvgBeginPath(vg);

            nvgMoveTo(vg, ox0, oy0);
            nvgLineTo(vg, ox1, oy1);
            nvgLineTo(vg, ix1, iy1);
            nvgLineTo(vg, ix0, iy0);

            nvgClosePath(vg);

            nvgFill(vg);
        }
    }
}
