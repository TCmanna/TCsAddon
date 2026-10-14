package com.tcmanna.tcsaddon.mixin;

import com.tcmanna.tcsaddon.features.impl.render.ChestESP;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelChunk.class)
public class MixinLevelChunk {

    @Inject(method = "addAndRegisterBlockEntity", at = @At("TAIL"))
    private void onAddBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity instanceof ChestBlockEntity chest) {
            List<BlockPos> chestCache = ChestESP.getChestCache();
            BlockPos blockPos = chest.getBlockPos();

            if (!chestCache.contains(blockPos)) chestCache.add(blockPos);
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void onRemoveBlockEntity(BlockPos pos, CallbackInfo ci) {
        ChestESP.getChestCache().removeIf(pos::equals);
    }
}
