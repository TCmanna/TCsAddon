package com.tcmanna.tcsaddon.features.impl.fishing

import com.google.common.collect.Streams
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.tcmanna.tcsaddon.utils.Utils
import com.tcmanna.tcsaddon.events.AutoFishingEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.Endermite
import net.minecraft.world.entity.monster.Silverfish

object KillWorm : Module(
    name = "kill Worm",
    category = Category.custom("Fishing"),
    description = "Auto Kill Worms."
) {
    private val checkRange by NumberSetting("Check Range", 5, 1, 10, 1, "")
    val weaponSlot by NumberSetting("Weapon Slot", 1, 1, 9, 1, "")
    private val killThreshold by NumberSetting("Kill Threshold", 19, 1, 60, 1, "")
    private val strictMode by BooleanSetting("Strict Mode", false, "")
    private val KillDelay by NumberSetting("Kill Delay", 200L, 200L, 1500L, 100L, "")
    val killWithHit by BooleanSetting("Kill With Hit", true, "Request [Auto Fish] State Check enabled.")

    var kill = false

    private var checkDelay = 0

    init {
        on<AutoFishingEvent.Before> {
            if (!LocationUtils.isInSkyblock || mc.player == null || mc.level == null) return@on
            if (kill) {
                return@on
                cancel()
            }
            if (strictMode) return@on

            val wormsList: MutableList<Entity?> = Streams.stream(mc.level!!.entitiesForRendering())
                .filter { entity -> entity is Silverfish || entity is Endermite }
                .filter { entity -> entity.position().distanceTo(mc.player!!.position()) < checkRange }
                .toList()
            if (wormsList.size >= killThreshold) kill = true
        }

        on<AutoFishingEvent.After> {
            if (!LocationUtils.isInSkyblock) return@on


            if (kill) {
                cancel()
                if (!strictMode) {
                    killWorms()
                }

            }
        }

        on<TickEvent.End> {
            if (checkDelay > 0) {
                checkDelay--
                return@on
            }

            if (!LocationUtils.isInSkyblock) return@on
            if (!strictMode) return@on
            if (AutoFish.enabled && mc.player?.fishing != null) {
                val wormsList: MutableList<Entity?> = Streams.stream(mc.level!!.entitiesForRendering())
                    .filter { entity -> entity is Silverfish || entity is Endermite }
                    .filter { entity -> entity.position().distanceTo(mc.player!!.position()) < checkRange }
                    .toList()

                if (wormsList.size >= killThreshold && !kill) {
                    kill = true
                    checkDelay = 200
                    killWorms()
                }
            }
        }

        on<WorldEvent.Load> {
            reset()
            onKeybind()
        }
    }

    override fun onEnable() {
        super.onEnable()
        reset()
    }

    fun killWorms() {
        if (mc.player == null) return
        val weaponSlot = weaponSlot - 1
        val lastSlot = mc.player?.getInventory()?.selectedSlot?: 0

        Thread.startVirtualThread {
            Thread.sleep(KillDelay)
            mc.execute { mc.player?.let { it.getInventory().selectedSlot = weaponSlot } }


            Thread.sleep(500)
            mc.execute { Utils.playerUseHeldItem(mc.player, AutoFish.packetClick) }

            Thread.sleep(500)
            mc.execute { mc.player?.let { it.getInventory().selectedSlot = lastSlot } }

            Thread.sleep(500)
            if (Utils.playerHoldFishRod(mc.player)) {
                mc.execute { Utils.playerUseHeldItem(mc.player, AutoFish.packetClick) }
            }
            kill = false
        }
    }

    private fun reset() {
        kill = false
        checkDelay = 0
    }
}