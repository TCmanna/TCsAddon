package com.tcmanna.tcsaddon.mixin.skyhanni;

import at.hannibal2.skyhanni.features.event.carnival.CarnivalFruitDigging;
import at.hannibal2.skyhanni.utils.LorenzVec;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tcmanna.tcsaddon.features.impl.skyblock.AutoCarnivalFruit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CarnivalFruitDigging.class)
public class CarnivalFruitDiggingAccessor {
    @ModifyExpressionValue(
            method = "onRenderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lat/hannibal2/skyhanni/features/event/carnival/CarnivalFruitDigging$GamePos;toLorenzVec()Lat/hannibal2/skyhanni/utils/LorenzVec;",
                    ordinal = 1
            )
    )
    private LorenzVec modifyVec(LorenzVec original) {
        AutoCarnivalFruit.guessPos = original.toBlockPos();
        return original;
    }
}
