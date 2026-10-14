package com.tcmanna.tcsaddon.mixin.skyhanni;

import at.hannibal2.skyhanni.features.event.diana.GuessEntry;
import at.hannibal2.skyhanni.utils.LocationUtils;
import at.hannibal2.skyhanni.utils.LorenzVec;
import com.odtheking.odin.utils.ItemUtilsKt;
import com.tcmanna.tcsaddon.features.impl.skyblock.FixSHDiana;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(GuessEntry.class)
abstract public class MixinGuessEntry {
    @Shadow
    abstract public LorenzVec getCurrent();

    @Redirect(method = "checkRemove", at = @At(value = "INVOKE", target = "Lat/hannibal2/skyhanni/utils/LocationUtils;distanceSqToPlayer(Lat/hannibal2/skyhanni/utils/LorenzVec;)D"))
    private double redirect(LocationUtils instance, LorenzVec $this$distanceSqToPlayer) {
        if (!FixSHDiana.INSTANCE.getFixRemoveGuess())
            return instance.distanceSqToPlayer($this$distanceSqToPlayer);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 900;
        }

        String itemId = ItemUtilsKt.getItemId(player.getMainHandItem());
        if (instance.distanceSqToPlayer($this$distanceSqToPlayer) < 900 && itemId.endsWith("_SPADE")) {
            return 0;
        }
        return 900;
    }
}
