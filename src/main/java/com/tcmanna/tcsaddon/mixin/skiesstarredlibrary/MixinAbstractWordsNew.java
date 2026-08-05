package com.tcmanna.tcsaddon.mixin.skiesstarredlibrary;

import com.tcmanna.tcsaddon.features.impl.render.DisablePranks;
import foo.starred.snowbird.handlers.minecraft.AbstractWords;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(AbstractWords.class)
public class MixinAbstractWordsNew {

    @Inject(method = "fn(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void injectFn0(String input, CallbackInfoReturnable<String> cir) {
        if (DisablePranks.INSTANCE.getEnabled() && DisablePranks.INSTANCE.getStarredDonators()) cir.setReturnValue(input);
    }

    @Inject(method = "fn(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void injectFn1(Component input, CallbackInfoReturnable<Component> cir) {
        if (DisablePranks.INSTANCE.getEnabled() && DisablePranks.INSTANCE.getStarredDonators()) cir.setReturnValue(input);
    }

    @Inject(method = "fn(Lnet/minecraft/util/FormattedCharSequence;)Lnet/minecraft/util/FormattedCharSequence;", at = @At("HEAD"), cancellable = true)
    private void injectFn2(FormattedCharSequence input, CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (DisablePranks.INSTANCE.getEnabled() && DisablePranks.INSTANCE.getStarredDonators()) cir.setReturnValue(input);
    }

}
