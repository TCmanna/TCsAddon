package com.tcmanna.tcsaddon.mixin;

import com.tcmanna.tcsaddon.features.impl.render.ChestESP;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public class MixinLevelChunk {

    @Inject(method = "addAndRegisterBlockEntity", at = @At("TAIL"))
    private void onAddBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity instanceof ChestBlockEntity chest) {
            ChestESP.INSTANCE.getChestCache().add(chest);
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void onRemoveBlockEntity(BlockPos pos, CallbackInfo ci) {
        LevelChunk chunk = (LevelChunk)(Object)this;

        BlockEntity be = chunk.getBlockEntity(pos);

        if (be instanceof ChestBlockEntity chest) {
            ChestESP.INSTANCE.getChestCache().remove(chest);
        }
    }
}
