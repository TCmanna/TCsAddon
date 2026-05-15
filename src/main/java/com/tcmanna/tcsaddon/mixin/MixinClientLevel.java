package com.tcmanna.tcsaddon.mixin;

import com.tcmanna.tcsaddon.events.EntityEnterWorldEvent;
import com.tcmanna.tcsaddon.events.PlaySoundEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        new EntityEnterWorldEvent(entity).postAndCatch();
    }

    @Inject(method = "playSound", at = @At("HEAD"))
    private void onPlayerSound(double d, double e, double f, SoundEvent soundEvent, SoundSource soundSource, float g, float h, boolean bl, long l, CallbackInfo ci) {
        new PlaySoundEvent(new Vec3(d, e, f), soundEvent, soundSource).postAndCatch();
    }
}
