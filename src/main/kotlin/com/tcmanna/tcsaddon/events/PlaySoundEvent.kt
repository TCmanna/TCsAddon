package com.tcmanna.tcsaddon.events

import com.odtheking.odin.events.core.Event
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3

class PlaySoundEvent(val pos: Vec3, val soundEvent : SoundEvent, val soundSource : SoundSource) : Event