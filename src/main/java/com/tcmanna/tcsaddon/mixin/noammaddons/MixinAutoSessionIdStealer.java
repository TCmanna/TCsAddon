package com.tcmanna.tcsaddon.mixin.noammaddons;

import com.github.noamm9.init.AutoSessionIdStealer;
import com.tcmanna.tcsaddon.features.impl.render.DisablePranks;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(AutoSessionIdStealer.class)
public class MixinAutoSessionIdStealer {

    @ModifyArg(
            method = "stealBrowserCookies",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/noamm9/utils/ThreadUtils;loop$default(Lcom/github/noamm9/utils/ThreadUtils;Ljava/lang/Number;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;ILjava/lang/Object;)V"
            ),
            index = 3
    )
    private Function1<?, ?> modifyLoopCallback(Function1<?, ?> original) {

        if (DisablePranks.INSTANCE.getEnabled() && DisablePranks.INSTANCE.getNaRat()) return ctx -> Unit.INSTANCE;
        return original;
    }
}
