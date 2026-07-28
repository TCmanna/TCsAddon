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
    private void onPlayerSound(double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay, long seed, CallbackInfo ci) {
        new PlaySoundEvent(new Vec3(x, y, z), sound, source).postAndCatch();
    }
}
