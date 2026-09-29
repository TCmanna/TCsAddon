package com.tcmanna.tcsaddon.mixin.odin;

import com.odtheking.odin.utils.Color;
import com.odtheking.odin.utils.Colors;
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DungeonClass.class)
public class MixinDungeonClass {

    @Shadow
    @Final
    @Mutable
    private Color color;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void tcsaddon$swapColors(CallbackInfo ci) {

        ((MixinDungeonClass) (Object) DungeonClass.ARCHER).color = Colors.MINECRAFT_DARK_RED;
        ((MixinDungeonClass) (Object) DungeonClass.BERSERK).color = Colors.MINECRAFT_GOLD;
    }
}
