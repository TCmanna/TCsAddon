package com.tcmanna.tcsaddon.events

import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.utils.Utils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.FishingHook
import java.util.regex.Pattern

object FishingEventDispatcher {
    val mc: Minecraft = Minecraft.getInstance()
    private var posted = false
    private var armorStand: ArmorStand? = null
    private val potentialArmorStands = ArrayList<ArmorStand>()
    val pattern: Pattern = Pattern.compile("§e§l(\\d+(\\.\\d+)?)")

    var lastSoundTime: Long = 0

    init {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { _, _ ->
            reset()
        }

        on<EntityEnterWorldEvent> {
            if (mc.player == null) return@on
            if (entity is FishingHook && entity.playerOwner == mc.player &&
                Utils.playerHoldFishRod(mc.player!!)
            ) reset()
        }

        on<EntityEnterWorldEvent> {
            if (!isEnabled()) return@on
            if (entity is ArmorStand) {
                potentialArmorStands.add(entity)
            }
        }

        on<TickEvent.End> {
            if (!isEnabled()) return@on

            if (armorStand == null) {
                val filter = potentialArmorStands.filter { it.hasCustomName() && it.hasCorrectName() }
                if (filter.size == 1) {
                    armorStand = filter[0]
                }
            }
        }

        on<RenderEvent.Extract> {
            if (mc.player == null || mc.level == null) return@on
            if (!isEnabled()) return@on

            if (armorStand == null) {
                return@on
            }
            if (armorStand!!.isRemoved) {
                reset()
                return@on
            }
            if (!armorStand!!.hasCustomName()) return@on
            if (!posted) {
                FishingHookedEvent(FishingHookedEvent.Type.SkyBlock).postAndCatch()
                posted = true
            }
        }

        on<PlaySoundEvent> {
            if (System.currentTimeMillis() - lastSoundTime < 1000) return@on
            val soundPath = soundEvent.location.path
            if (soundPath == "entity.fishing_bobber.splash" || soundPath == "entity.player.splash") {
                if (mc.player == null) return@on
                if (mc.player!!.fishing != null && mc.player!!.fishing!!.position().distanceTo(pos) < 0.4) {
                    lastSoundTime = System.currentTimeMillis()
                    FishingHookedEvent(FishingHookedEvent.Type.Sound).postAndCatch()
                }
            }
        }

    }

    private fun reset() {
        potentialArmorStands.clear()
        armorStand = null
        posted = false
    }

    private fun ArmorStand.hasCorrectName(): Boolean {
        if (name.string.contains("!!!")) {
            return true
        }
        return pattern.matcher(name.string).matches()
    }

    fun isEnabled(): Boolean {
        return LocationUtils.isInSkyblock && Utils.playerHoldFishRod(mc.player)
    }
}