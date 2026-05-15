package com.tcmanna.tcsaddon.mixin;

import com.odtheking.odin.features.impl.render.HidePlayers;
import com.tcmanna.tcsaddon.features.impl.render.HideEntity;
import com.tcmanna.tcsaddon.features.impl.render.NameTag;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void removeNameTag(T entity, S state, float tickDelta, CallbackInfo ci) {
        if (!NameTag.INSTANCE.getEnabled()) return;
        if (entity instanceof Player player) {
            if (NameTag.shouldRender(player)) state.nameTag = null;
        }
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onRender(T entity, Frustum frustum, double d, double e, double f, CallbackInfoReturnable<Boolean> cir) {
        if (HideEntity.INSTANCE.getEnabled()) cir.setReturnValue(false);
    }
}
